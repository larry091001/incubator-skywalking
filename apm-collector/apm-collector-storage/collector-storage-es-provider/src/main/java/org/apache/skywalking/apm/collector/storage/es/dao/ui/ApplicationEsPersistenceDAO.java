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
import org.apache.skywalking.apm.collector.core.util.BooleanUtils;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.core.util.StringUtils;
import org.apache.skywalking.apm.collector.storage.dao.ui.IApplicationUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.alarm.AlarmContactTable;
import org.apache.skywalking.apm.collector.storage.table.register.ApplicationTable;
import org.apache.skywalking.apm.collector.storage.ui.application.Application;
import org.apache.skywalking.apm.collector.storage.ui.application.ApplicationList;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

/**
 * @author peng-yongsheng
 */
public class ApplicationEsPersistenceDAO extends EsDAO implements IApplicationUIDAO {

    public ApplicationEsPersistenceDAO(ElasticSearchClient client) {
        super(client);
    }

    private String tableName() {
        return ApplicationTable.TABLE;
    }

    @Override
    public ApplicationList getApplications(String applicationCode, int limit, int from) {
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName());
        searchRequestBuilder.setTypes(AlarmContactTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(applicationCode)) {
            boolQueryBuilder.must().add(QueryBuilders.termQuery(ApplicationTable.APPLICATION_CODE.getName(), applicationCode));
        }
        boolQueryBuilder.must().add(QueryBuilders.termQuery(ApplicationTable.IS_ADDRESS.getName(), BooleanUtils.FALSE));
        boolQueryBuilder.mustNot().add(QueryBuilders.termQuery(ApplicationTable.APPLICATION_ID.getName(), Const.NONE_APPLICATION_ID));
        searchRequestBuilder.setQuery(boolQueryBuilder);
        searchRequestBuilder.setSize(limit);
        searchRequestBuilder.setFrom(from);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        ApplicationList applicationList = new ApplicationList();
        applicationList.setTotal((int) searchResponse.getHits().getTotalHits());
        for (SearchHit searchHit : searchHits) {
            Application application = new Application();
            application.setId(((Number) searchHit.getSource().get(ApplicationTable.APPLICATION_ID.getName())).intValue());
            application.setName((String) searchHit.getSource().get(ApplicationTable.APPLICATION_CODE.getName()));
            applicationList.getItems().add(application);
        }
        return applicationList;
    }
}
