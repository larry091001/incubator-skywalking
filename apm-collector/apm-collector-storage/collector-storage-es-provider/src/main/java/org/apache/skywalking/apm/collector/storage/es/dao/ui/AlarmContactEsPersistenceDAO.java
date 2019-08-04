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
import org.apache.skywalking.apm.collector.core.util.StringUtils;
import org.apache.skywalking.apm.collector.storage.dao.ui.IAlarmContactUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.alarm.AlarmContactTable;
import org.apache.skywalking.apm.collector.storage.ui.alarm.AlarmContactList;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author peng-yongsheng
 */
public class AlarmContactEsPersistenceDAO extends EsDAO implements IAlarmContactUIDAO {
    private final Logger logger = LoggerFactory.getLogger(AlarmContactEsPersistenceDAO.class);

    public AlarmContactEsPersistenceDAO(ElasticSearchClient client) {
        super(client);
    }

    private String tableName() {
        return AlarmContactTable.TABLE;
    }

    private IAlarmContactUIDAO.AlarmContact esDataToStreamData(Map<String, Object> source) {
        IAlarmContactUIDAO.AlarmContact instanceAlarm = new IAlarmContactUIDAO.AlarmContact();
        instanceAlarm.setCreateTime(((Number) source.get(AlarmContactTable.CREATE_TIME.getName())).longValue());
        instanceAlarm.setUpdateTime(((Number) source.get(AlarmContactTable.UPDATE_TIME.getName())).longValue());
        instanceAlarm.setStatus(((Number) source.get(AlarmContactTable.STATUS.getName())).intValue());

        instanceAlarm.setEmail((String) source.get(AlarmContactTable.EMAIL.getName()));
        instanceAlarm.setPhoneNumber((String) source.get(AlarmContactTable.PHONE_NUMBER.getName()));
        instanceAlarm.setRealName((String) source.get(AlarmContactTable.REAL_NAME.getName()));

        return instanceAlarm;
    }

    private XContentBuilder esStreamDataToEsData(IAlarmContactUIDAO.AlarmContact alarmContact) throws IOException {
        return XContentFactory.jsonBuilder().startObject()
                .field(AlarmContactTable.CREATE_TIME.getName(), alarmContact.getCreateTime())
                .field(AlarmContactTable.UPDATE_TIME.getName(), alarmContact.getUpdateTime())
                .field(AlarmContactTable.STATUS.getName(), alarmContact.getStatus())

                .field(AlarmContactTable.EMAIL.getName(), alarmContact.getEmail())
                .field(AlarmContactTable.PHONE_NUMBER.getName(), alarmContact.getPhoneNumber())
                .field(AlarmContactTable.REAL_NAME.getName(), alarmContact.getRealName())
                .field(AlarmContactTable.ALARM_CONTACT_ID.getName(), alarmContact.getAlarmContactId())
                .endObject();
    }


    @Override
    public AlarmContact get(String id) {
        GetResponse getResponse = getClient().prepareGet(tableName(), id).get();
        if (getResponse.isExists()) {
            AlarmContact alarmContact = esDataToStreamData(getResponse.getSource());
            alarmContact.setId(id);
            return alarmContact;
        } else {
            return null;
        }
    }

