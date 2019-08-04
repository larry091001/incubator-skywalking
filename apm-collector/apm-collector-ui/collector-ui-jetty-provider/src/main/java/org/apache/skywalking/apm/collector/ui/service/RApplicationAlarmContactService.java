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

import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.apache.skywalking.apm.collector.storage.StorageModule;
import org.apache.skywalking.apm.collector.storage.dao.ui.IRApplicationAlarmContactUIDAO;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peng-yongsheng
 */
public class RApplicationAlarmContactService {

    private static final Logger logger = LoggerFactory.getLogger(RApplicationAlarmContactService.class);

    private final IRApplicationAlarmContactUIDAO rApplicationAlarmContactUIDAO;

    public RApplicationAlarmContactService(ModuleManager moduleManager) {
        this.rApplicationAlarmContactUIDAO = moduleManager.find(StorageModule.NAME).getService(IRApplicationAlarmContactUIDAO.class);
    }

    public void setApplicationAlarmContact(List<Integer> alarmContactIds, Integer applicationId) throws Exception {
        rApplicationAlarmContactUIDAO.deleteByApplicationId(applicationId);
        if (alarmContactIds.size() > 0) {
            List<IndexRequestBuilder> collection = new ArrayList<IndexRequestBuilder>();
            for (Integer alarmContactId : alarmContactIds) {
                IRApplicationAlarmContactUIDAO.RApplicationAlarmContact rApplicationAlarmContact = new IRApplicationAlarmContactUIDAO.RApplicationAlarmContact();
                rApplicationAlarmContact.setCreateTime(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
                rApplicationAlarmContact.setUpdateTime(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
                rApplicationAlarmContact.setAlarmContactId(alarmContactId);
                rApplicationAlarmContact.setApplicationId(applicationId);
                collection.add(rApplicationAlarmContactUIDAO.prepareBatchInsert(rApplicationAlarmContact));
            }
            rApplicationAlarmContactUIDAO.batchPersistence(collection);
        }
    }
}
