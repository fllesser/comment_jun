package com.cjun.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.cjun.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 逻辑过期写入缓存
     */
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));
        redisData.setData(value);
        this.set(key, JSONUtil.toJsonStr(redisData), expireTime, unit);
    }

    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R>/*代表有参有返回值的函数*/ dbFallback,
            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1. 从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3. 存在, 直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空对象
        if (json != null) {
            // 空对象
            // 返回一个错误信息
            return null;
        }
        //4. 不存在, 根据id查询数据库
        R r = dbFallback.apply(id);
        //5. 不存在, 返回错误
        if (r == null) {
            // 缓存空对象
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6. 存在, 写入redis, 返回
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 利用逻辑过期解决缓存击穿问题
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback,
            Long expireTime, TimeUnit unit
            ) {
        String rKey = keyPrefix + id;
        //1. 从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(rKey);
        //2. 判断是否存在
        if (StrUtil.isBlank(json)) {
            //3. 未命中, 返回空, 说明不是热点key, 需手动添加
            return null;
        }
        //4. 命中缓存, 判断是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        //这里反序列化回来的data属性由于是Object类, 所以为JSONObject, 而不是R
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            //5. 没有过期, 直接返回
            return r;
        }
        //6. 过期, 尝试获取互斥锁
        String lockKey = LOCK_KEY + r.getClass().toString().toLowerCase() + ":" + id;
        boolean isLock = tryLock(lockKey);
        //7. 获取锁成功, 开启独立线程查询数据库
        if (isLock) {
            log.info("获取锁成功");
            CACHE_REBUILD_EXECUTOR.submit(() -> this.setWithLogicalExpire(rKey, dbFallback, expireTime, unit));
        }
        //8. 返回旧数据
        return r;
    }


    /**
     * 获得互斥锁
     */
    private boolean tryLock(String lockKey) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     */
    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

}