    @Override
    public Boolean save(AlarmContact data) throws IOException {
        XContentBuilder source = esStreamDataToEsData(data);
        IndexResponse response = getClient().prepareIndex(tableName(), data.getId()).setSource(source).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
        logger.debug("save status: {}", response.status().name());
        if (response.status() == RestStatus.CREATED) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean update(AlarmContact data) throws IOException {
        XContentBuilder source = XContentFactory.jsonBuilder().startObject()
                .field(AlarmContactTable.UPDATE_TIME.getName(), data.getUpdateTime());
        if (StringUtils.isNotEmpty(data.getEmail())) {
            source.field(AlarmContactTable.EMAIL.getName(), data.getEmail());
        }
        if (StringUtils.isNotEmpty(data.getPhoneNumber())) {
            source.field(AlarmContactTable.PHONE_NUMBER.getName(), data.getPhoneNumber());
        }
        if (StringUtils.isNotEmpty(data.getRealName())) {
            source.field(AlarmContactTable.REAL_NAME.getName(), data.getRealName());
        }
        source.endObject();
        UpdateRequestBuilder updateRequestBuilder = getClient().prepareUpdate(tableName(), data.getId());
        updateRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        updateRequestBuilder.setDoc(source);

        UpdateResponse response = updateRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
        if (response.status() == RestStatus.OK) {
            return true;
        }
        return false;
    }

    @Override
    public int getMaxAlarmContactId() {
        return getMaxId(AlarmContactTable.TABLE, AlarmContactTable.ALARM_CONTACT_ID.getName());
    }

    @Override
    public AlarmContactList loadAlarmContactList(String keyword, int limit, int from) {
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(AlarmContactTable.TABLE);
        searchRequestBuilder.setTypes(AlarmContactTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(keyword)) {
            boolQueryBuilder.must().add(QueryBuilders.matchQuery(AlarmContactTable.REAL_NAME.getName(), keyword));
        }

        searchRequestBuilder.setQuery(boolQueryBuilder);
        searchRequestBuilder.setSize(limit);
        searchRequestBuilder.setFrom(from);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        AlarmContactList alarmContactList = new AlarmContactList();
        alarmContactList.setTotal((int) searchResponse.getHits().getTotalHits());
        for (SearchHit searchHit : searchHits) {
            AlarmContact alarmContact = new AlarmContact();
            alarmContact.setId(String.valueOf(((Number) searchHit.getSource().get(AlarmContactTable.ALARM_CONTACT_ID.getName())).intValue()));
            alarmContact.setPhoneNumber((String) searchHit.getSource().get(AlarmContactTable.PHONE_NUMBER.getName()));
            alarmContact.setEmail((String) searchHit.getSource().get(AlarmContactTable.EMAIL.getName()));
            alarmContact.setRealName((String) searchHit.getSource().get(AlarmContactTable.REAL_NAME.getName()));
            alarmContact.setUpdateTime(((Number) searchHit.getSource().get(AlarmContactTable.UPDATE_TIME.getName())).longValue());
            alarmContact.setCreateTime(((Number) searchHit.getSource().get(AlarmContactTable.CREATE_TIME.getName())).longValue());
            alarmContact.setStatus(((Number) searchHit.getSource().get(AlarmContactTable.STATUS.getName())).intValue());
            alarmContactList.getItems().add(alarmContact);
        }
        return alarmContactList;
    }

    @Override
    public List<AlarmContact> loadAllAlarmContact() {
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(AlarmContactTable.TABLE);
        searchRequestBuilder.setTypes(AlarmContactTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        List<AlarmContact> alarmContacts = new ArrayList<AlarmContact>();
        for (SearchHit searchHit : searchHits) {
            AlarmContact alarmContact = new AlarmContact();
            alarmContact.setId(String.valueOf(((Number) searchHit.getSource().get(AlarmContactTable.ALARM_CONTACT_ID.getName())).intValue()));
            alarmContact.setPhoneNumber((String) searchHit.getSource().get(AlarmContactTable.PHONE_NUMBER.getName()));
            alarmContact.setEmail((String) searchHit.getSource().get(AlarmContactTable.EMAIL.getName()));
            alarmContact.setRealName((String) searchHit.getSource().get(AlarmContactTable.REAL_NAME.getName()));
            alarmContact.setUpdateTime(((Number) searchHit.getSource().get(AlarmContactTable.UPDATE_TIME.getName())).longValue());
            alarmContact.setCreateTime(((Number) searchHit.getSource().get(AlarmContactTable.CREATE_TIME.getName())).longValue());
            alarmContact.setStatus(((Number) searchHit.getSource().get(AlarmContactTable.STATUS.getName())).intValue());
            alarmContacts.add(alarmContact);
        }
        return alarmContacts;
    }

    @Override
    public Boolean delete(Integer alarmContactId) {
        DeleteResponse response = getClient().prepareDelete(tableName(), alarmContactId.toString()).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
        if (response.status() == RestStatus.OK) {
            return true;
        }
        return false;
    }
}
