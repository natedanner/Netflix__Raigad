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
package com.netflix.raigad.aws;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.raigad.configuration.IConfiguration;
import com.netflix.raigad.identity.IMembership;
import com.netflix.raigad.identity.IRaigadInstanceFactory;
import com.netflix.raigad.identity.InstanceManager;
import com.netflix.raigad.identity.RaigadInstance;
import com.netflix.raigad.scheduler.SimpleTimer;
import com.netflix.raigad.scheduler.Task;
import com.netflix.raigad.scheduler.TaskTimer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This class will associate public IP's with a new instance so they can talk across the regions.
 * <p>
 * Requirements:
 * 1. Nodes in the same region needs to be able to talk to each other.
 * 2. Nodes in other regions needs to be able to talk to the others in the other region.
 * <p>
 * Assumptions:
 * 1. IRaigadInstanceFactory will provide the membership and will be visible across the regions
 * 2. IMembership amazon or any other implementation which can tell if the instance is a
 * part of the group (ASG in Amazon's case).
 */

@Singleton
public class UpdateSecuritySettings extends Task {
    private static final Logger logger = LoggerFactory.getLogger(UpdateSecuritySettings.class);

    public static final String JOB_NAME = "Update_SG";
    public static boolean firstTimeUpdated;
    private static final Random RANDOM = new Random();

    private final IMembership membership;
    private final IRaigadInstanceFactory factory;


    @Inject
    public UpdateSecuritySettings(IConfiguration config, IMembership membership, IRaigadInstanceFactory factory) {
        super(config);
        this.membership = membership;
        this.factory = factory;
    }

    /**
     * Master nodes execute this at the specified interval, others run only on startup
     */
    @Override
    public void execute() {
        int transportPort = config.getTransportTcpPort();
        int restPort = config.getHttpPort();

        List<String> accessControlLists = membership.listACL(transportPort, transportPort);

        // Get instances based on node types (tribe / non-tribe)
        List<RaigadInstance> instances = getInstanceList();

        // Iterate cluster nodes and build a list of IP's
        List<String> ipsToAdd = Lists.newArrayList();
        List<String> currentRanges = Lists.newArrayList();
        for (RaigadInstance instance : instances) {
            String range = instance.getHostIP() + "/32";
            currentRanges.add(range);
            if (!accessControlLists.contains(range)) {
                ipsToAdd.add(range);
            }
        }

        if (!ipsToAdd.isEmpty()) {
            logger.info("Adding IPs on ports {} and {}: {}", transportPort, restPort, ipsToAdd);
            membership.addACL(ipsToAdd, transportPort, transportPort);
            membership.addACL(ipsToAdd, restPort, restPort);
            firstTimeUpdated = true;
        }

        // Create a list of IP's to remove
        List<String> ipsToRemove = Lists.newArrayList();
        for (String accessControlList : accessControlLists) {
            // Remove if not found
            if (!currentRanges.contains(accessControlList)) {
                ipsToRemove.add(accessControlList);
            }
        }

        if (!ipsToRemove.isEmpty()) {
            logger.info("Removing IPs on ports {} and {}: {}", transportPort, restPort, ipsToRemove);
            membership.removeACL(ipsToRemove, transportPort, transportPort);
            membership.removeACL(ipsToRemove, restPort, restPort);
            firstTimeUpdated = true;
        }
    }

    private List<RaigadInstance> getInstanceList() {
        List<RaigadInstance> instances = new ArrayList<>();

        List<String> tribeClusters = new ArrayList<>(Arrays.asList(StringUtils.split(config.getCommaSeparatedTribeClusterNames(), ",")));
        assert (!tribeClusters.isEmpty()) : "Need at least one tribe cluster";

        tribeClusters.forEach(tribeClusterName -> instances.addAll(factory.getAllIds(tribeClusterName)));

        if (config.isDebugEnabled()) {
            instances.forEach(instance -> logger.debug(instance.toString()));
        }

        return instances;
    }

    public static TaskTimer getTimer(InstanceManager instanceManager) {
        // Only master nodes will update security group settings
        if (!instanceManager.isMaster()) {
            return new SimpleTimer(JOB_NAME);
        } else {
            return new SimpleTimer(JOB_NAME, 120 * 1000 + RANDOM.nextInt(120 * 1000));
        }
    }

    @Override
    public String getName() {
        return JOB_NAME;
    }
}