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

package org.apache.skywalking.apm.collector.ui.mutation;

import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.apache.skywalking.apm.collector.storage.dao.ui.IAlarmContactUIDAO;
import org.apache.skywalking.apm.collector.storage.ui.common.Response;
import org.apache.skywalking.apm.collector.ui.graphql.Mutation;
import org.apache.skywalking.apm.collector.ui.service.AlarmContactService;
import org.apache.skywalking.apm.collector.ui.service.RApplicationAlarmContactService;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * @author peng-yongsheng
 */
public class AlarmContactMutation implements Mutation {
    private AlarmContactService alarmContactService;
    private RApplicationAlarmContactService rApplicationAlarmContactService;
    private final ModuleManager moduleManager;

    public AlarmContactMutation(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private AlarmContactService getAlarmContactService() {
        if (isNull(alarmContactService)) {
            this.alarmContactService = new AlarmContactService(moduleManager);
        }
        return alarmContactService;
    }

    public RApplicationAlarmContactService getRApplicationAlarmContactService() {
        if (isNull(rApplicationAlarmContactService)) {
            this.rApplicationAlarmContactService = new RApplicationAlarmContactService(moduleManager);
        }
        return rApplicationAlarmContactService;
    }

    public Response addAlarmContact(String email, String phoneNumber, String realName) {
        Response response = new Response();
        try {
            IAlarmContactUIDAO.AlarmContact alarmContact = new IAlarmContactUIDAO.AlarmContact();
            alarmContact.setCreateTime(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
            alarmContact.setUpdateTime(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
            alarmContact.setRealName(realName);
            alarmContact.setPhoneNumber(phoneNumber);
            alarmContact.setEmail(email);
            if (getAlarmContactService().addAlarmContact(alarmContact)) {
                response.setErrCode(0);
                response.setErrMsg("success");
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setErrCode(-1);
        response.setErrMsg("error");
        return response;
    }

    public Response editAlarmContact(String id, String email, String phoneNumber, String realName) {
        Response response = new Response();
        try {
            IAlarmContactUIDAO.AlarmContact alarmContact = new IAlarmContactUIDAO.AlarmContact();
            alarmContact.setUpdateTime(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
            alarmContact.setRealName(realName);
            alarmContact.setPhoneNumber(phoneNumber);
            alarmContact.setEmail(email);
            alarmContact.setId(id);
            if (getAlarmContactService().editAlarmContact(alarmContact)) {
                response.setErrCode(0);
                response.setErrMsg("success");
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setErrCode(-1);
        response.setErrMsg("error");
        return response;
    }

    public Response setApplicationAlarmContact(List<Integer> alarmContactIds, Integer applicationId) {
        Response response = new Response();
        try {
            getRApplicationAlarmContactService().setApplicationAlarmContact(alarmContactIds, applicationId);
            response.setErrCode(0);
            response.setErrMsg("success");
        } catch (Exception e) {
            e.printStackTrace();
            response.setErrCode(-1);
            response.setErrMsg("error");
        }
        return response;
    }

    public Response deleteAlarmContact(Integer alarmContactId) {
        Response response = new Response();
        try {
            if (getAlarmContactService().deleteAlarmContact(alarmContactId)) {
                response.setErrCode(0);
                response.setErrMsg("success");
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setErrCode(-1);
        response.setErrMsg("error");
        return response;
    }
}
