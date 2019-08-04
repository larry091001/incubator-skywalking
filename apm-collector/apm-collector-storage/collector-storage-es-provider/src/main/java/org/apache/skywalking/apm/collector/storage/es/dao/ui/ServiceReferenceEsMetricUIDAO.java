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

package org.apache.skywalking.apm.collector.storage.es.dao.ui;

import java.util.LinkedList;
import java.util.List;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.storage.dao.ui.IServiceReferenceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.service.ServiceReferenceMetricTable;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.utils.TimePyramidTableNameBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricBrief;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceReferenceMetricQueryOrder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

/**
 * @author peng-yongsheng
 */
public class ServiceReferenceEsMetricUIDAO extends EsDAO implements IServiceReferenceMetricUIDAO {

    public ServiceReferenceEsMetricUIDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override public List<ServiceReferenceMetric> getFrontServices(Step step, long startTimeBucket, long endTimeBucket,
        MetricSource metricSource,
        int behindServiceId) {
        String tableName = TimePyramidTableNameBuilder.build(step, ServiceReferenceMetricTable.TABLE);

        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName);
        searchRequestBuilder.setTypes(ServiceReferenceMetricTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must().add(QueryBuilders.rangeQuery(ServiceReferenceMetricTable.TIME_BUCKET.getName()).gte(startTimeBucket).lte(endTimeBucket));
        boolQuery.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.BEHIND_SERVICE_ID.getName(), behindServiceId));
        boolQuery.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.SOURCE_VALUE.getName(), metricSource.getValue()));

        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.setSize(0);

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(ServiceReferenceMetricTable.FRONT_SERVICE_ID.getName()).field(ServiceReferenceMetricTable.FRONT_SERVICE_ID.getName()).size(100);
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName()).field(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName()).field(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName()).field(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName()).field(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName()));

        searchRequestBuilder.addAggregation(aggregationBuilder);
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        List<ServiceReferenceMetric> referenceMetrics = new LinkedList<>();
        Terms frontServiceIdTerms = searchResponse.getAggregations().get(ServiceReferenceMetricTable.FRONT_SERVICE_ID.getName());
        buildNodeByBehindServiceId(referenceMetrics, frontServiceIdTerms, behindServiceId);

        return referenceMetrics;
    }

    @Override public List<ServiceReferenceMetric> getBehindServices(Step step, long startTimeBucket, long endTimeBucket,
        MetricSource metricSource,
        int frontServiceId) {
        String tableName = TimePyramidTableNameBuilder.build(step, ServiceReferenceMetricTable.TABLE);

        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName);
        searchRequestBuilder.setTypes(ServiceReferenceMetricTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must().add(QueryBuilders.rangeQuery(ServiceReferenceMetricTable.TIME_BUCKET.getName()).gte(startTimeBucket).lte(endTimeBucket));
        boolQuery.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.FRONT_SERVICE_ID.getName(), frontServiceId));
        boolQuery.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.SOURCE_VALUE.getName(), metricSource.getValue()));

        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.setSize(0);

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(ServiceReferenceMetricTable.BEHIND_SERVICE_ID.getName()).field(ServiceReferenceMetricTable.BEHIND_SERVICE_ID.getName()).size(100);
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName()).field(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName()).field(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName()).field(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName()));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName()).field(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName()));

        searchRequestBuilder.addAggregation(aggregationBuilder);
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        List<ServiceReferenceMetric> referenceMetrics = new LinkedList<>();
        Terms behindServiceIdTerms = searchResponse.getAggregations().get(ServiceReferenceMetricTable.BEHIND_SERVICE_ID.getName());
        buildNodeByFrontServiceId(referenceMetrics, behindServiceIdTerms, frontServiceId);

        return referenceMetrics;
    }

    private void buildNodeByFrontServiceId(List<ServiceReferenceMetric> referenceMetrics, Terms behindServiceIdTerms,
        int frontServiceId) {
        behindServiceIdTerms.getBuckets().forEach(behindServiceIdBucket -> {
            int behindServiceId = behindServiceIdBucket.getKeyAsNumber().intValue();

            Sum callsSum = behindServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName());
            Sum errorCallsSum = behindServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName());
            Sum durationSum = behindServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName());
            Sum errorDurationSum = behindServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName());

            ServiceReferenceMetric referenceMetric = new ServiceReferenceMetric();
            referenceMetric.setSource(frontServiceId);
            referenceMetric.setTarget(behindServiceId);
            referenceMetric.setCalls((long)callsSum.getValue());
            referenceMetric.setErrorCalls((long)errorCallsSum.getValue());
            referenceMetric.setDurations((long)durationSum.getValue());
            referenceMetric.setErrorDurations((long)errorDurationSum.getValue());
            referenceMetrics.add(referenceMetric);
        });
    }

    private void buildNodeByBehindServiceId(List<ServiceReferenceMetric> referenceMetrics, Terms frontServiceIdTerms,
        int behindServiceId) {
        frontServiceIdTerms.getBuckets().forEach(frontServiceIdBucket -> {
            int frontServiceId = frontServiceIdBucket.getKeyAsNumber().intValue();

            Sum callsSum = frontServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName());
            Sum errorCallsSum = frontServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName());
            Sum durationSum = frontServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_DURATION_SUM.getName());
            Sum errorDurationSum = frontServiceIdBucket.getAggregations().get(ServiceReferenceMetricTable.TRANSACTION_ERROR_DURATION_SUM.getName());

            ServiceReferenceMetric referenceMetric = new ServiceReferenceMetric();
            referenceMetric.setTarget(behindServiceId);
            referenceMetric.setSource(frontServiceId);
            referenceMetric.setCalls((long)callsSum.getValue());
            referenceMetric.setErrorCalls((long)errorCallsSum.getValue());
            referenceMetric.setDurations((long)durationSum.getValue());
            referenceMetric.setErrorDurations((long)errorDurationSum.getValue());
            referenceMetrics.add(referenceMetric);
        });
    }

    @Override
    public ServiceReferenceMetricBrief getServiceReferenceMetricBrief(Step step, long startSecondTimeBucket, long endSecondTimeBucket, long minDuration, long maxDuration, MetricSource metricSource, int frontApplicationId, int behindApplicationId, int limit, int from, ServiceReferenceMetricQueryOrder queryOrder) {
        String tableName = TimePyramidTableNameBuilder.build(step, ServiceReferenceMetricTable.TABLE);
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName);
        searchRequestBuilder.setTypes(ServiceReferenceMetricTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchRequestBuilder.setQuery(boolQueryBuilder);
        List<QueryBuilder> mustQueryList = boolQueryBuilder.must();

        if (startSecondTimeBucket != 0 && endSecondTimeBucket != 0) {
            mustQueryList.add(QueryBuilders.rangeQuery(ServiceReferenceMetricTable.TIME_BUCKET.getName()).gte(startSecondTimeBucket).lte(endSecondTimeBucket));
        }

        if (minDuration != 0 || maxDuration != 0) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(ServiceReferenceMetricTable.TRANSACTION_AVERAGE_DURATION.getName());
            if (minDuration != 0) {
                rangeQueryBuilder.gte(minDuration);
            }
            if (maxDuration != 0) {
                rangeQueryBuilder.lte(maxDuration);
            }
            boolQueryBuilder.must().add(rangeQueryBuilder);
        }
        if (behindApplicationId != 0) {
            boolQueryBuilder.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.BEHIND_APPLICATION_ID.getName(), behindApplicationId));
        }
        if (frontApplicationId != 0) {
            boolQueryBuilder.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.FRONT_APPLICATION_ID.getName(), frontApplicationId));
        }
        boolQueryBuilder.must().add(QueryBuilders.termQuery(ServiceReferenceMetricTable.SOURCE_VALUE.getName(), metricSource.getValue()));
        switch (queryOrder) {
            case BY_TIME_BUCKET:
                searchRequestBuilder.addSort(ServiceReferenceMetricTable.TIME_BUCKET.getName(), SortOrder.DESC);
                break;
            case BY_TRANSACTION_AVERAGE_DURATION:
                searchRequestBuilder.addSort(ServiceReferenceMetricTable.TRANSACTION_AVERAGE_DURATION.getName(), SortOrder.DESC);
                break;
            case BY_TRANSACTION_CALLS:
                searchRequestBuilder.addSort(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName(), SortOrder.DESC);
                break;
            case BY_TRANSACTION_ERROR_CALLS:
                searchRequestBuilder.addSort(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName(), SortOrder.DESC);
                break;
        }
        searchRequestBuilder.setSize(limit);
        searchRequestBuilder.setFrom(from);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        ServiceReferenceMetricBrief serviceReferenceMetricBrief = new ServiceReferenceMetricBrief();
        serviceReferenceMetricBrief.setTotal((int) searchResponse.getHits().totalHits);

        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            ServiceReferenceMetric serviceReferenceMetric = new ServiceReferenceMetric();
            serviceReferenceMetric.setId(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.TIME_BUCKET.getName())).longValue() + "_" + searchHit.getSource().get(ServiceReferenceMetricTable.METRIC_ID.getName()).toString());
            serviceReferenceMetric.setAverageDuration(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.TRANSACTION_AVERAGE_DURATION.getName())).intValue());
            serviceReferenceMetric.setCalls(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.TRANSACTION_CALLS.getName())).longValue());
            serviceReferenceMetric.setErrorCalls(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.TRANSACTION_ERROR_CALLS.getName())).longValue());
            serviceReferenceMetric.getBehindServiceInfo().setId(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.BEHIND_SERVICE_ID.getName())).intValue());
            serviceReferenceMetric.getFrontServiceInfo().setId(((Number) searchHit.getSource().get(ServiceReferenceMetricTable.FRONT_SERVICE_ID.getName())).intValue());
            serviceReferenceMetricBrief.getServiceReferenceMetrics().add(serviceReferenceMetric);
        }

        return serviceReferenceMetricBrief;
    }

}
