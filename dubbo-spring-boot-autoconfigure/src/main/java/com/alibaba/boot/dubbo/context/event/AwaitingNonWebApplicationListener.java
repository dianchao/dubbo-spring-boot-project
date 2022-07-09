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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Awaiting Non-Web Spring Boot {@link ApplicationListener}
 *
 * 实现 SmartApplicationListener 接口，实现在非 Web 的环境下，提供 JVM 不退出关闭的功能，即 JVM 一直运行着
 *
 * @since 0.1.1
 */
public class AwaitingNonWebApplicationListener implements SmartApplicationListener {

    private static final Logger logger = LoggerFactory.getLogger(AwaitingNonWebApplicationListener.class);

    private static final Class<? extends ApplicationEvent>[] SUPPORTED_APPLICATION_EVENTS =
            of(ApplicationReadyEvent.class, ContextClosedEvent.class);

    /**
     * 是否已经等待完成
     */
    private static final AtomicBoolean awaited = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 判断支持的事件类型是 ApplicationReadyEvent 和 ContextClosedEvent
     *
     * @param eventType
     * @return
     */
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ObjectUtils.containsElement(SUPPORTED_APPLICATION_EVENTS, eventType);
    }

    /**
     * 判断支持的事件来源
     * 全部返回 true ，意味支持所有的事件来源.
     *
     * @param sourceType
     * @return
     */
    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    private static <T> T[] of(T... values) {
        return values;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            onApplicationReadyEvent((ApplicationReadyEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    protected void onApplicationReadyEvent(ApplicationReadyEvent event) {

        final SpringApplication springApplication = event.getSpringApplication();

        // <1> 如果是 Web 环境，则直接返回
        if (!WebApplicationType.NONE.equals(springApplication.getWebApplicationType())) {
            return;
        }

        // <2> 启动一个用户线程，从而实现等待
        await();

    }

    /**
     * 处理 ContextClosedEvent 事件
     *
     * @param event
     */
    protected void onContextClosedEvent(ContextClosedEvent event) {
        // <1> 释放
        release();
        // <2> 关闭线程池
        shutdown();
    }

    protected void await() {

        // 如果已经处于阻塞等待，直接返回
        if (awaited.get()) {
            return;
        }

        // 创建任务，实现阻塞
        executorService.execute(() -> executeMutually(() -> {
            while (!awaited.get()) {
                if (logger.isInfoEnabled()) {
                    logger.info(" [Dubbo] Current Spring Boot Application is await...");
                }
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }));
    }

    protected void release() {
        executeMutually(() -> {
            // CAS 设置 awaited 为 true
            while (awaited.compareAndSet(false, true)) {
                if (logger.isInfoEnabled()) {
                    logger.info(" [Dubbo] Current Spring Boot Application is about to shutdown...");
                }
                // 通知 Condition
                condition.signalAll();
            }
        });
    }

    /**
     * 关闭线程池
     */
    private void shutdown() {
        if (!executorService.isShutdown()) {
            // Shutdown executorService
            executorService.shutdown();
        }
    }

    private void executeMutually(Runnable runnable) {
        try {
            lock.lock();
            // <X> 执行 Runnable
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    static AtomicBoolean getAwaited() {
        return awaited;
    }
}
