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

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dubbo {@link Service} Metadata {@link Endpoint}
 * 继承 AbstractDubboEndpoint 抽象类，获取所有的 Dubbo @Service Bean 的元数据
 *
 * @since 0.2.0
 */
@Endpoint(id = "dubboservices")
public class DubboServicesMetadataEndpoint extends AbstractDubboEndpoint {

    @ReadOperation
    public Map<String, Map<String, Object>> services() {
        // 获取所有的 ServiceBean
        Map<String, ServiceBean> serviceBeansMap = getServiceBeansMap();

        // 创建 Map
        // KEY：Bean 的名字
        // VALUE：Bean 的元数据
        Map<String, Map<String, Object>> servicesMetadata = new LinkedHashMap<>(serviceBeansMap.size());

        // 遍历 serviceBeansMap 元素
        for (Map.Entry<String, ServiceBean> entry : serviceBeansMap.entrySet()) {
            // 获取 Bean 的名字
            String serviceBeanName = entry.getKey();

            // 获取 ServiceBean 对象
            ServiceBean serviceBean = entry.getValue();

            // 获取 Bean 的元数据
            Map<String, Object> serviceBeanMetadata = resolveBeanMetadata(serviceBean);

            // 获取 Service 对象。若获取到，则添加到 serviceBeanMetadata 中
            Object service = resolveServiceBean(serviceBeanName, serviceBean);

            if (service != null) {
                // Add Service implementation class
                serviceBeanMetadata.put("serviceClass", service.getClass().getName());
            }
            // 添加到 servicesMetadata 中
            servicesMetadata.put(serviceBeanName, serviceBeanMetadata);

        }

        return servicesMetadata;

    }

    private Object resolveServiceBean(String serviceBeanName, ServiceBean serviceBean) {

        int index = serviceBeanName.indexOf("#");

        if (index > -1) {

            Class<?> interfaceClass = serviceBean.getInterfaceClass();

            String serviceName = serviceBeanName.substring(index + 1);

            if (applicationContext.containsBean(serviceName)) {
                return applicationContext.getBean(serviceName, interfaceClass);
            }

        }

        return null;

    }

}
