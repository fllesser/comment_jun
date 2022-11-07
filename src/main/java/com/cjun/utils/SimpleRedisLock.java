package com.cjun.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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

    private final static DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void unlockWithLua() {
        //if (redis.call('GET', KEYS[1]) == ARGV[1]) then
        //  return redis.call('DEL', key)
        //end
        //return 0
        // 获取线程标识
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }

    /**
     * 不太行, 因为get不能放到事务中, get要提交事务才能得到结果,
     */
    @Deprecated
    @Override
    public void unlockWithWatch() {
        String key = LOCK_KEY_PREFIX + name;
        stringRedisTemplate.watch(key);
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(key);
        if (threadId.equals(id)) {
            stringRedisTemplate.multi();
            stringRedisTemplate.delete(key);
            stringRedisTemplate.exec();
        }
        stringRedisTemplate.unwatch();
    }

}
