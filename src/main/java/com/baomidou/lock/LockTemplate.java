/*
 *  Copyright (c) 2018-2020, baomidou (63976799@qq.com).
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

package com.baomidou.lock;

import com.baomidou.lock.exception.LockException;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import com.baomidou.lock.util.LockUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 * 锁模板方法
 * </p>
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class LockTemplate implements InitializingBean {

    private final Map<Class<? extends LockExecutor>, LockExecutor> executorMap = new LinkedHashMap<>();
    @Setter
    private Lock4jProperties properties;
    @Setter
    private LockFailureStrategy lockFailureStrategy;
    @Setter
    private List<LockExecutor> executors;

    private LockExecutor primaryExecutor;

    public LockTemplate() {
    }

    public LockInfo lock(String key) {
        return lock(key, 0, 0);
    }

    public LockInfo lock(String key, long expire, long acquireTimeout) {
        return lock(key, expire, acquireTimeout, null);
    }

    /**
     * 加锁方法
     *
     * @param key            锁key 同一个key只能被一个客户端持有
     * @param expire         过期时间(ms) 防止死锁
     * @param acquireTimeout 尝试获取锁超时时间(ms)
     * @param executor       执行器
     * @return 加锁成功返回锁信息 失败返回null
     */
    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        expire = expire == 0 ? properties.getExpire() : expire;
        acquireTimeout = acquireTimeout == 0 ? properties.getAcquireTimeout() : acquireTimeout;
        long retryInterval = properties.getRetryInterval();
        // 防止重试时间大于超时时间
        if (retryInterval >= acquireTimeout) {
            log.warn("retryInterval more than acquireTimeout,please check your configuration");
        }
        LockExecutor lockExecutor = obtainExecutor(executor);
        int acquireCount = 0;
        String value = LockUtil.simpleUUID();
        long start = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - start < acquireTimeout) {
                acquireCount++;
                Object lockInstance = lockExecutor.acquire(key, value, expire, acquireTimeout);
                if (null != lockInstance) {
                    return new LockInfo(key, value, expire, acquireTimeout, acquireCount, lockInstance,
                            lockExecutor);
                }
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            }
        } catch (InterruptedException e) {
            log.error("lock error", e);
            throw new LockException();
        }
        // lock failure
        lockFailureStrategy.onLockFailure(key, acquireTimeout, acquireCount);
        return null;
    }

    public boolean releaseLock(LockInfo lockInfo) {
        if (null == lockInfo) {
            return false;
        }
        return lockInfo.getLockExecutor().releaseLock(lockInfo.getLockKey(), lockInfo.getLockValue(),
                lockInfo.getLockInstance());
    }

    protected LockExecutor obtainExecutor(Class<? extends LockExecutor> clazz) {
        if (null == clazz || clazz == LockExecutor.class) {
            return primaryExecutor;
        }
        final LockExecutor lockExecutor = executorMap.get(clazz);
        Assert.notNull(lockExecutor, String.format("can not get bean type of %s", clazz));
        return lockExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.isTrue(properties.getAcquireTimeout() > 0, "tryTimeout must more than 0");
        Assert.isTrue(properties.getExpire() > 0, "expireTime must more than 0");
        Assert.isTrue(properties.getRetryInterval() >= 0, "retryInterval must more than 0");
        Assert.notEmpty(executors, "executors must have at least one");

        for (LockExecutor executor : executors) {
            executorMap.put(executor.getClass(), executor);
        }

        final Class<? extends LockExecutor> primaryExecutor = properties.getPrimaryExecutor();
        if (null == primaryExecutor) {
            this.primaryExecutor = executors.get(0);
        } else {
            this.primaryExecutor = executorMap.get(primaryExecutor);
            Assert.notNull(this.primaryExecutor, "primaryExecutor must not be null");
        }
    }
}
