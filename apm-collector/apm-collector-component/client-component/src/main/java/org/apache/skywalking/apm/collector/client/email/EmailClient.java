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

package org.apache.skywalking.apm.collector.client.email;

import com.sun.mail.util.MailSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Properties;

/**
 * @author peng-yongsheng
 */
public class EmailClient implements IEmailClient {
    private static final Logger logger = LoggerFactory.getLogger(EmailClient.class);
    private JavaMailSender sender;

    private final String host;
    private final String userName;
    private final String password;
    private final Boolean sslEnable;
    private final Boolean auth;
    private final Boolean starttlsEnable;
    private final Boolean starttlsRequired;


    public EmailClient(String host, String userName, String password, Boolean sslEnable, Boolean auth, Boolean starttlsEnable, Boolean starttlsRequired) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.sslEnable = sslEnable;
        this.auth = auth;
        this.starttlsEnable = starttlsEnable;
        this.starttlsRequired = starttlsRequired;
    }

    @Override
    public void initialize() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", auth);
        properties.put("mail.smtp.starttls.enable", starttlsEnable);
        properties.put("mail.smtp.starttls.required", starttlsRequired);
        if (sslEnable) {
            properties.put("mail.smtp.ssl.enable", "true");
            MailSSLSocketFactory sf = null;
            try {
                sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
            } catch (GeneralSecurityException e1) {
                e1.printStackTrace();
            }
            properties.put("mail.smtp.ssl.socketFactory", sf);
        }
        JavaMailSenderImpl senderImpl = new JavaMailSenderImpl();

        senderImpl.setHost(host);
        senderImpl.setUsername(userName);
        senderImpl.setPassword(password);
        senderImpl.setJavaMailProperties(properties);
        try {
            senderImpl.testConnection();
        } catch (Exception e) {
            logger.error("Mail Client Connection Failed", e);
        }
        sender = senderImpl;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void send(List<String> emails, String notice, String title) {
        logger.info("Start sending email notifications");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(userName);
        message.setSubject(title);
        message.setText(notice);
        if (!emails.isEmpty()) {
            message.setTo(emails.toArray(new String[0]));
            logger.info("sending.....");
            sender.send(message);
        }
        logger.info("End sending email notifications");
    }
}
