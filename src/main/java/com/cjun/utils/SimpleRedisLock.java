package com.cjun.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.cjun.utils.RedisConstants.LOCK_KEY_PREFIX;

public class SimpleRedisLock implements ILock {

    private final String name;
    private final StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示, 用UUID表示，保证多台JVM情况下线程标识不重复
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(LOCK_KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return success != null && success;
        // return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(LOCK_KEY_PREFIX + name);
        // 判断锁中的标识
        if (threadId.equals(id)) {
            // 释放锁
            stringRedisTemplate.delete(LOCK_KEY_PREFIX + name);
        }

    }
}
