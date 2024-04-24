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

package com.netflix.raigad.configuration;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.*;
import com.netflix.raigad.aws.ICredential;
import com.netflix.raigad.utils.RetriableCallable;
import com.netflix.raigad.utils.SystemUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class RaigadConfiguration implements IConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RaigadConfiguration.class);

    public static final String MY_WEBAPP_NAME = "Raigad";

    private static final String CONFIG_CLUSTER_NAME = MY_WEBAPP_NAME + ".es.clustername";
    private static final String CONFIG_AVAILABILITY_ZONES = MY_WEBAPP_NAME + ".zones.available";
    private static final String CONFIG_DATA_LOCATION = MY_WEBAPP_NAME + ".es.data.location";
    private static final String CONFIG_LOG_LOCATION = MY_WEBAPP_NAME + ".es.log.location";
    private static final String CONFIG_ES_START_SCRIPT = MY_WEBAPP_NAME + ".es.startscript";
    private static final String CONFIG_ES_STOP_SCRIPT = MY_WEBAPP_NAME + ".es.stopscript";
    private static final String CONFIG_ES_HOME = MY_WEBAPP_NAME + ".es.home";
    private static final String CONFIG_FD_PING_INTERVAL = MY_WEBAPP_NAME + ".es.fd.pinginterval";
    private static final String CONFIG_FD_PING_TIMEOUT = MY_WEBAPP_NAME + ".es.fd.pingtimeout";
    private static final String CONFIG_HTTP_PORT = MY_WEBAPP_NAME + ".es.http.port";
    private static final String CONFIG_TRANSPORT_TCP_PORT = MY_WEBAPP_NAME + ".es.transport.tcp.port";
    private static final String CONFIG_MIN_MASTER_NODES = MY_WEBAPP_NAME + ".es.min.master.nodes";
    private static final String CONFIG_NUM_REPLICAS = MY_WEBAPP_NAME + ".es.num.replicas";
    private static final String CONFIG_NUM_SHARDS = MY_WEBAPP_NAME + ".es.num.shards";
    private static final String CONFIG_PING_TIMEOUT = MY_WEBAPP_NAME + ".es.pingtimeout";
    private static final String CONFIG_INDEX_REFRESH_INTERVAL = MY_WEBAPP_NAME + ".es.index.refresh.interval";
    private static final String CONFIG_IS_MASTER_QUORUM_ENABLED = MY_WEBAPP_NAME + ".es.master.quorum.enabled";
    private static final String CONFIG_IS_PING_MULTICAST_ENABLED = MY_WEBAPP_NAME + ".es.ping.multicast.enabled";
    private static final String CONFIG_ES_DISCOVERY_TYPE = MY_WEBAPP_NAME + ".es.discovery.type";
    private static final String CONFIG_BOOTCLUSTER_NAME = MY_WEBAPP_NAME + ".bootcluster";
    private static final String CONFIG_INSTANCE_DATA_RETRIEVER = MY_WEBAPP_NAME + ".instanceDataRetriever";
    private static final String CONFIG_CREDENTIAL_PROVIDER = MY_WEBAPP_NAME + ".credentialProvider";
    private static final String CONFIG_SECURITY_GROUP_NAME = MY_WEBAPP_NAME + ".security.group.name";
    private static final String CONFIG_IS_MULTI_DC_ENABLED = MY_WEBAPP_NAME + ".es.multi.dc.enabled";
    private static final String CONFIG_IS_ASG_BASED_DEPLOYMENT_ENABLED = MY_WEBAPP_NAME + ".es.asg.based.deployment.enabled";
    private static final String CONFIG_ES_CLUSTER_ROUTING_ATTRIBUTES = MY_WEBAPP_NAME + ".es.cluster.routing.attributes";
    private static final String CONFIG_ES_PROCESS_NAME = MY_WEBAPP_NAME + ".es.processname";
    private static final String CONFIG_ES_SHARD_ALLOCATION_ATTRIBUTE = MY_WEBAPP_NAME + ".es.shard.allocation.attribute";
    private static final String CONFIG_IS_SHARD_ALLOCATION_POLICY_ENABLED = MY_WEBAPP_NAME + ".shard.allocation.policy.enabled";
    private static final String CONFIG_EXTRA_PARAMS = MY_WEBAPP_NAME + ".extra.params";
    private static final String CONFIG_IS_DEBUG_ENABLED = MY_WEBAPP_NAME + ".debug.enabled";
    private static final String CONFIG_IS_SHARDS_PER_NODE_ENABLED = MY_WEBAPP_NAME + ".shards.per.node.enabled";
    private static final String CONFIG_SHARDS_PER_NODE = MY_WEBAPP_NAME + ".shards.per.node";
    private static final String CONFIG_INDEX_METADATA = MY_WEBAPP_NAME + ".index.metadata";
    private static final String CONFIG_IS_INDEX_AUTOCREATION_ENABLED = MY_WEBAPP_NAME + ".index.autocreation.enabled";
    private static final String CONFIG_AUTOCREATE_INDEX_TIMEOUT = MY_WEBAPP_NAME + ".autocreate.index.timeout";
    private static final String CONFIG_AUTOCREATE_INDEX_INITIAL_START_DELAY_SECONDS = MY_WEBAPP_NAME + ".autocreate.index.initial.start.delay.seconds";
    private static final String CONFIG_AUTOCREATE_INDEX_SCHEDULE_MINUTES = MY_WEBAPP_NAME + ".autocreate.index.schedule.minutes";
    private static final String CONFIG_BACKUP_LOCATION = MY_WEBAPP_NAME + ".backup.location";
    private static final String CONFIG_BACKUP_HOUR = MY_WEBAPP_NAME + ".backup.hour";
    private static final String CONFIG_BACKUP_IS_SNAPSHOT_ENABLED = MY_WEBAPP_NAME + ".snapshot.enabled";
    private static final String CONFIG_BACKUP_IS_HOURLY_SNAPSHOT_ENABLED = MY_WEBAPP_NAME + ".hourly.snapshot.enabled";
    private static final String CONFIG_BACKUP_COMMA_SEPARATED_INDICES = MY_WEBAPP_NAME + ".backup.comma.separated.indices";
    private static final String CONFIG_BACKUP_PARTIAL_INDICES = MY_WEBAPP_NAME + ".backup.partial.indices";
    private static final String CONFIG_BACKUP_INCLUDE_GLOBAL_STATE = MY_WEBAPP_NAME + ".backup.include.global.state";
    private static final String CONFIG_BACKUP_WAIT_FOR_COMPLETION = MY_WEBAPP_NAME + ".backup.wait.for.completion";
    private static final String CONFIG_BACKUP_INCLUDE_INDEX_NAME = MY_WEBAPP_NAME + ".backup.include.index.name";
    private static final String CONFIG_BACKUP_CRON_TIMER_SECONDS = MY_WEBAPP_NAME + ".backup.cron.timer.seconds";
    private static final String CONFIG_IS_RESTORE_ENABLED = MY_WEBAPP_NAME + ".restore.enabled";
    private static final String CONFIG_RESTORE_REPOSITORY_NAME = MY_WEBAPP_NAME + ".restore.repository.name";
    private static final String CONFIG_RESTORE_REPOSITORY_TYPE = MY_WEBAPP_NAME + ".restore.repository.type";
    private static final String CONFIG_RESTORE_SNAPSHOT_NAME = MY_WEBAPP_NAME + ".restore.snapshot.name";
    private static final String CONFIG_RESTORE_COMMA_SEPARATED_INDICES = MY_WEBAPP_NAME + ".restore.comma.separated.indices";
    private static final String CONFIG_RESTORE_TASK_INITIAL_START_DELAY_SECONDS = MY_WEBAPP_NAME + ".restore.task.initial.start.delay.seconds";
    private static final String CONFIG_RESTORE_SOURCE_CLUSTER_NAME = MY_WEBAPP_NAME + ".restore.source.cluster.name";
    private static final String CONFIG_RESTORE_SOURCE_REPO_REGION = MY_WEBAPP_NAME + ".restore.source.repo.region";
    private static final String CONFIG_RESTORE_LOCATION = MY_WEBAPP_NAME + ".restore.location";
    private static final String CONFIG_AM_I_TRIBE_NODE = MY_WEBAPP_NAME + ".tribe.node.enabled";
    private static final String CONFIG_AM_I_WRITE_ENABLED_TRIBE_NODE = MY_WEBAPP_NAME + ".tribe.node.write.enabled";
    private static final String CONFIG_AM_I_METADATA_ENABLED_TRIBE_NODE = MY_WEBAPP_NAME + ".tribe.node.metadata.enabled";
    private static final String CONFIG_TRIBE_COMMA_SEPARATED_SOURCE_CLUSTERS = MY_WEBAPP_NAME + ".tribe.comma.separated.source.clusters";
    private static final String CONFIG_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE = MY_WEBAPP_NAME + ".tribe.node.source.cluster.enabled";
    private static final String CONFIG_TRIBE_COMMA_SEPARATED_TRIBE_CLUSTERS = MY_WEBAPP_NAME + ".tribe.comma.separated.tribe.clusters";
    private static final String CONFIG_IS_NODEMISMATCH_WITH_DISCOVERY_ENABLED = MY_WEBAPP_NAME + ".nodemismatch.health.metrics.enabled";
    private static final String CONFIG_DESIRED_NUM_NODES_IN_CLUSTER = MY_WEBAPP_NAME + ".desired.num.nodes.in.cluster";
    private static final String CONFIG_IS_EUREKA_HEALTH_CHECK_ENABLED = MY_WEBAPP_NAME + ".eureka.health.check.enabled";
    private static final String CONFIG_IS_LOCAL_MODE_ENABLED = MY_WEBAPP_NAME + ".local.mode.enabled";
    private static final String CONFIG_CASSANDRA_KEYSPACE_NAME = MY_WEBAPP_NAME + ".cassandra.keyspace.name";
    private static final String CONFIG_CASSANDRA_THRIFT_PORT = MY_WEBAPP_NAME + ".cassandra.thrift.port";
    private static final String CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED = MY_WEBAPP_NAME + ".eureka.host.supplier.enabled";
    private static final String CONFIG_COMMA_SEPARATED_CASSANDRA_HOSTNAMES = MY_WEBAPP_NAME + ".comma.separated.cassandra.hostnames";
    private static final String CONFIG_IS_SECURITY_GROUP_IN_MULTI_DC = MY_WEBAPP_NAME + ".security.group.in.multi.dc.enabled";
    private static final String CONFIG_IS_KIBANA_SETUP_REQUIRED = MY_WEBAPP_NAME + ".kibana.setup.required";
    private static final String CONFIG_KIBANA_PORT = MY_WEBAPP_NAME + ".kibana.port";
    private static final String CONFIG_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE_IN_MULTI_DC = MY_WEBAPP_NAME + ".tribe.node.source.cluster.enabled.in.multi.dc";
    private static final String CONFIG_REPORT_METRICS_FROM_MASTER_ONLY = MY_WEBAPP_NAME + ".report.metrics.from.master.only";
    private static final String CONFIG_TRIBE_PREFERRED_CLUSTER_ID_ON_CONFLICT = MY_WEBAPP_NAME + ".tribe.preferred.cluster.id.on.conflict";

    // Amazon specific
    private static final String CONFIG_ASG_NAME = MY_WEBAPP_NAME + ".az.asgname";
    private static final String CONFIG_STACK_NAME = MY_WEBAPP_NAME + ".az.stack";
    private static final String CONFIG_REGION_NAME = MY_WEBAPP_NAME + ".az.region";
    private static final String CONFIG_ACL_GROUP_NAME = MY_WEBAPP_NAME + ".acl.groupname";
    private static final String CONFIG_ACL_GROUP_NAME_FOR_VPC = MY_WEBAPP_NAME + ".acl.groupname.vpc";

    private static Boolean isDeployedInVpc = false;
    private static Boolean isVpcExternal = false;
    private static final String MAC_ID = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/mac");
    private static String vpcId = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/network/interfaces/macs/" + MAC_ID + "/vpc-id").trim();

    private static String publicHostname;
    private static String publicIp;
    private static String aclGroupIdForVpc;

    {
        if (StringUtils.equals(vpcId, SystemUtils.NOT_FOUND_STR)) {
            publicHostname = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-hostname").trim();
            publicIp = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-ipv4").trim();
        } else {
            isDeployedInVpc = true;
            isVpcExternal = true;

            publicHostname = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-hostname").trim();
            if (StringUtils.equals(publicHostname, SystemUtils.NOT_FOUND_STR)) {
                // Looks like this is VPC internal, trying local hostname
                publicHostname = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/local-hostname").trim();
                isVpcExternal = false;
            }
            logger.info("Node host name initialized with {}", publicHostname);

            publicIp = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-ipv4").trim();
            if (StringUtils.equals(publicIp, SystemUtils.NOT_FOUND_STR)) {
                // Looks like this is VPC internal, trying local IP
                publicIp = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/local-ipv4").trim();
                isVpcExternal = false;
            }
            logger.info("Node IP initialized with {}", publicIp);
        }
    }

    private static final String RAC = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/placement/availability-zone");
    private static final String LOCAL_HOSTNAME = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/local-hostname").trim();
    private static final String LOCAL_IP = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/local-ipv4").trim();
    private static final String INSTANCE_ID = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/instance-id").trim();
    private static final String INSTANCE_TYPE = SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/instance-type").trim();
    private static final String ES_NODE_NAME = RAC + "." + INSTANCE_ID;

    private static String asgName = System.getenv("ASG_NAME");
    private static String stackName = System.getenv("STACK_NAME");
    private static String REGION = System.getenv("EC2_REGION");

    // Defaults
    private static final String DEFAULT_CLUSTER_NAME = "es_samplecluster";
    private List<String> defaultAvailabilityZones = ImmutableList.of();


    private static final String DEFAULT_DATA_LOCATION = "/mnt/data/es";
    private static final String DEFAULT_LOG_LOCATION = "/logs/es";
    private static final String DEFAULT_YAML_LOCATION = "/apps/elasticsearch/config/elasticsearch.yml";
    private static final String DEFAULT_ES_START_SCRIPT = "/etc/init.d/elasticsearch start";
    private static final String DEFAULT_ES_STOP_SCRIPT = "/etc/init.d/elasticsearch stop";

    private static final String DEFAULT_ES_HOME = "/apps/elasticsearch";
    private static final String DEFAULT_FD_PING_INTERVAL = "30s";
    private static final String DEFAULT_FD_PING_TIMEOUT = "30s";
    private static final int DEFAULT_HTTP_PORT = 7104;
    private static final int DEFAULT_TRANSPORT_TCP_PORT = 7102;
    private static final int DEFAULT_MIN_MASTER_NODES = 1;
    private static final int DEFAULT_NUM_REPLICAS = 2;
    private static final int DEFAULT_NUM_SHARDS = 5;
    private static final String DEFAULT_PING_TIMEOUT = "60s";
    private static final String DEFAULT_INDEX_REFRESH_INTERVAL = "1m";
    private static final boolean DEFAULT_IS_MASTER_QUORUM_ENABLED = true;
    private static final boolean DEFAULT_IS_PING_MULTICAST_ENABLED = false;
    private static final String DEFAULT_CONFIG_BOOTCLUSTER_NAME = "cass_metadata";
    private static final String DEFAULT_CREDENTIAL_PROVIDER = "com.netflix.raigad.aws.IAMCredential";
    private static final String DEFAULT_ES_DISCOVERY_TYPE = "raigad";
    private static final boolean DEFAULT_IS_MULTI_DC_ENABLED = false;
    private static final boolean DEFAULT_IS_ASG_BASED_DEPLOYMENT_ENABLED = false;
    private static final String DEFAULT_ES_CLUSTER_ROUTING_ATTRIBUTES = "rack_id";
    private static final String DEFAULT_ES_PROCESS_NAME = "org.elasticsearch.bootstrap.Elasticsearch";
    private static final boolean DEFAULT_IS_SHARD_ALLOCATION_POLICY_ENABLED = false;
    private static final String DEFAULT_ES_SHARD_ALLOCATION_ATTRIBUTE = "all";
    private static final String DEFAULT_CONFIG_EXTRA_PARAMS = null;
    private static final boolean DEFAULT_IS_DEBUG_ENABLED = false;
    private static final boolean DEFAULT_IS_SHARDS_PER_NODE_ENABLED = false;
    private static final int DEFAULT_SHARDS_PER_NODE = 5;
    private static final boolean DEFAULT_IS_INDEX_AUTOCREATION_ENABLED = false;
    private static final int DEFAULT_AUTOCREATE_INDEX_TIMEOUT = 300000;
    private static final int DEFAULT_AUTOCREATE_INDEX_INITIAL_START_DELAY_SECONDS = 300;
    private static final int DEFAULT_AUTOCREATE_INDEX_SCHEDULE_MINUTES = 10;
    private static final String DEFAULT_INDEX_METADATA = null;
    private static final String DEFAULT_BACKUP_LOCATION = "elasticsearch-us-east-1-backup";
    private static final int DEFAULT_BACKUP_HOUR = 1;
    private static final String DEFAULT_BACKUP_COMMA_SEPARATED_INDICES = "_all";
    private static final boolean DEFAULT_BACKUP_PARTIAL_INDICES = false;
    private static final boolean DEFAULT_BACKUP_INCLUDE_GLOBAL_STATE = false;
    private static final boolean DEFAULT_BACKUP_WAIT_FOR_COMPLETION = true;
    private static final boolean DEFAULT_BACKUP_INCLUDE_INDEX_NAME = false;
    private static final boolean DEFAULT_IS_RESTORE_ENABLED = false;
    private static final String DEFAULT_RESTORE_REPOSITORY_NAME = "testrepo";
    private static final String DEFAULT_RESTORE_REPOSITORY_TYPE = "s3";
    private static final String DEFAULT_RESTORE_SNAPSHOT_NAME = "";
    private static final String DEFAULT_RESTORE_COMMA_SEPARATED_INDICES = "_all";
    private static final int DEFAULT_RESTORE_TASK_INITIAL_START_DELAY_SECONDS = 600;
    private static final String DEFAULT_RESTORE_SOURCE_CLUSTER_NAME = "";
    private static final String DEFAULT_RESTORE_SOURCE_REPO_REGION = "us-east-1";
    private static final String DEFAULT_RESTORE_LOCATION = "elasticsearch-us-east-1-backup";
    private static final boolean DEFAULT_BACKUP_IS_SNAPSHOT_ENABLED = false;
    private static final boolean DEFAULT_BACKUP_IS_HOURLY_SNAPSHOT_ENABLED = false;
    private static final long DEFAULT_BACKUP_CRON_TIMER_SECONDS = 3600;
    private static final boolean DEFAULT_AM_I_TRIBE_NODE = false;
    private static final boolean DEFAULT_AM_I_WRITE_ENABLED_TRIBE_NODE = false;
    private static final boolean DEFAULT_AM_I_METADATA_ENABLED_TRIBE_NODE = false;
    private static final String DEFAULT_TRIBE_COMMA_SEPARATED_SOURCE_CLUSTERS = "";
    private static final boolean DEFAULT_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE = false;
    private static final String DEFAULT_TRIBE_COMMA_SEPARATED_TRIBE_CLUSTERS = "";
    private static final boolean DEFAULT_IS_NODEMISMATCH_WITH_DISCOVERY_ENABLED = false;
    private static final int DEFAULT_DESIRED_NUM_NODES_IN_CLUSTER = 6;
    private static final boolean DEFAULT_IS_EUREKA_HEALTH_CHECK_ENABLED = true;
    private static final boolean DEFAULT_IS_LOCAL_MODE_ENABLED = false;
    private static final String DEFAULT_CASSANDRA_KEYSPACE_NAME = "escarbootstrap";
    private static final int DEFAULT_CASSANDRA_THRIFT_PORT = 7102;
    private static final boolean DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED = true;
    private static final String DEFAULT_COMMA_SEPARATED_CASSANDRA_HOSTNAMES = "";
    private static final boolean DEFAULT_IS_SECURITY_GROUP_IN_MULTI_DC = false;
    private static final boolean DEFAULT_IS_KIBANA_SETUP_REQUIRED = false;
    private static final int DEFAULT_KIBANA_PORT = 8001;
    private static final boolean DEFAULT_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE_IN_MULTI_DC = false;
    private static final boolean DEFAULT_REPORT_METRICS_FROM_MASTER_ONLY = false;
    private static final String DEFAULT_TRIBE_PREFERRED_CLUSTER_ID_ON_CONFLICT = "t0";
    private static final String DEFAULT_ACL_GROUP_NAME_FOR_VPC = "es_samplecluster";

    private final IConfigSource config;
    private final ICredential provider;

    private final DynamicStringProperty credentialProvider = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_CREDENTIAL_PROVIDER, DEFAULT_CREDENTIAL_PROVIDER);
    private final DynamicStringProperty esStartupScriptLocation = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_START_SCRIPT, DEFAULT_ES_START_SCRIPT);
    private final DynamicStringProperty esStopScriptLocation = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_STOP_SCRIPT, DEFAULT_ES_STOP_SCRIPT);
    private final DynamicStringProperty dataLocation = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_DATA_LOCATION, DEFAULT_DATA_LOCATION);
    private final DynamicStringProperty logLocation = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_LOG_LOCATION, DEFAULT_LOG_LOCATION);
    private final DynamicStringProperty esHome = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_HOME, DEFAULT_ES_HOME);
    private final DynamicStringProperty fdPingInterval = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_FD_PING_INTERVAL, DEFAULT_FD_PING_INTERVAL);
    private final DynamicStringProperty fdPingTimeout = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_FD_PING_TIMEOUT, DEFAULT_FD_PING_TIMEOUT);
    private final DynamicIntProperty esHttpPort = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_HTTP_PORT, DEFAULT_HTTP_PORT);
    private final DynamicIntProperty esTransportTcpPort = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_TRANSPORT_TCP_PORT, DEFAULT_TRANSPORT_TCP_PORT);
    private final DynamicIntProperty minimumMasterNodes = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_MIN_MASTER_NODES, DEFAULT_MIN_MASTER_NODES);
    private final DynamicIntProperty numReplicas = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_NUM_REPLICAS, DEFAULT_NUM_REPLICAS);
    private final DynamicIntProperty numShards = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_NUM_SHARDS, DEFAULT_NUM_SHARDS);
    private final DynamicStringProperty pingTimeout = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_PING_TIMEOUT, DEFAULT_PING_TIMEOUT);
    private final DynamicStringProperty indexRefreshInterval = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_INDEX_REFRESH_INTERVAL, DEFAULT_INDEX_REFRESH_INTERVAL);
    private final DynamicBooleanProperty isMasterQuorumEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_MASTER_QUORUM_ENABLED, DEFAULT_IS_MASTER_QUORUM_ENABLED);
    private final DynamicBooleanProperty isPingMulticastEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_PING_MULTICAST_ENABLED, DEFAULT_IS_PING_MULTICAST_ENABLED);
    private final DynamicStringProperty bootclusterName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_BOOTCLUSTER_NAME, DEFAULT_CONFIG_BOOTCLUSTER_NAME);
    private final DynamicStringProperty esDiscoveryType = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_DISCOVERY_TYPE, DEFAULT_ES_DISCOVERY_TYPE);
    private final DynamicStringProperty securityGroupName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_SECURITY_GROUP_NAME, DEFAULT_CLUSTER_NAME);
    private final DynamicBooleanProperty isMultiDcEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_MULTI_DC_ENABLED, DEFAULT_IS_MULTI_DC_ENABLED);
    private final DynamicBooleanProperty isAsgBasedDeploymentEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_ASG_BASED_DEPLOYMENT_ENABLED, DEFAULT_IS_ASG_BASED_DEPLOYMENT_ENABLED);
    private final DynamicStringProperty esClusterRoutingAttributes = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_CLUSTER_ROUTING_ATTRIBUTES, DEFAULT_ES_CLUSTER_ROUTING_ATTRIBUTES);
    private final DynamicBooleanProperty isShardAllocationPolicyEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_SHARD_ALLOCATION_POLICY_ENABLED, DEFAULT_IS_SHARD_ALLOCATION_POLICY_ENABLED);
    private final DynamicStringProperty esShardAllocationAttribute = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_SHARD_ALLOCATION_ATTRIBUTE, DEFAULT_ES_SHARD_ALLOCATION_ATTRIBUTE);
    private final DynamicStringProperty extraParams = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_EXTRA_PARAMS, DEFAULT_CONFIG_EXTRA_PARAMS);
    private final DynamicBooleanProperty isDebugEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_DEBUG_ENABLED, DEFAULT_IS_DEBUG_ENABLED);
    private final DynamicBooleanProperty isShardsPerNodeEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_SHARDS_PER_NODE_ENABLED, DEFAULT_IS_SHARDS_PER_NODE_ENABLED);
    private final DynamicIntProperty totalShardsPerNodes = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_SHARDS_PER_NODE, DEFAULT_SHARDS_PER_NODE);
    private final DynamicStringProperty indexMetadata = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_INDEX_METADATA, DEFAULT_INDEX_METADATA);
    private final DynamicBooleanProperty isIndexAutocreationEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_INDEX_AUTOCREATION_ENABLED, DEFAULT_IS_INDEX_AUTOCREATION_ENABLED);
    private final DynamicIntProperty autocreateIndexTimeout = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_AUTOCREATE_INDEX_TIMEOUT, DEFAULT_AUTOCREATE_INDEX_TIMEOUT);
    private final DynamicIntProperty autocreateIndexInitialStartDelaySeconds = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_AUTOCREATE_INDEX_INITIAL_START_DELAY_SECONDS, DEFAULT_AUTOCREATE_INDEX_INITIAL_START_DELAY_SECONDS);
    private final DynamicIntProperty autocreateIndexScheduleMinutes = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_AUTOCREATE_INDEX_SCHEDULE_MINUTES, DEFAULT_AUTOCREATE_INDEX_SCHEDULE_MINUTES);
    private final DynamicStringProperty esProcessName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ES_PROCESS_NAME, DEFAULT_ES_PROCESS_NAME);
    private final DynamicStringProperty bucketName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_BACKUP_LOCATION, DEFAULT_BACKUP_LOCATION);
    private final DynamicIntProperty backupHour = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_BACKUP_HOUR, DEFAULT_BACKUP_HOUR);
    private final DynamicStringProperty commaSeparatedIndicesToBackup = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_BACKUP_COMMA_SEPARATED_INDICES, DEFAULT_BACKUP_COMMA_SEPARATED_INDICES);
    private final DynamicBooleanProperty partiallyBackupIndices = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_PARTIAL_INDICES, DEFAULT_BACKUP_PARTIAL_INDICES);
    private final DynamicBooleanProperty includeGlobalStateDuringBackup = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_INCLUDE_GLOBAL_STATE, DEFAULT_BACKUP_INCLUDE_GLOBAL_STATE);
    private final DynamicBooleanProperty waitForCompletionOfBackup = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_WAIT_FOR_COMPLETION, DEFAULT_BACKUP_WAIT_FOR_COMPLETION);
    private final DynamicBooleanProperty includeIndexNameInSnapshotBackup = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_INCLUDE_INDEX_NAME, DEFAULT_BACKUP_INCLUDE_INDEX_NAME);
    private final DynamicBooleanProperty isRestoreEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_RESTORE_ENABLED, DEFAULT_IS_RESTORE_ENABLED);
    private final DynamicStringProperty restoreRepositoryName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_REPOSITORY_NAME, DEFAULT_RESTORE_REPOSITORY_NAME);
    private final DynamicStringProperty restoreRepositoryType = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_REPOSITORY_TYPE, DEFAULT_RESTORE_REPOSITORY_TYPE);
    private final DynamicStringProperty restoreSnapshotName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_SNAPSHOT_NAME, DEFAULT_RESTORE_SNAPSHOT_NAME);
    private final DynamicStringProperty commaSeparatedIndicesToRestore = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_COMMA_SEPARATED_INDICES, DEFAULT_RESTORE_COMMA_SEPARATED_INDICES);
    private final DynamicIntProperty restoreTaskInitialStartDelaySeconds = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_RESTORE_TASK_INITIAL_START_DELAY_SECONDS, DEFAULT_RESTORE_TASK_INITIAL_START_DELAY_SECONDS);
    private final DynamicStringProperty restoreSourceClusterName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_SOURCE_CLUSTER_NAME, DEFAULT_RESTORE_SOURCE_CLUSTER_NAME);
    private final DynamicStringProperty restoreSourceRepoRegion = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_SOURCE_REPO_REGION, DEFAULT_RESTORE_SOURCE_REPO_REGION);
    private final DynamicStringProperty restoreLocation = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_RESTORE_LOCATION, DEFAULT_RESTORE_LOCATION);
    private final DynamicBooleanProperty isSnapshotBackupEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_IS_SNAPSHOT_ENABLED, DEFAULT_BACKUP_IS_SNAPSHOT_ENABLED);
    private final DynamicBooleanProperty isHourlySnapshotBackupEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_BACKUP_IS_HOURLY_SNAPSHOT_ENABLED, DEFAULT_BACKUP_IS_HOURLY_SNAPSHOT_ENABLED);
    private final DynamicLongProperty backupCronTimerSeconds = DynamicPropertyFactory.getInstance().getLongProperty(CONFIG_BACKUP_CRON_TIMER_SECONDS, DEFAULT_BACKUP_CRON_TIMER_SECONDS);
    private final DynamicBooleanProperty amITribeNode = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_AM_I_TRIBE_NODE, DEFAULT_AM_I_TRIBE_NODE);
    private final DynamicBooleanProperty amIWriteEnabledTribeNode = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_AM_I_WRITE_ENABLED_TRIBE_NODE, DEFAULT_AM_I_WRITE_ENABLED_TRIBE_NODE);
    private final DynamicBooleanProperty amIMetadataEnabledTribeNode = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_AM_I_METADATA_ENABLED_TRIBE_NODE, DEFAULT_AM_I_METADATA_ENABLED_TRIBE_NODE);
    private final DynamicStringProperty commaSeparatedSourceClustersInTribe = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_TRIBE_COMMA_SEPARATED_SOURCE_CLUSTERS, DEFAULT_TRIBE_COMMA_SEPARATED_SOURCE_CLUSTERS);
    private final DynamicBooleanProperty amISourceClusterForTribeNode = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE, DEFAULT_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE);
    private final DynamicStringProperty commaSeparatedTribeClusters = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_TRIBE_COMMA_SEPARATED_TRIBE_CLUSTERS, DEFAULT_TRIBE_COMMA_SEPARATED_TRIBE_CLUSTERS);
    private final DynamicBooleanProperty isNodeMismatchWithDiscoveryEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_NODEMISMATCH_WITH_DISCOVERY_ENABLED, DEFAULT_IS_NODEMISMATCH_WITH_DISCOVERY_ENABLED);
    private final DynamicIntProperty desiredNumNodesInCluster = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_DESIRED_NUM_NODES_IN_CLUSTER, DEFAULT_DESIRED_NUM_NODES_IN_CLUSTER);
    private final DynamicBooleanProperty isEurekaHealthCheckEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_EUREKA_HEALTH_CHECK_ENABLED, DEFAULT_IS_EUREKA_HEALTH_CHECK_ENABLED);
    private final DynamicBooleanProperty isLocalModeEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_LOCAL_MODE_ENABLED, DEFAULT_IS_LOCAL_MODE_ENABLED);
    private final DynamicStringProperty cassandraKeyspaceName = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_CASSANDRA_KEYSPACE_NAME, DEFAULT_CASSANDRA_KEYSPACE_NAME);
    private final DynamicIntProperty cassandraThriftPort = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_CASSANDRA_THRIFT_PORT, DEFAULT_CASSANDRA_THRIFT_PORT);
    private final DynamicBooleanProperty isEurekaHostSupplierEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED, DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED);
    private final DynamicStringProperty commaSeparatedCassandraHostnames = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_COMMA_SEPARATED_CASSANDRA_HOSTNAMES, DEFAULT_COMMA_SEPARATED_CASSANDRA_HOSTNAMES);
    private final DynamicBooleanProperty isSecurityGroupInMultiDc = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_SECURITY_GROUP_IN_MULTI_DC, DEFAULT_IS_SECURITY_GROUP_IN_MULTI_DC);
    private final DynamicBooleanProperty isKibanaSetupRequired = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_IS_KIBANA_SETUP_REQUIRED, DEFAULT_IS_KIBANA_SETUP_REQUIRED);
    private final DynamicIntProperty kibanaPort = DynamicPropertyFactory.getInstance().getIntProperty(CONFIG_KIBANA_PORT, DEFAULT_KIBANA_PORT);
    private final DynamicBooleanProperty amISourceClusterForTribeNodeInMultiDc = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE_IN_MULTI_DC, DEFAULT_AM_I_SOURCE_CLUSTER_FOR_TRIBE_NODE_IN_MULTI_DC);
    private final DynamicBooleanProperty reportMetricsFromMasterOnly = DynamicPropertyFactory.getInstance().getBooleanProperty(CONFIG_REPORT_METRICS_FROM_MASTER_ONLY, DEFAULT_REPORT_METRICS_FROM_MASTER_ONLY);
    private final DynamicStringProperty tribePreferredClusterIdOnConflict = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_TRIBE_PREFERRED_CLUSTER_ID_ON_CONFLICT, DEFAULT_TRIBE_PREFERRED_CLUSTER_ID_ON_CONFLICT);
    private final DynamicStringProperty aclGroupNameForVpc = DynamicPropertyFactory.getInstance().getStringProperty(CONFIG_ACL_GROUP_NAME_FOR_VPC, DEFAULT_ACL_GROUP_NAME_FOR_VPC);

    @Inject
    public RaigadConfiguration(ICredential provider, IConfigSource config) {
        this.provider = provider;
        this.config = config;
    }

    @Override
    public void initialize() {
        setupEnvVars();
        this.config.initialize(asgName, REGION);
        setDefaultRACList(REGION);
        populateProps();
        SystemUtils.createDirs(getDataFileLocation());
    }

    private void setupEnvVars() {
        REGION = StringUtils.isBlank(REGION) ? System.getProperty("EC2_REGION") : REGION;

        if (StringUtils.isBlank(REGION)) {
            REGION = RAC.substring(0, RAC.length() - 1);
        }

        asgName = StringUtils.isBlank(asgName) ? System.getProperty("ASG_NAME") : asgName;

        if (StringUtils.isBlank(asgName)) {
            asgName = populateASGName(REGION, INSTANCE_ID);
        }

        stackName = StringUtils.isBlank(stackName) ? System.getProperty("STACK_NAME") : stackName;

        logger.info(String.format("REGION set to [%s], ASG Name set to [%s]", REGION, asgName));
    }

    /**
     * Query amazon to get ASG name. Currently not available as part of instance
     * info api.
     */
    private String populateASGName(String region, String instanceId) {
        GetASGName getASGName = new GetASGName(region, instanceId);

        try {
            return getASGName.call();
        } catch (Exception e) {
            logger.error("Failed to determine ASG name", e);
            return null;
        }
    }

    private class GetASGName extends RetriableCallable<String> {
        private static final int NUMBER_OF_RETRIES = 15;
        private static final long WAIT_TIME = 30000;
        private final String region;
        private final String instanceId;
        private final AmazonEC2 client;

        public GetASGName(String region, String instanceId) {
            super(NUMBER_OF_RETRIES, WAIT_TIME);
            this.region = region;
            this.instanceId = instanceId;
            client = new AmazonEC2Client(provider.getAwsCredentialProvider());
            client.setEndpoint("ec2." + region + ".amazonaws.com");
        }

        @Override
        public String retriableCall() throws IllegalStateException {
            DescribeInstancesRequest desc = new DescribeInstancesRequest().withInstanceIds(instanceId);
            DescribeInstancesResult res = client.describeInstances(desc);

            for (Reservation resr : res.getReservations()) {
                for (Instance ins : resr.getInstances()) {
                    for (com.amazonaws.services.ec2.model.Tag tag : ins.getTags()) {
                        if ("aws:autoscaling:groupName".equals(tag.getKey())) {
                            return tag.getValue();
                        }
                    }
                }
            }

            logger.warn("Couldn't determine ASG name");
            throw new IllegalStateException("Couldn't determine ASG name");
        }
    }

    /**
     * Get the fist 3 available zones in the region
     */
    public void setDefaultRACList(String region) {
        AmazonEC2 client = new AmazonEC2Client(provider.getAwsCredentialProvider());
        client.setEndpoint("ec2." + region + ".amazonaws.com");
        DescribeAvailabilityZonesResult res = client.describeAvailabilityZones();
        List<String> zone = Lists.newArrayList();

        for (AvailabilityZone reg : res.getAvailabilityZones()) {
            if ("available".equals(reg.getState())) {
                zone.add(reg.getZoneName());
            }
            if (zone.size() == 3) {
                break;
            }
        }
        defaultAvailabilityZones = ImmutableList.copyOf(zone);
    }

    private void populateProps() {
        config.set(CONFIG_ASG_NAME, asgName);
        config.set(CONFIG_REGION_NAME, REGION);
    }

    @Override
    public List<String> getRacs() {
        return config.getList(CONFIG_AVAILABILITY_ZONES, defaultAvailabilityZones);
    }

    @Override
    public String getDC() {
        return config.get(CONFIG_REGION_NAME, "");
    }

    @Override
    public void setDC(String region) {
        config.set(CONFIG_REGION_NAME, region);
    }

    @Override
    public String getASGName() {
        return config.get(CONFIG_ASG_NAME, asgName);
    }

    @Override
    public String getStackName() {
        return config.get(CONFIG_STACK_NAME, stackName);
    }

    @Override
    public String getACLGroupName() {
        return config.get(CONFIG_ACL_GROUP_NAME, this.getAppName());
    }

    @Override
    public String getDataFileLocation() {
        return dataLocation.get();
    }

    @Override
    public String getLogFileLocation() {
        return logLocation.get();
    }

    @Override
    public String getElasticsearchStartupScript() {
        return esStartupScriptLocation.get();
    }

    @Override
    public String getYamlLocation() {
        return DEFAULT_YAML_LOCATION;
    }

    @Override
    public String getBackupLocation() {
        return bucketName.get();
    }

    @Override
    public String getElasticsearchHome() {
        return esHome.get();
    }

    @Override
    public String getElasticsearchStopScript() {
        return esStopScriptLocation.get();
    }

    @Override
    public String getFdPingInterval() {
        return fdPingInterval.get();
    }

    @Override
    public String getFdPingTimeout() {
        return fdPingTimeout.get();
    }

    @Override
    public int getHttpPort() {
        return esHttpPort.get();
    }

    @Override
    public int getTransportTcpPort() {
        return esTransportTcpPort.get();
    }

    @Override
    public int getMinimumMasterNodes() {
        return minimumMasterNodes.get();
    }

    @Override
    public int getNumOfReplicas() {
        return numReplicas.get();
    }

    @Override
    public int getTotalShardsPerNode() {
        return totalShardsPerNodes.get();
    }

    @Override
    public int getNumOfShards() {
        return numShards.get();
    }

    @Override
    public String getPingTimeout() {
        return pingTimeout.get();
    }

    @Override
    public String getRefreshInterval() {
        return indexRefreshInterval.get();
    }

    @Override
    public boolean isMasterQuorumEnabled() {
        return isMasterQuorumEnabled.get();
    }

    @Override
    public boolean isPingMulticastEnabled() {
        return isPingMulticastEnabled.get();
    }

    @Override
    public String getHostIP() {
        return publicIp;
    }

    @Override
    public String getHostname() {
        return publicHostname;
    }

    @Override
    public String getInstanceName() {
        return INSTANCE_ID;
    }

    @Override
    public String getInstanceId() {
        return INSTANCE_ID;
    }

    @Override
    public String getHostLocalIP() {
        return LOCAL_IP;
    }

    @Override
    public String getRac() {
        return RAC;
    }

    @Override
    public String getAppName() {
        return config.get(CONFIG_CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
    }

    @Override
    public String getBootClusterName() {
        return bootclusterName.get();
    }

    @Override
    public String getElasticsearchDiscoveryType() {
        return esDiscoveryType.get();
    }

    @Override
    public boolean isMultiDC() {
        return isMultiDcEnabled.get();
    }

    @Override
    public String getClusterRoutingAttributes() {
        return esClusterRoutingAttributes.get();
    }

    @Override
    public boolean isAsgBasedDedicatedDeployment() {
        return isAsgBasedDeploymentEnabled.get();
    }

    @Override
    public String getElasticsearchProcessName() {
        return esProcessName.get();
    }

    /**
     * @return Elasticsearch Index Refresh Interval
     */
    public String getIndexRefreshInterval() {
        return indexRefreshInterval.get();
    }


    @Override
    public boolean doesElasticsearchStartManually() {
        return false;
    }

    @Override
    public String getClusterShardAllocationAttribute() {
        return esShardAllocationAttribute.get();
    }

    @Override
    public boolean isCustomShardAllocationPolicyEnabled() {
        return isShardAllocationPolicyEnabled.get();
    }

    @Override
    public String getEsKeyName(String escarKey) {
        return config.get(escarKey);
    }

    @Override
    public boolean isDebugEnabled() {
        return isDebugEnabled.get();
    }

    @Override
    public boolean isShardPerNodeEnabled() {
        return isShardsPerNodeEnabled.get();
    }

    @Override
    public boolean isIndexAutoCreationEnabled() {
        return isIndexAutocreationEnabled.get();
    }

    @Override
    public String getIndexMetadata() {
        return indexMetadata.get();
    }

    @Override
    public int getAutoCreateIndexTimeout() {
        return autocreateIndexTimeout.get();
    }

    @Override
    public int getAutoCreateIndexInitialStartDelaySeconds() {
        return autocreateIndexInitialStartDelaySeconds.get();
    }

    @Override
    public int getAutoCreateIndexScheduleMinutes() {
        return autocreateIndexScheduleMinutes.get();
    }

    @Override
    public String getExtraConfigParams() {
        return extraParams.get();
    }

    @Override
    public int getBackupHour() {
        return backupHour.get();
    }

    public boolean isSnapshotBackupEnabled() {
        return isSnapshotBackupEnabled.get();
    }

    @Override
    public String getCommaSeparatedIndicesToBackup() {
        return commaSeparatedIndicesToBackup.get();
    }

    @Override
    public boolean partiallyBackupIndices() {
        return partiallyBackupIndices.get();
    }

    @Override
    public boolean includeGlobalStateDuringBackup() {
        return includeGlobalStateDuringBackup.get();
    }

    @Override
    public boolean waitForCompletionOfBackup() {
        return waitForCompletionOfBackup.get();
    }

    @Override
    public boolean includeIndexNameInSnapshot() {
        return includeIndexNameInSnapshotBackup.get();
    }

    @Override
    public boolean isHourlySnapshotEnabled() {
        return isHourlySnapshotBackupEnabled.get();
    }

    @Override
    public long getBackupCronTimerInSeconds() {
        return backupCronTimerSeconds.get();
    }

    @Override
    public boolean isRestoreEnabled() {
        return isRestoreEnabled.get();
    }

    @Override
    public String getRestoreRepositoryName() {
        return restoreRepositoryName.get();
    }

    @Override
    public String getRestoreSourceClusterName() {
        return restoreSourceClusterName.get();
    }

    @Override
    public String getRestoreSourceRepositoryRegion() {
        return restoreSourceRepoRegion.get();
    }

    @Override
    public String getRestoreLocation() {
        return restoreLocation.get();
    }

    @Override
    public String getRestoreRepositoryType() {
        return restoreRepositoryType.get();
    }

    @Override
    public String getRestoreSnapshotName() {
        return restoreSnapshotName.get();
    }

    @Override
    public String getCommaSeparatedIndicesToRestore() {
        return commaSeparatedIndicesToRestore.get();
    }

    @Override
    public int getRestoreTaskInitialDelayInSeconds() {
        return restoreTaskInitialStartDelaySeconds.get();
    }

    @Override
    public boolean amITribeNode() {
        return amITribeNode.get();
    }

    @Override
    public boolean amIWriteEnabledTribeNode() {
        return amIWriteEnabledTribeNode.get();
    }

    @Override
    public boolean amIMetadataEnabledTribeNode() {
        return amIMetadataEnabledTribeNode.get();
    }

    @Override
    public String getCommaSeparatedSourceClustersForTribeNode() {
        return commaSeparatedSourceClustersInTribe.get();
    }

    @Override
    public boolean amISourceClusterForTribeNode() {
        return amISourceClusterForTribeNode.get();
    }

    @Override
    public String getCommaSeparatedTribeClusterNames() {
        return commaSeparatedTribeClusters.get();
    }

    @Override
    public boolean isNodeMismatchWithDiscoveryEnabled() {
        return isNodeMismatchWithDiscoveryEnabled.get();
    }

    @Override
    public int getDesiredNumberOfNodesInCluster() {
        return desiredNumNodesInCluster.get();
    }

    @Override
    public boolean isEurekaHealthCheckEnabled() {
        return isEurekaHealthCheckEnabled.get();
    }

    @Override
    public boolean isLocalModeEnabled() {
        return isLocalModeEnabled.get();
    }

    @Override
    public String getCassandraKeyspaceName() {
        return cassandraKeyspaceName.get();
    }

    @Override
    public int getCassandraThriftPortForAstyanax() {
        return cassandraThriftPort.get();
    }

    @Override
    public boolean isEurekaHostSupplierEnabled() {
        return isEurekaHostSupplierEnabled.get();
    }

    @Override
    public String getCommaSeparatedCassandraHostNames() {
        return commaSeparatedCassandraHostnames.get();
    }

    @Override
    public boolean isSecurityGroupInMultiDC() {
        return isSecurityGroupInMultiDc.get();
    }

    @Override
    public boolean isKibanaSetupRequired() {
        return isKibanaSetupRequired.get();
    }

    @Override
    public int getKibanaPort() {
        return kibanaPort.get();
    }

    @Override
    public boolean amISourceClusterForTribeNodeInMultiDC() {
        return amISourceClusterForTribeNodeInMultiDc.get();
    }

    @Override
    public boolean reportMetricsFromMasterOnly() {
        return reportMetricsFromMasterOnly.get();
    }

    @Override
    public String getTribePreferredClusterIdOnConflict() {
        return tribePreferredClusterIdOnConflict.get();
    }

    @Override
    public String getEsNodeName() {
        return ES_NODE_NAME;
    }

    @Override
    public boolean isDeployedInVPC() {
        return isDeployedInVpc;
    }

    @Override
    public boolean isVPCExternal() {
        return isVpcExternal;
    }

    @Override
    public String getACLGroupNameForVPC() {
        return aclGroupNameForVpc.get();
    }

    @Override
    public String getACLGroupIdForVPC() {
        return aclGroupIdForVpc;
    }

    @Override
    public void setACLGroupIdForVPC(String aclGroupIdForVPC) {
        aclGroupIdForVpc = aclGroupIdForVPC;
    }

    @Override
    public String getMacIdForInstance() {
        return MAC_ID;
    }
}
