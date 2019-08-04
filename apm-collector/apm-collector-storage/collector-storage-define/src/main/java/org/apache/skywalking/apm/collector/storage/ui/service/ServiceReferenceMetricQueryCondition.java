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

package org.apache.skywalking.apm.collector.storage.ui.service;

import org.apache.skywalking.apm.collector.storage.ui.common.Duration;
import org.apache.skywalking.apm.collector.storage.ui.common.Pagination;

/**
 * @author peng-yongsheng
 */
public class ServiceReferenceMetricQueryCondition {
    private int frontApplicationId;
    private int behindApplicationId;
    private Duration queryDuration;
    private int minTransactionAverageDuration;
    private int maxTransactionAverageDuration;
    private ServiceReferenceMetricQueryOrder queryOrder;
    private Pagination paging;

    public ServiceReferenceMetricQueryOrder getQueryOrder() {
        return queryOrder;
    }

    public void setQueryOrder(ServiceReferenceMetricQueryOrder queryOrder) {
        this.queryOrder = queryOrder;
    }

    public int getFrontApplicationId() {
        return frontApplicationId;
    }

    public void setFrontApplicationId(int frontApplicationId) {
        this.frontApplicationId = frontApplicationId;
    }

    public Duration getQueryDuration() {
        return queryDuration;
    }

    public void setQueryDuration(Duration queryDuration) {
        this.queryDuration = queryDuration;
    }

    public int getMinTransactionAverageDuration() {
        return minTransactionAverageDuration;
    }

    public void setMinTransactionAverageDuration(int minTransactionAverageDuration) {
        this.minTransactionAverageDuration = minTransactionAverageDuration;
    }

    public int getMaxTransactionAverageDuration() {
        return maxTransactionAverageDuration;
    }

    public void setMaxTransactionAverageDuration(int maxTransactionAverageDuration) {
        this.maxTransactionAverageDuration = maxTransactionAverageDuration;
    }

    public Pagination getPaging() {
        return paging;
    }

    public void setPaging(Pagination paging) {
        this.paging = paging;
    }

    public int getBehindApplicationId() {
        return behindApplicationId;
    }

    public void setBehindApplicationId(int behindApplicationId) {
        this.behindApplicationId = behindApplicationId;
    }
}
