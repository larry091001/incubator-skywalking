/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.collector.ui.service;

import org.apache.skywalking.apm.collector.cache.CacheModule;
import org.apache.skywalking.apm.collector.cache.service.ApplicationCacheService;
import org.apache.skywalking.apm.collector.cache.service.ServiceNameCacheService;
import org.apache.skywalking.apm.collector.configuration.ConfigurationModule;
import org.apache.skywalking.apm.collector.configuration.service.IComponentLibraryCatalogService;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.storage.StorageModule;
import org.apache.skywalking.apm.collector.storage.dao.ui.IServiceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.dao.ui.IServiceReferenceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.register.ServiceName;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricBrief;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricQueryOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class ServiceReferenceMetricService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceReferenceMetricService.class);

    private final ApplicationCacheService applicationCacheService;
    private final IServiceMetricUIDAO serviceMetricUIDAO;
    private final IServiceReferenceMetricUIDAO serviceReferenceMetricUIDAO;
    private final ServiceNameCacheService serviceNameCacheService;
    private final DateBetweenService dateBetweenService;
    private final IComponentLibraryCatalogService componentLibraryCatalogService;

    public ServiceReferenceMetricService(ModuleManager moduleManager) {
        this.serviceMetricUIDAO = moduleManager.find(StorageModule.NAME).getService(IServiceMetricUIDAO.class);
        this.serviceReferenceMetricUIDAO = moduleManager.find(StorageModule.NAME).getService(IServiceReferenceMetricUIDAO.class);
        this.applicationCacheService = moduleManager.find(CacheModule.NAME).getService(ApplicationCacheService.class);
        this.serviceNameCacheService = moduleManager.find(CacheModule.NAME).getService(ServiceNameCacheService.class);
        this.dateBetweenService = new DateBetweenService(moduleManager);
        this.componentLibraryCatalogService = moduleManager.find(ConfigurationModule.NAME).getService(IComponentLibraryCatalogService.class);
    }

    public ServiceReferenceMetricBrief getServiceReferenceMetricBrief(Step step, long startSecondTimeBucket, long endSecondTimeBucket, long minDuration, long maxDuration, MetricSource metricSource, int frontApplicationId, int behindApplicationId, int limit, int from, ServiceReferenceMetricQueryOrder queryOrder) {
        ServiceReferenceMetricBrief serviceReferenceMetricBrief = serviceReferenceMetricUIDAO.getServiceReferenceMetricBrief(step, startSecondTimeBucket, endSecondTimeBucket, minDuration, maxDuration, metricSource, frontApplicationId, behindApplicationId, limit, from, queryOrder);
        serviceReferenceMetricBrief.getServiceReferenceMetrics().forEach(reference -> {
            ServiceName frontServiceName = serviceNameCacheService.get(reference.getFrontServiceInfo().getId());
            reference.getFrontServiceInfo().setName(frontServiceName.getServiceName());
            reference.getFrontServiceInfo().setApplicationId(frontServiceName.getApplicationId());
            reference.getFrontServiceInfo().setApplicationName(applicationCacheService.getApplicationById(frontServiceName.getApplicationId()).getApplicationCode());
            ServiceName behindServiceName = serviceNameCacheService.get(reference.getBehindServiceInfo().getId());
            reference.getBehindServiceInfo().setName(behindServiceName.getServiceName());
            reference.getBehindServiceInfo().setApplicationId(behindServiceName.getApplicationId());
            reference.getBehindServiceInfo().setApplicationName(applicationCacheService.getApplicationById(behindServiceName.getApplicationId()).getApplicationCode());
        });
        return serviceReferenceMetricBrief;
    }

}
