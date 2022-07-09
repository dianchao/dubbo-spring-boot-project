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
package com.alibaba.boot.dubbo.env;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfigBinding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.ContextIdApplicationContextInitializer;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * DubboDefaultPropertiesEnvironmentPostProcessor ，实现 EnvironmentPostProcessor、Ordered 接口，
 * 生成 Dubbo 默认的配置，添加到 environment 中。而需要生成的 Dubbo 默认的配置如下：
 * # 直接复用 spring.application.name
 * dubbo.application.name=
 * # 默认为 true
 * dubbo.config.multiple=true
 * # 默认为 false
 * dubbo.config.qos-enable=false
 *
 * The lowest precedence {@link EnvironmentPostProcessor} processes
 * {@link SpringApplication#setDefaultProperties(Properties) Spring Boot default properties} for Dubbo
 * as late as possible before {@link ConfigurableApplicationContext#refresh() application context refresh}.
 */
public class DubboDefaultPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * The name of default {@link PropertySource} defined in SpringApplication#configurePropertySources method.
     */
    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";

    /**
     * The property name of Spring Application
     *
     * @see ContextIdApplicationContextInitializer
     */
    private static final String SPRING_APPLICATION_NAME_PROPERTY = "spring.application.name";

    /**
     * The property name of {@link ApplicationConfig}
     *
     * @see EnableDubboConfig
     * @see EnableDubboConfigBinding
     */
    private static final String DUBBO_APPLICATION_NAME_PROPERTY = "dubbo.application.name";

    /**
     * The property name of {@link EnableDubboConfig#multiple() @EnableDubboConfig.multiple()}
     */
    private static final String DUBBO_CONFIG_MULTIPLE_PROPERTY = "dubbo.config.multiple";

    /**
     * The property name of {@link ApplicationConfig#getQosEnable() application's QOS enable}
     */
    private static final String DUBBO_APPLICATION_QOS_ENABLE_PROPERTY = "dubbo.application.qos-enable";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        // <1> 生成 Dubbo 默认配置
        Map<String, Object> defaultProperties = createDefaultProperties(environment);
        // <2> 有默认配置，则添加到 environment 中
        if (!CollectionUtils.isEmpty(defaultProperties)) {
            addOrReplace(propertySources, defaultProperties);
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private Map<String, Object> createDefaultProperties(ConfigurableEnvironment environment) {
        Map<String, Object> defaultProperties = new HashMap<String, Object>();
        // "dubbo.application.name"
        setDubboApplicationNameProperty(environment, defaultProperties);
        // "dubbo.config.multiple"
        setDubboConfigMultipleProperty(defaultProperties);
        // "dubbo.application.qos-enable"
        setDubboApplicationQosEnableProperty(defaultProperties);
        return defaultProperties;
    }

    private void setDubboApplicationNameProperty(Environment environment, Map<String, Object> defaultProperties) {
        String springApplicationName = environment.getProperty(SPRING_APPLICATION_NAME_PROPERTY);
        if (StringUtils.hasLength(springApplicationName)
                && !environment.containsProperty(DUBBO_APPLICATION_NAME_PROPERTY)) {
            defaultProperties.put(DUBBO_APPLICATION_NAME_PROPERTY, springApplicationName);
        }
    }

    private void setDubboConfigMultipleProperty(Map<String, Object> defaultProperties) {
        defaultProperties.put(DUBBO_CONFIG_MULTIPLE_PROPERTY, Boolean.TRUE.toString());
    }

    private void setDubboApplicationQosEnableProperty(Map<String, Object> defaultProperties) {
        defaultProperties.put(DUBBO_APPLICATION_QOS_ENABLE_PROPERTY, Boolean.FALSE.toString());
    }

    /**
     * Copy from BusEnvironmentPostProcessor#addOrReplace(MutablePropertySources, Map)
     *
     * @param propertySources {@link MutablePropertySources}
     * @param map             Default Dubbo Properties
     */
    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        // 情况一，获取到 "defaultProperties" 对应的 PropertySource 对象，则进行替换
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) { // 找到
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            // 遍历 map 数组，进行替换到 "defaultProperties" 中
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        // 情况二，不存在 "defaultProperties" 对应的 PropertySource 对象，则进行添加
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}