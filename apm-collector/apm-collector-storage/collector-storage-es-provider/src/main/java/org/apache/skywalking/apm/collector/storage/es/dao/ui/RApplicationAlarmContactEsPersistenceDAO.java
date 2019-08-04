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

import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.core.util.CollectionUtils;
import org.apache.skywalking.apm.collector.storage.dao.ui.IRApplicationAlarmContactUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.alarm.RApplicationAlarmContactTable;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peng-yongsheng
 */
public class RApplicationAlarmContactEsPersistenceDAO extends EsDAO implements IRApplicationAlarmContactUIDAO {

    public RApplicationAlarmContactEsPersistenceDAO(ElasticSearchClient client) {
        super(client);
    }

    private String tableName() {
        return RApplicationAlarmContactTable.TABLE;
    }

    private XContentBuilder esStreamDataToEsData(IRApplicationAlarmContactUIDAO.RApplicationAlarmContact rApplicationAlarmContact) throws IOException {
        return XContentFactory.jsonBuilder().startObject()
                .field(RApplicationAlarmContactTable.CREATE_TIME.getName(), rApplicationAlarmContact.getCreateTime())
                .field(RApplicationAlarmContactTable.UPDATE_TIME.getName(), rApplicationAlarmContact.getUpdateTime())
                .field(RApplicationAlarmContactTable.STATUS.getName(), rApplicationAlarmContact.getStatus())
                .field(RApplicationAlarmContactTable.APPLICATION_ID.getName(), rApplicationAlarmContact.getApplicationId())
                .field(RApplicationAlarmContactTable.ALARM_CONTACT_ID.getName(), rApplicationAlarmContact.getAlarmContactId())
                .endObject();
    }


    @Override
    public Long deleteByApplicationId(Integer applicationId) {
        BulkByScrollResponse response = getClient().prepareDelete(
                QueryBuilders.termQuery(RApplicationAlarmContactTable.APPLICATION_ID.getName(), applicationId),
                tableName())
                .get();
        return response.getDeleted();
    }

    @Override
    public IndexRequestBuilder prepareBatchInsert(IRApplicationAlarmContactUIDAO.RApplicationAlarmContact rApplicationAlarmContact) throws IOException {
        XContentBuilder source = esStreamDataToEsData(rApplicationAlarmContact);
        return getClient().prepareIndex(tableName(), rApplicationAlarmContact.getId()).setSource(source);
    }

    @Override
    public List<RApplicationAlarmContact> getByApplicationId(Integer applicationId) {
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName());
        searchRequestBuilder.setTypes(RApplicationAlarmContactTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must().add(QueryBuilders.termQuery(RApplicationAlarmContactTable.APPLICATION_ID.getName(), applicationId));
        searchRequestBuilder.setQuery(boolQueryBuilder);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<RApplicationAlarmContact> applicationList = new ArrayList<RApplicationAlarmContact>();
        for (SearchHit searchHit : searchHits) {
            RApplicationAlarmContact application = new RApplicationAlarmContact();
            application.setApplicationId(((Number) searchHit.getSource().get(RApplicationAlarmContactTable.APPLICATION_ID.getName())).intValue());
            application.setAlarmContactId(((Number) searchHit.getSource().get(RApplicationAlarmContactTable.ALARM_CONTACT_ID.getName())).intValue());
            applicationList.add(application);
        }
        return applicationList;
    }

    @Override
    public Long deleteByAlarmContactId(Integer alarmContactId) {
        BulkByScrollResponse response = getClient().prepareDelete(
                QueryBuilders.termQuery(RApplicationAlarmContactTable.ALARM_CONTACT_ID.getName(), alarmContactId),
                tableName())
                .get();
        return response.getDeleted();
    }

    @Override
    public void batchPersistence(List<IndexRequestBuilder> collection) {
        BulkRequestBuilder bulkRequestBuilder = getClient().prepareBulk();
        if (CollectionUtils.isNotEmpty(collection)) {
            collection.forEach(builder -> {
                bulkRequestBuilder.add(builder.request());
            });
        }
        bulkRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
    }
}
