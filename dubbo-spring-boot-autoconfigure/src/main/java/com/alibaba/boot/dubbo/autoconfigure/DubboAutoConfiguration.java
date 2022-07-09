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
package com.alibaba.boot.dubbo.autoconfigure;

import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import com.alibaba.dubbo.config.spring.context.annotation.DubboConfigConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import java.util.Set;

import static com.alibaba.boot.dubbo.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static com.alibaba.boot.dubbo.util.DubboUtils.DUBBO_PREFIX;
import static com.alibaba.boot.dubbo.util.DubboUtils.MULTIPLE_CONFIG_PROPERTY_NAME;
import static java.util.Collections.emptySet;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Dubbo Auto {@link Configuration}
 *
 * @see ApplicationConfig
 * @see Service
 * @see Reference
 * @see DubboComponentScan
 * @see EnableDubboConfig
 * @see EnableDubbo
 * @since 1.0.0
 */
@Configuration // 配置类
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true, havingValue = "true") // 要求配置了 "dubbo.enabled=true" 或者，"dubbo.enabled" 未配置
@ConditionalOnClass(AbstractConfig.class) // AbstractConfig 类存在的时候，即用于判断有 Dubbo 库
public class DubboAutoConfiguration {

    /**
     * Creates {@link ServiceAnnotationBeanPostProcessor} Bean
     *
     * @param environment {@link Environment} Bean
     * @return {@link ServiceAnnotationBeanPostProcessor}
     */
    @ConditionalOnProperty(name = BASE_PACKAGES_PROPERTY_NAME) // 配置了 "dubbo.scan.base-package" 属性，即要扫描 Dubbo 注解的包
    @ConditionalOnClass(ConfigurationPropertySources.class) // 有 Spring Boot 配置加载的功能
    @Bean
    public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor(Environment environment) {
        // <1> 获取 "dubbo.scan.base-package" 属性，即要扫描 Dubbo 注解的包。
        Set<String> packagesToScan = environment.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, emptySet());

        // <2> 创建 ServiceAnnotationBeanPostProcessor 对象，后续，ServiceAnnotationBeanPostProcessor 会扫描 packagesToScan 包的 Dubbo @Service 注解，
        // 创建对应的 Dubbo Service Bean 对象
        return new ServiceAnnotationBeanPostProcessor(packagesToScan);
    }

    @ConditionalOnClass(Binder.class) // 存在 Binder 类的时候
    @Bean
    @Scope(scopeName = SCOPE_PROTOTYPE) // 多例, 为什么？因为有多个 AbstractConfig 对象
    public RelaxedDubboConfigBinder relaxedDubboConfigBinder() {
        // RelaxedDubboConfigBinder ，用于将具体的属性，设置到相应的 AbstractConfig 对象中。
        return new RelaxedDubboConfigBinder();
    }

    /**
     * Creates {@link ReferenceAnnotationBeanPostProcessor} Bean if Absent
     *
     * @return {@link ReferenceAnnotationBeanPostProcessor}
     */
    @ConditionalOnMissingBean  // 不存在 ReferenceAnnotationBeanPostProcessor Bean 的时候
    @Bean(name = ReferenceAnnotationBeanPostProcessor.BEAN_NAME) // Bean 的名字是 referenceAnnotationBeanPostProcessor
    public ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor() {
        // 创建 Bean 名字为 "referenceAnnotationBeanPostProcessor" 的 ReferenceAnnotationBeanPostProcessor Bean 对象
        // 后续，ReferenceAnnotationBeanPostProcessor 会扫描 Dubbo @Reference 注解，创建对应的 Dubbo Service Bean 对象。
        return new ReferenceAnnotationBeanPostProcessor();
    }

    /**
     * SingleDubboConfigConfiguration 对应 @EnableDubboConfig(multiple = false) 。
     * 无任何条件，所以会创建。
     * 引入了单个 Dubbo 配置绑定 Bean 的配置
     *
     * dubbo.application
     * dubbo.module
     * dubbo.registry
     * dubbo.protocol
     * dubbo.monitor
     * dubbo.provider
     * dubbo.consumer
     *
     * Single Dubbo Config Configuration
     *
     * @see EnableDubboConfig
     * @see DubboConfigConfiguration.Single
     */
    @EnableDubboConfig
    protected static class SingleDubboConfigConfiguration {
    }

    /**
     * MultipleDubboConfigConfiguration 对应 @EnableDubboConfig(multiple = true)
     * 要求配置 "dubbo.config.multiple=true" 。默认情况下，Dubbo 自带 "dubbo.config.multiple=true" ，所以也会创建。
     * 引入了多个 Dubbo 配置绑定 Bean 的配置。
     *
     * dubbo.applications
     * dubbo.modules
     * dubbo.registries
     * dubbo.protocols
     * dubbo.monitors
     * dubbo.providers
     * dubbo.consumers
     *
     * Multiple Dubbo Config Configuration , equals @EnableDubboConfig.multiple() == <code>true</code>
     *
     * @see EnableDubboConfig
     * @see DubboConfigConfiguration.Multiple
     */
    @ConditionalOnProperty(name = MULTIPLE_CONFIG_PROPERTY_NAME, havingValue = "true")
    @EnableDubboConfig(multiple = true)
    protected static class MultipleDubboConfigConfiguration {
    }

}
