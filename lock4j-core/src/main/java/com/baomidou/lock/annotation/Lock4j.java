/*
 *  Copyright (c) 2018-2021, baomidou (63976799@qq.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.baomidou.lock.annotation;

import com.baomidou.lock.constant.LockScope;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 *
 * @author zengzhihong TaoYu
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Lock4j {

    /**
     * 锁范围
     *
     * @return {@link LockScope}
     */
    LockScope scope() default LockScope.METHOD;

    /**
     * @return lock 执行器
     */
    Class<? extends LockExecutor> executor() default LockExecutor.class;

    /**
     * @return KEY 默认包名+方法名
     */
    String[] keys() default "";

    /**
     * @return 过期时间 单位：毫秒
     * <pre>
     *     过期时间一定是要长于业务的执行时间. 未设置则为默认时间3秒 default value {@link Lock4jProperties#expire}
     * </pre>
     */
    long expire() default 0;

    /**
     * @return 获取锁超时时间 单位：毫秒
     * <pre>
     *     结合业务,建议该时间不宜设置过长,特别在并发高的情况下. 未设置则为默认时间3秒 default value {@link Lock4jProperties#acquireTimeout}
     * </pre>
     */
    long acquireTimeout() default 0;

    /**
     * support SPEL expresion
     *
     * @return lock failure message
     */
    String message() default "";

}
