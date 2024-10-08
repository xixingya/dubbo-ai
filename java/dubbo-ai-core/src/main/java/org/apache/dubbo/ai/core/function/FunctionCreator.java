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
package org.apache.dubbo.ai.core.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FunctionCreator {

    public static List<FunctionInfo<?,?>> getAiFunctions(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getMethods();
        var wrappers = new ArrayList<FunctionInfo<?,?>>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AiFunction.class)) {
                AiFunction aiFunction = method.getAnnotation(AiFunction.class);
                String desc = aiFunction.value();
                String methodName = method.getName();
                var wrapper = createWrapper(obj, method, methodName, desc);
                wrappers.add(wrapper);
            }
        }
        return wrappers;
    }

    private static FunctionInfo<?,?> createWrapper(Object obj, Method method, String name, String desc) {
        return new FunctionInfo<>(name, desc, method.getParameterTypes()[0], createFunction(obj, method));
    }


    public static <T, R> Function<T, R> createFunction(Object obj, Method method) {
        return (T t) -> {
            try {
                return (R) method.invoke(obj, t);
            } catch (Exception e) {
                throw new RuntimeException("can not invoke method " + method.getName(), e);
            }
        };
    }
}
