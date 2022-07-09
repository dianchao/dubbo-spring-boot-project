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
package com.alibaba.boot.dubbo.actuate.endpoint;

import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.alibaba.dubbo.registry.support.AbstractRegistryFactory.getRegistries;

/**
 * Dubbo Shutdown
 * 继承 AbstractDubboEndpoint 抽象类，关闭 Dubbo
 *
 * @since 0.2.0
 */
@Endpoint(id = "dubboshutdown")
public class DubboShutdownEndpoint extends AbstractDubboEndpoint {


    @WriteOperation
    public Map<String, Object> shutdown() throws Exception {
        // 创建 Map
        Map<String, Object> shutdownCountData = new LinkedHashMap<>();

        // 获取注册中心的数量
        int registriesCount = getRegistries().size();

        // 销毁 ProtocolConfig
        int protocolsCount = getProtocolConfigsBeanMap().size();

        ProtocolConfig.destroyAll();
        // 添加到 shutdownCountData 中
        shutdownCountData.put("registries", registriesCount);
        shutdownCountData.put("protocols", protocolsCount);

        // 获取所有 ServiceBean ，然后逐个销毁
        Map<String, ServiceBean> serviceBeansMap = getServiceBeansMap();
        if (!serviceBeansMap.isEmpty()) {
            for (ServiceBean serviceBean : serviceBeansMap.values()) {
                serviceBean.destroy();
            }
        }
        // 添加到 shutdownCountData 中
        shutdownCountData.put("services", serviceBeansMap.size());

        // 获取 ReferenceAnnotationBeanPostProcessor 对象
        ReferenceAnnotationBeanPostProcessor beanPostProcessor = getReferenceAnnotationBeanPostProcessor();

        // 获取 Reference Bean 的数量
        int referencesCount = beanPostProcessor.getReferenceBeans().size();
        // 销毁所有 Reference Bean
        beanPostProcessor.destroy();
        // 添加到 shutdownCountData 中
        shutdownCountData.put("references", referencesCount);

        // Set Result to complete
        Map<String, Object> shutdownData = new TreeMap<>();
        shutdownData.put("shutdown.count", shutdownCountData);


        return shutdownData;
    }

}
