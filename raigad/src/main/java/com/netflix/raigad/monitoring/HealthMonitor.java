/**
 * Copyright 2017 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.raigad.monitoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.raigad.configuration.IConfiguration;
import com.netflix.raigad.identity.InstanceManager;
import com.netflix.raigad.scheduler.SimpleTimer;
import com.netflix.raigad.scheduler.Task;
import com.netflix.raigad.scheduler.TaskTimer;
import com.netflix.raigad.utils.ElasticsearchProcessMonitor;
import com.netflix.raigad.utils.ElasticsearchTransportClient;
import com.netflix.raigad.utils.ElasticsearchUtils;
import com.netflix.raigad.utils.HttpModule;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class HealthMonitor extends Task {
    private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);

    public static final String METRIC_NAME = "Elasticsearch_HealthMonitor";
    private final Elasticsearch_HealthReporter healthReporter;
    private final InstanceManager instanceManager;
    private static TimeValue masterNodeTimeout = TimeValue.timeValueSeconds(60);
    private final DiscoveryClient discoveryClient;
    private final HttpModule httpModule;

    @Inject
    public HealthMonitor(IConfiguration config, InstanceManager instanceManager, HttpModule httpModule) {
        super(config);
        this.instanceManager = instanceManager;
        this.httpModule = httpModule;
        healthReporter = new Elasticsearch_HealthReporter();
        discoveryClient = DiscoveryManager.getInstance().getDiscoveryClient();
        Monitors.registerObject(healthReporter);
    }

    @Override
    public void execute() throws Exception {
        // Only start monitoring if Elasticsearch is started
        if (!ElasticsearchProcessMonitor.isElasticsearchRunning()) {
            String exceptionMsg = "Elasticsearch is not running, check back again later";
            logger.info(exceptionMsg);
            return;
        }

        // In case we configured only the master node to report metrics and this node is not a master - bail out
        if (config.reportMetricsFromMasterOnly() && !ElasticsearchUtils.amIMasterNode(config, httpModule)) {
            return;
        }

        HealthBean healthBean = new HealthBean();
        try {
            Client esTransportClient = ElasticsearchTransportClient.instance(config).getTransportClient();
            ClusterHealthStatus clusterHealthStatus = esTransportClient.admin().cluster().prepareHealth().setTimeout(masterNodeTimeout).execute().get().getStatus();
            ClusterHealthResponse clusterHealthResponse = esTransportClient.admin().cluster().prepareHealth().execute().actionGet(masterNodeTimeout);

            if (clusterHealthStatus == null) {
                logger.info("ClusterHealthStatus is null, hence returning (no health).");
                resetHealthStats(healthBean);
                return;
            }

            //Check if status = GREEN, YELLOW or RED
            if ("GREEN".equalsIgnoreCase(clusterHealthStatus.name())) {
                healthBean.greenorredstatus = 0;
                healthBean.greenoryellowstatus = 0;
            } else if ("YELLOW".equalsIgnoreCase(clusterHealthStatus.name())) {
                healthBean.greenoryellowstatus = 1;
                healthBean.greenorredstatus = 0;
            } else if ("RED".equalsIgnoreCase(clusterHealthStatus.name())) {
                healthBean.greenorredstatus = 1;
                healthBean.greenoryellowstatus = 0;
            }

            if (config.isNodeMismatchWithDiscoveryEnabled()) {
                // Check if there is node mismatch between discovery and ES
                healthBean.nodematch = clusterHealthResponse.getNumberOfNodes() == instanceManager.getAllInstances().size() ? 0 : 1;
            } else {
                healthBean.nodematch = clusterHealthResponse.getNumberOfNodes() == config.getDesiredNumberOfNodesInCluster() ? 0 : 1;
            }

            if (config.isEurekaHealthCheckEnabled()) {
                healthBean.eurekanodematch = clusterHealthResponse.getNumberOfNodes() == discoveryClient.getApplication(config.getAppName()).getInstances().size() ? 0 : 1;
            }
        } catch (Exception e) {
            resetHealthStats(healthBean);
            logger.warn("Failed to load cluster health status", e);
        }

        healthReporter.healthBean.set(healthBean);
    }

    public class Elasticsearch_HealthReporter {
        private final AtomicReference<HealthBean> healthBean;

        public Elasticsearch_HealthReporter() {
            healthBean = new AtomicReference<>(new HealthBean());
        }

        @Monitor(name = "es_healthstatus_greenorred", type = DataSourceType.GAUGE)
        public int getEsHealthstatusGreenorred() {
            return healthBean.get().greenorredstatus;
        }

        @Monitor(name = "es_healthstatus_greenoryellow", type = DataSourceType.GAUGE)
        public int getEsHealthstatusGreenoryellow() {
            return healthBean.get().greenoryellowstatus;
        }

        @Monitor(name = "es_nodematchstatus", type = DataSourceType.GAUGE)
        public int getEsNodematchstatus() {
            return healthBean.get().nodematch;
        }

        @Monitor(name = "es_eurekanodematchstatus", type = DataSourceType.GAUGE)
        public int getEsEurekanodematchstatus() {
            return healthBean.get().eurekanodematch;
        }
    }

    private static class HealthBean {
        private int greenorredstatus = -1;
        private int greenoryellowstatus = -1;
        private int nodematch = -1;
        private int eurekanodematch = -1;
    }

    public static TaskTimer getTimer(String name) {
        return new SimpleTimer(name, 60 * 1000);
    }

    @Override
    public String getName() {
        return METRIC_NAME;
    }

    private void resetHealthStats(HealthBean healthBean) {
        healthBean.greenorredstatus = -1;
        healthBean.greenoryellowstatus = -1;
        healthBean.nodematch = -1;
        healthBean.eurekanodematch = -1;
    }
}
