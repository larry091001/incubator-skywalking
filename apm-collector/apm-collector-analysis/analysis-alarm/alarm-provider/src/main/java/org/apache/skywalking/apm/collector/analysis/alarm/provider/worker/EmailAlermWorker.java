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

package org.apache.skywalking.apm.collector.analysis.alarm.provider.worker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.skywalking.apm.collector.analysis.alarm.define.graph.AlarmWorkerIdDefine;
import org.apache.skywalking.apm.collector.cache.CacheModule;
import org.apache.skywalking.apm.collector.cache.service.ApplicationCacheService;
import org.apache.skywalking.apm.collector.cache.service.ServiceNameCacheService;
import org.apache.skywalking.apm.collector.client.email.EmailClient;
import org.apache.skywalking.apm.collector.client.email.IEmailClient;
import org.apache.skywalking.apm.collector.configuration.ConfigurationModule;
import org.apache.skywalking.apm.collector.core.data.StreamData;
import org.apache.skywalking.apm.collector.core.graph.Next;
import org.apache.skywalking.apm.collector.core.graph.NodeProcessor;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.core.util.StringUtils;
import org.apache.skywalking.apm.collector.storage.StorageModule;
import org.apache.skywalking.apm.collector.storage.dao.ui.IAlarmContactUIDAO;
import org.apache.skywalking.apm.collector.storage.dao.ui.IInstanceUIDAO;
import org.apache.skywalking.apm.collector.storage.dao.ui.IRApplicationAlarmContactUIDAO;
import org.apache.skywalking.apm.collector.storage.table.alarm.*;
import org.apache.skywalking.apm.collector.storage.table.register.Instance;
import org.apache.skywalking.apm.collector.storage.table.register.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peng-yongsheng
 */
public class EmailAlermWorker<INPUT extends StreamData & Alarm> implements NodeProcessor<INPUT, INPUT> {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlermWorker.class);
    private final ModuleManager moduleManager;
    private final ServiceNameCacheService serviceNameCacheService;
    private final ApplicationCacheService applicationCacheService;
    private final EmailClient emailClient;
    private static final String RESPONSE_TIME_ALARM = " 响应时间报警!";
    private static final String SUCCESS_RATE_ALARM = " 成功率报警!";
    private final Gson gson = new Gson();
    private final IInstanceUIDAO instanceDAO;
    private final IAlarmContactUIDAO alarmContactUIDAO;
    private final IRApplicationAlarmContactUIDAO rApplicationAlarmContactUIDAO;

    public EmailAlermWorker(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.emailClient = (EmailClient) moduleManager.find(ConfigurationModule.NAME).getService(IEmailClient.class);
        this.applicationCacheService = moduleManager.find(CacheModule.NAME).getService(ApplicationCacheService.class);
        this.serviceNameCacheService = moduleManager.find(CacheModule.NAME).getService(ServiceNameCacheService.class);
        this.instanceDAO = moduleManager.find(StorageModule.NAME).getService(IInstanceUIDAO.class);
        this.alarmContactUIDAO = moduleManager.find(StorageModule.NAME).getService(IAlarmContactUIDAO.class);
        this.rApplicationAlarmContactUIDAO = moduleManager.find(StorageModule.NAME).getService(IRApplicationAlarmContactUIDAO.class);
    }

    @Override
    public int id() {
        return AlarmWorkerIdDefine.EMAIL_ALREM_WORKER_ID;
    }

    @Override
    public void process(INPUT input, Next<INPUT> next) {
        if (emailClient != null) {
            try {
                String title = null;
                Integer applicationId = null;
                if (input instanceof ServiceAlarm) {
                    ServiceAlarm serviceAlarm = (ServiceAlarm) input;
                    ServiceName serviceName = serviceNameCacheService.get(serviceAlarm.getServiceId());
                    String applicationCode = applicationCacheService.getApplicationById(serviceAlarm.getApplicationId()).getApplicationCode();
                    if (serviceAlarm.getAlarmType() == AlarmType.SLOW_RTT.getValue()) {
                        title = "应用[" + applicationCode + "]服务[ " + serviceName.getServiceName() + "]" + RESPONSE_TIME_ALARM;
                    } else if (serviceAlarm.getAlarmType() == AlarmType.ERROR_RATE.getValue()) {
                        title = "应用[" + applicationCode + "]服务[ " + serviceName.getServiceName() + "]" + SUCCESS_RATE_ALARM;
                    }
                    applicationId = serviceAlarm.getApplicationId();
                } else if (input instanceof InstanceAlarm) {
                    InstanceAlarm instanceAlarm = (InstanceAlarm) input;
                    Instance instance = instanceDAO.getInstance(instanceAlarm.getInstanceId());
                    String applicationCode = applicationCacheService.getApplicationById(instanceAlarm.getApplicationId()).getApplicationCode();
                    String serverName = buildServerName(instance.getOsInfo());
                    if (instanceAlarm.getAlarmType() == AlarmType.SLOW_RTT.getValue()) {
                        title = "应用[" + applicationCode + "]主机[ " + serverName + "]" + RESPONSE_TIME_ALARM;
                    } else if (instanceAlarm.getAlarmType() == AlarmType.ERROR_RATE.getValue()) {
                        title = "应用[" + applicationCode + "]主机[ " + serverName + "]" + SUCCESS_RATE_ALARM;
                    }
                    applicationId = instanceAlarm.getApplicationId();
                } else if (input instanceof ApplicationAlarm) {
                    ApplicationAlarm applicationAlarm = (ApplicationAlarm) input;
                    String applicationCode = applicationCacheService.getApplicationById(applicationAlarm.getApplicationId()).getApplicationCode();
                    if (applicationAlarm.getAlarmType() == AlarmType.SLOW_RTT.getValue()) {
                        title = "应用[" + applicationCode + "]" + RESPONSE_TIME_ALARM;
                    } else if (applicationAlarm.getAlarmType() == AlarmType.ERROR_RATE.getValue()) {
                        title = "应用[" + applicationCode + "]" + SUCCESS_RATE_ALARM;
                    }
                    applicationId = applicationAlarm.getApplicationId();
                }
                if (applicationId != null) {
                    List<String> emails = new ArrayList<>();
                    List<IRApplicationAlarmContactUIDAO.RApplicationAlarmContact> rApplicationAlarmContacts = rApplicationAlarmContactUIDAO.getByApplicationId(applicationId);
                    for (IRApplicationAlarmContactUIDAO.RApplicationAlarmContact rApplicationAlarmContact : rApplicationAlarmContacts) {
                        IAlarmContactUIDAO.AlarmContact alarmContact = alarmContactUIDAO.get(rApplicationAlarmContact.getAlarmContactId().toString());
                        if (alarmContact != null && StringUtils.isNotEmpty(alarmContact.getEmail())) {
                            emails.add(alarmContact.getEmail());
                        }
                    }
                    if (emails.size() > 0) {
                        emailClient.send(emails, input.getAlarmContent(), title);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        next.execute(input);
    }

    private String buildServerName(String osInfoJson) {
        JsonObject osInfo = gson.fromJson(osInfoJson, JsonObject.class);
        String serverName = Const.UNKNOWN;
        if (osInfo != null && osInfo.has("hostName")) {
            serverName = osInfo.get("hostName").getAsString();
        }
        return serverName;
    }
}