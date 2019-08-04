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

package org.apache.skywalking.apm.collector.storage.dao.ui;

import java.util.List;
import org.apache.skywalking.apm.collector.storage.base.dao.DAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceInfo;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricBrief;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricQueryOrder;
/**
 * Interface to be implemented for execute database query operation
 * from {@link org.apache.skywalking.apm.collector.storage.table.service.ServiceReferenceMetricTable#TABLE}.
 *
 * @author peng-yongsheng
 * @see org.apache.skywalking.apm.collector.storage.table.service.ServiceReferenceMetricTable
 * @see org.apache.skywalking.apm.collector.storage.StorageModule
 */
public interface IServiceReferenceMetricUIDAO extends DAO {

    /**
     * Returns the service reference metrics which call the given service id
     * that collected between start time bucket and end time bucket.
     *
     * <p>SQL as: select FRONT_SERVICE_ID, sum(TRANSACTION_CALLS), sum(TRANSACTION_ERROR_CALLS),
     * sum(TRANSACTION_DURATION_SUM), sum(TRANSACTION_ERROR_DURATION_SUM)
     * from SERVICE_REFERENCE_METRIC
     * where TIME_BUCKET ge ${startTimeBucket} and TIME_BUCKET le ${endTimeBucket}
     * and SOURCE_VALUE = ${metricSource}
     * and BEHIND_SERVICE_ID = ${behindServiceId}
     * group by FRONT_SERVICE_ID
     *
     * <p>Use {@link org.apache.skywalking.apm.collector.storage.utils.TimePyramidTableNameBuilder#build(Step, String)}
     * to generate table name which mixed with step name.
     *
     * @param step the step which represent time formats
     * @param startTimeBucket start time bucket
     * @param endTimeBucket end time bucket
     * @param metricSource source of this metric, server side or client side
     * @param behindServiceId the callee service id
     * @return not nullable result list
     */
    List<ServiceReferenceMetric> getFrontServices(Step step, long startTimeBucket, long endTimeBucket,
        MetricSource metricSource, int behindServiceId);

    /**
     * Returns the service reference metrics which call from the given service id
     * that collected between start time bucket and end time bucket.
     *
     * <p>SQL as: select FRONT_SERVICE_ID, sum(TRANSACTION_CALLS), sum(TRANSACTION_ERROR_CALLS),
     * sum(TRANSACTION_DURATION_SUM), sum(TRANSACTION_ERROR_DURATION_SUM)
     * from SERVICE_REFERENCE_METRIC
     * where TIME_BUCKET ge ${startTimeBucket} and TIME_BUCKET le ${endTimeBucket}
     * and SOURCE_VALUE = ${metricSource}
     * and BEHIND_SERVICE_ID = ${frontServiceId}
     * group by BEHIND_SERVICE_ID
     *
     * <p>Use {@link org.apache.skywalking.apm.collector.storage.utils.TimePyramidTableNameBuilder#build(Step, String)}
     * to generate table name which mixed with step name.
     *
     * @param step the step which represent time formats
     * @param startTimeBucket start time bucket
     * @param endTimeBucket end time bucket
     * @param metricSource source of this metric, server side or client side
     * @param frontServiceId the caller service id
     * @return not nullable result list
     */
    List<ServiceReferenceMetric> getBehindServices(Step step, long startTimeBucket, long endTimeBucket,
        MetricSource metricSource, int frontServiceId);

    ServiceReferenceMetricBrief getServiceReferenceMetricBrief(Step step, long startSecondTimeBucket, long endSecondTimeBucket, long minDuration, long maxDuration, MetricSource metricSource, int frontApplicationId, int behindApplicationId, int limit, int from, ServiceReferenceMetricQueryOrder queryOrder);

    class ServiceReferenceMetric {
        private String id;
        private ServiceInfo behindServiceInfo;
        private ServiceInfo frontServiceInfo;
        private int source;
        private int target;
        private long calls;
        private long errorCalls;
        private long durations;
        private long errorDurations;
        private long averageDuration;

        public ServiceReferenceMetric() {
            this.behindServiceInfo = new ServiceInfo();
            this.frontServiceInfo = new ServiceInfo();
        }

        public int getSource() {
            return source;
        }

        public void setSource(int source) {
            this.source = source;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(int target) {
            this.target = target;
        }

        public long getCalls() {
            return calls;
        }

        public void setCalls(long calls) {
            this.calls = calls;
        }

        public long getErrorCalls() {
            return errorCalls;
        }

        public void setErrorCalls(long errorCalls) {
            this.errorCalls = errorCalls;
        }

        public long getDurations() {
            return durations;
        }

        public void setDurations(long durations) {
            this.durations = durations;
        }

        public long getErrorDurations() {
            return errorDurations;
        }

        public void setErrorDurations(long errorDurations) {
            this.errorDurations = errorDurations;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ServiceInfo getBehindServiceInfo() {
            return behindServiceInfo;
        }

        public void setBehindServiceInfo(ServiceInfo behindServiceInfo) {
            this.behindServiceInfo = behindServiceInfo;
        }

        public ServiceInfo getFrontServiceInfo() {
            return frontServiceInfo;
        }

        public void setFrontServiceInfo(ServiceInfo frontServiceInfo) {
            this.frontServiceInfo = frontServiceInfo;
        }

        public long getAverageDuration() {
            return averageDuration;
        }

        public void setAverageDuration(long averageDuration) {
            this.averageDuration = averageDuration;
        }
    }
}
