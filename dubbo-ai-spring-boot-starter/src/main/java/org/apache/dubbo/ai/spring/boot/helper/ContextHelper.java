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
package org.apache.dubbo.ai.spring.boot.helper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.PriorityOrdered;

public class ContextHelper implements ApplicationContextAware, PriorityOrdered {

    private static GenericApplicationContext genericApplicationContext;
    

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        genericApplicationContext = (GenericApplicationContext) applicationContext;
    }

    public static BeanFactory getBeanFactory() {
        return genericApplicationContext.getBeanFactory();
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
