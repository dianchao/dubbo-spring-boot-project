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
package com.alibaba.boot.dubbo.actuate.health;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.alibaba.boot.dubbo.actuate.health.DubboHealthIndicatorProperties.PREFIX;
import static com.alibaba.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * Dubbo {@link HealthIndicator}
 * 继承 AbstractHealthIndicator 抽象类，Dubbo Health Indicator 实现类。
 *
 *
 * @see HealthIndicator
 * @since 1.0.0
 */
public class DubboHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private DubboHealthIndicatorProperties dubboHealthIndicatorProperties;

    @Autowired(required = false)
    private Map<String, ProtocolConfig> protocolConfigs = Collections.emptyMap();

    @Autowired(required = false)
    private Map<String, ProviderConfig> providerConfigs = Collections.emptyMap();

    /**
     * 执行健康检查
     *
     * @param builder
     * @throws Exception
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // <1> 获取 StatusChecker 对应的 Dubbo ExtensionLoader 对象
        ExtensionLoader<StatusChecker> extensionLoader = getExtensionLoader(StatusChecker.class);

        // <2> 解析 StatusChecker 的名字的 Map
        Map<String, String> statusCheckerNamesMap = resolveStatusCheckerNamesMap();

        // <3> 声明 hasError、hasUnknown 变量
        boolean hasError = false; // 是否有错误的返回

        boolean hasUnknown = false; // 是否有未知的返回

        // <4> 先 builder 标记状态是 UP
        builder.up();

        // <5> 遍历 statusCheckerNamesMap 元素
        for (Map.Entry<String, String> entry : statusCheckerNamesMap.entrySet()) {
            // <6.1> 获取 StatusChecker 的名字
            String statusCheckerName = entry.getKey();
            // <6.2> 获取 source
            String source = entry.getValue();
            // <6.3> 获取 StatusChecker 对象
            StatusChecker checker = extensionLoader.getExtension(statusCheckerName);
            // <6.4> 执行校验
            com.alibaba.dubbo.common.status.Status status = checker.check();
            // <7.1> 获取校验结果
            com.alibaba.dubbo.common.status.Status.Level level = status.getLevel();
            // <7.2> 如果是 ERROR 检验结果，则标记 hasError 为 true ，并标记 builder 状态为 down
            if (!hasError && level.equals(com.alibaba.dubbo.common.status.Status.Level.ERROR)) {
                hasError = true;
                builder.down();
            }
            // <7.3> 如果是 UNKNOWN 检验结果，则标记 hasUnknown 为 true ，并标记 builder 状态为 unknown
            if (!hasError && !hasUnknown && level.equals(com.alibaba.dubbo.common.status.Status.Level.UNKNOWN)) {
                hasUnknown = true;
                builder.unknown();
            }
            // <8.1> 创建 detail Map
            Map<String, Object> detail = new LinkedHashMap<>();
            // <8.2> 设置 detail 属性值
            detail.put("source", source);
            detail.put("status", status);
            // <8.3> 添加到 builder 中
            builder.withDetail(statusCheckerName, detail);

        }


    }

    /**
     * Resolves the map of {@link StatusChecker}'s name and its' source.
     * 解析 StatusChecker 的名字的 Map
     *
     * KEY：StatusChecker 的名字
     * VALUE：配置的来源
     *
     * @return non-null {@link Map}
     */
    protected Map<String, String> resolveStatusCheckerNamesMap() {
        // 创建 Map
        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();
        // <1> 从 DubboHealthIndicatorProperties 中获取
        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromDubboHealthIndicatorProperties());
        // <2> 从 ProtocolConfig 中获取
        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromProtocolConfigs());
        // <3> 从 ProviderConfig 中获取
        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromProviderConfig());

        return statusCheckerNamesMap;

    }

    private Map<String, String> resolveStatusCheckerNamesMapFromDubboHealthIndicatorProperties() {
        // 获取 DubboHealthIndicatorProperties.Status
        DubboHealthIndicatorProperties.Status status =
                dubboHealthIndicatorProperties.getStatus();
        // 创建 Map
        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();
        // 1. 读取 defaults 属性
        for (String statusName : status.getDefaults()) {

            statusCheckerNamesMap.put(statusName, PREFIX + ".status.defaults");

        }
        // 2. 读取 extras 属性
        for (String statusName : status.getExtras()) {

            statusCheckerNamesMap.put(statusName, PREFIX + ".status.extras");

        }

        return statusCheckerNamesMap;

    }


    private Map<String, String> resolveStatusCheckerNamesMapFromProtocolConfigs() {
        // 创建 Map
        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();
        // 遍历 protocolConfigs
        for (Map.Entry<String, ProtocolConfig> entry : protocolConfigs.entrySet()) {
            // 获取 Bean 的名字
            String beanName = entry.getKey();
            // 获取 ProtocolConfig 对象
            ProtocolConfig protocolConfig = entry.getValue();
            // 获取 ProtocolConfig 的 StatusChecker 的名字的集合
            Set<String> statusCheckerNames = getStatusCheckerNames(protocolConfig);
            // 遍历 statusCheckerNames 数组
            for (String statusCheckerName : statusCheckerNames) {
                // 构建 source 属性
                String source = buildSource(beanName, protocolConfig);
                // 添加到 statusCheckerNamesMap 中
                statusCheckerNamesMap.put(statusCheckerName, source);

            }

        }

        return statusCheckerNamesMap;

    }

    private Map<String, String> resolveStatusCheckerNamesMapFromProviderConfig() {
        // 创建 Map
        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();
        // 遍历 providerConfigs
        for (Map.Entry<String, ProviderConfig> entry : providerConfigs.entrySet()) {
            // 获取 Bean 的名字
            String beanName = entry.getKey();
            // 获取 ProviderConfig 对象
            ProviderConfig providerConfig = entry.getValue();
            // 获取 ProtocolConfig 的 StatusChecker 的名字的集合
            Set<String> statusCheckerNames = getStatusCheckerNames(providerConfig);
            // 遍历 statusCheckerNames 数组
            for (String statusCheckerName : statusCheckerNames) {
                // 构建 source 属性
                String source = buildSource(beanName, providerConfig);
                // 添加到 statusCheckerNamesMap 中
                statusCheckerNamesMap.put(statusCheckerName, source);

            }

        }

        return statusCheckerNamesMap;

    }

    private Set<String> getStatusCheckerNames(ProtocolConfig protocolConfig) {
        String status = protocolConfig.getStatus();
        return StringUtils.commaDelimitedListToSet(status);
    }

    private Set<String> getStatusCheckerNames(ProviderConfig providerConfig) {
        String status = providerConfig.getStatus();
        return StringUtils.commaDelimitedListToSet(status);
    }

    private String buildSource(String beanName, Object bean) {
        return beanName + "@" + bean.getClass().getSimpleName() + ".getStatus()";
    }

}
