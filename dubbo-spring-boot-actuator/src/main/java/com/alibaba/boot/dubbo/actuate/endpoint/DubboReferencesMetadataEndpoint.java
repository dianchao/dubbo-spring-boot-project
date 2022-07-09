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

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;

import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dubbo {@link Reference} Metadata {@link Endpoint}
 * 继承 AbstractDubboEndpoint 抽象类，获取所有的 Dubbo @Reference Bean 的元数据
 *
 * @since 1.0.0
 */
@Endpoint(id = "dubboreferences")
public class DubboReferencesMetadataEndpoint extends AbstractDubboEndpoint {

    @ReadOperation
    public Map<String, Map<String, Object>> references() {
        // 创建 Map
        // KEY：Bean 的名字
        // VALUE：Bean 的元数据
        Map<String, Map<String, Object>> referencesMetadata = new LinkedHashMap<>();

        // 获取 ReferenceAnnotationBeanPostProcessor Bean 对象
        ReferenceAnnotationBeanPostProcessor beanPostProcessor = getReferenceAnnotationBeanPostProcessor();

        // injected Field ReferenceBean Cache
        referencesMetadata.putAll(buildReferencesMetadata(beanPostProcessor.getInjectedFieldReferenceBeanMap()));

        // injected Method ReferenceBean Cache
        referencesMetadata.putAll(buildReferencesMetadata(beanPostProcessor.getInjectedMethodReferenceBeanMap()));

        return referencesMetadata;

    }

    private Map<String, Map<String, Object>> buildReferencesMetadata(
            Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedElementReferenceBeanMap) {
        // 创建 Map
        // KEY：Bean 的名字
        // VALUE：Bean 的元数据
        Map<String, Map<String, Object>> referencesMetadata = new LinkedHashMap<>();

        // 遍历 injectedElementReferenceBeanMap 元素
        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry :
                injectedElementReferenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();
            // 获取 ReferenceBean 对象
            ReferenceBean<?> referenceBean = entry.getValue();
            // 获取 Bean 元数据
            Map<String, Object> beanMetadata = resolveBeanMetadata(referenceBean);
            // 获取 invoker 属性
            beanMetadata.put("invoker", resolveBeanMetadata(referenceBean.get()));

            // 添加到 referencesMetadata 中
            referencesMetadata.put(String.valueOf(injectedElement.getMember()), beanMetadata);

        }

        return referencesMetadata;
    }

}
