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

package org.apache.skywalking.apm.collector.ui.query;

import java.text.ParseException;
import java.util.List;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.storage.ui.common.*;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceInfo;
import org.apache.skywalking.apm.collector.ui.graphql.Query;
import org.apache.skywalking.apm.collector.ui.service.*;
import org.apache.skywalking.apm.collector.ui.utils.DurationUtils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import org.apache.skywalking.apm.collector.core.UnexpectedException;
import org.apache.skywalking.apm.collector.ui.utils.PaginationUtils;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricBrief;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricQueryCondition;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricQueryOrder;

/**
 * @author peng-yongsheng
 */
public class ServiceQuery implements Query {

    private final ModuleManager moduleManager;
    private ServiceNameService serviceNameService;
    private ServiceTopologyService serviceTopologyService;
    private ServiceReferenceMetricService serviceReferenceMetricService;

    public ServiceQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private ServiceNameService getServiceNameService() {
        if (isNull(serviceNameService)) {
            this.serviceNameService = new ServiceNameService(moduleManager);
        }
        return serviceNameService;
    }

    private ServiceTopologyService getServiceTopologyService() {
        if (isNull(serviceTopologyService)) {
            this.serviceTopologyService = new ServiceTopologyService(moduleManager);
        }
        return serviceTopologyService;
    }

    private ServiceReferenceMetricService getServiceReferenceMetricService() {
        if (isNull(serviceReferenceMetricService)) {
            this.serviceReferenceMetricService = new ServiceReferenceMetricService(moduleManager);
        }
        return serviceReferenceMetricService;
    }

    public List<ServiceInfo> searchService(String keyword, int applicationId, int topN) {
        return getServiceNameService().searchService(keyword, applicationId, topN);
    }

    public ResponseTimeTrend getServiceResponseTimeTrend(int serviceId, Duration duration) throws ParseException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());
        return getServiceNameService().getServiceResponseTimeTrend(serviceId, duration.getStep(), startTimeBucket, endTimeBucket);
    }

    public ThroughputTrend getServiceThroughputTrend(int serviceId, Duration duration) throws ParseException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());

        return getServiceNameService().getServiceThroughputTrend(serviceId, duration.getStep(), startTimeBucket, endTimeBucket);
    }

    public SLATrend getServiceSLATrend(int serviceId, Duration duration) throws ParseException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());
        return getServiceNameService().getServiceSLATrend(serviceId, duration.getStep(), startTimeBucket, endTimeBucket);
    }

    public Topology getServiceTopology(int serviceId, Duration duration) {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());

        long startSecondTimeBucket = DurationUtils.INSTANCE.startTimeDurationToSecondTimeBucket(duration.getStep(), duration.getStart());
        long endSecondTimeBucket = DurationUtils.INSTANCE.endTimeDurationToSecondTimeBucket(duration.getStep(), duration.getEnd());

        return getServiceTopologyService().getServiceTopology(duration.getStep(), serviceId, startTimeBucket, endTimeBucket, startSecondTimeBucket, endSecondTimeBucket);
    }

    public ServiceReferenceMetricBrief queryServiceReferenceMetricBrief(ServiceReferenceMetricQueryCondition condition) {
        long startSecondTimeBucket = 0;
        long endSecondTimeBucket = 0;
        if (nonNull(condition.getQueryDuration())) {
            startSecondTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(condition.getQueryDuration().getStart());
            endSecondTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(condition.getQueryDuration().getEnd());
        } else {
            throw new UnexpectedException("The condition must contains queryDuration.");
        }

        long minDuration = condition.getMinTransactionAverageDuration();
        long maxDuration = condition.getMaxTransactionAverageDuration();
        int behindApplicationId = condition.getBehindApplicationId();
        int frontApplicationId = condition.getFrontApplicationId();
        ServiceReferenceMetricQueryOrder queryOrder = condition.getQueryOrder();

        PaginationUtils.Page page = PaginationUtils.INSTANCE.exchange(condition.getPaging());
        return getServiceReferenceMetricService().getServiceReferenceMetricBrief(condition.getQueryDuration().getStep(), startSecondTimeBucket, endSecondTimeBucket, minDuration, maxDuration, MetricSource.Callee, frontApplicationId, behindApplicationId, page.getLimit(), page.getFrom(), queryOrder);
    }

}
