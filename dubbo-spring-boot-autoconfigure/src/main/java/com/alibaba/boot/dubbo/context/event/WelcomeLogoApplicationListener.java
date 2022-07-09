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
 */
package com.alibaba.boot.dubbo.context.event;

import com.alibaba.dubbo.common.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.boot.dubbo.util.DubboUtils.DUBBO_GITHUB_URL;
import static com.alibaba.boot.dubbo.util.DubboUtils.DUBBO_MAILING_LIST;
import static com.alibaba.boot.dubbo.util.DubboUtils.DUBBO_SPRING_BOOT_GITHUB_URL;
import static com.alibaba.boot.dubbo.util.DubboUtils.LINE_SEPARATOR;

/**
 * 实现 ApplicationListener 接口，处理 ApplicationEnvironmentPreparedEvent 事件，从而打印 Dubbo Banner 文本
 *
 * @see ApplicationListener
 * @since 1.0.0
 */
@Order(LoggingApplicationListener.DEFAULT_ORDER + 1)
public class WelcomeLogoApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    /**
     * 是否执行过
     *
     * 通过该变量，保证有且仅处理一次 ApplicationEnvironmentPreparedEvent 事件
     */
    private static AtomicBoolean processed = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        // 如果已经处理，则直接跳过
        if (processed.get()) {
            return;
        }

        // 获取 Logger 对象
        final Logger logger = LoggerFactory.getLogger(getClass());

        // 获取 Dubbo Banner 文本
        String bannerText = buildBannerText();

        if (logger.isInfoEnabled()) {
            logger.info(bannerText);
        } else {
            System.out.print(bannerText);
        }

        // 标记已执行
        processed.compareAndSet(false, true);
    }

    String buildBannerText() {

        StringBuilder bannerTextBuilder = new StringBuilder();

        bannerTextBuilder
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                .append(" :: Dubbo Spring Boot (v").append(Version.getVersion(getClass(), "1.0.0")).append(") : ")
                .append(DUBBO_SPRING_BOOT_GITHUB_URL)
                .append(LINE_SEPARATOR)
                .append(" :: Dubbo (v").append(Version.getVersion()).append(") : ")
                .append(DUBBO_GITHUB_URL)
                .append(LINE_SEPARATOR)
                .append(" :: Discuss group : ")
                .append(DUBBO_MAILING_LIST)
                .append(LINE_SEPARATOR)
        ;

        return bannerTextBuilder.toString();

    }

}
