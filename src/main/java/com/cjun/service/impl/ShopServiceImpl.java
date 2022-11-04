package com.cjun.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cjun.dto.Result;
import com.cjun.entity.Shop;
import com.cjun.mapper.ShopMapper;
import com.cjun.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cjun.utils.CacheClient;
import com.cjun.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.cjun.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存空对象 解决缓存穿透
        // Shop shop = queryWithPassThrough(id);
        // Shop shop = cacheClient.queryWithPassThrough(
        //         CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 互斥锁 解决缓存击穿
        //Shop shop = queryWithMutex(id);
        // 逻辑过期方式解决缓存击穿
        //Shop shop = queryWithLogicalExpiration(id);
        Shop shop = cacheClient.queryWithLogicalExpire(
                CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 返回
        return Result.ok(shop);
    }

    @Override
    @Transactional // 开启事务
    public Result update(Shop shop) {
        //1. 更新数据库
        updateById(shop);
        //2. 删除缓存
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    /**
     * 利用互斥锁解决缓存击穿问题
     */
    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1. 从Redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2. 判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3. 存在, 直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中的是否是空对象
        if (shopJson != null) {
            // 如果是空对象
            // 返回一个错误信息
            return null;
        }
        //4. 实现缓存重建
        //4.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2 判断是否成功
            if (!isLock) {
                //4.3 失败, 则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功, 根据id查询数据库
            shop = getById(id);
            // 模拟重建的延时
            Thread.sleep(200);
            //5. 不存在, 返回错误
            if (shop == null) {
                // set一个缓存空对象
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //6. 存在, 写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
        //7. 释放互斥锁
        return shop;
    }

    /**
     * 获得互斥锁
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
//    /**
//     * 以逻辑过期方式添加缓存
//     */
//    public void saveShop2Redis(Long id, Long expireSeconds) {
//        //1. 查数据库
//        Shop shop = getById(id);
//        //2. 封装成逻辑过期
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //3. 写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//        //4. 释放锁
//        //unlock(LOCK_SHOP_KEY + id);
//        log.info("释放锁");
//    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

//    /**
//     * 利用逻辑过期解决缓存击穿问题
//     */
//    public Shop queryWithLogicalExpiration(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //1. 从Redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2. 判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //3. 未命中, 返回空, 说明不是热点key, 需手动添加
//            return null;
//        }
//        //4. 命中缓存, 判断是否过期
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        //这里反序列化回来的data属性由于是Object类, 所以为JSONObject, 而不是shop
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
//            //5. 没有过期, 直接返回
//            return shop;
//        }
//        //6. 过期, 尝试获取互斥锁
//        boolean isLock = tryLock(LOCK_SHOP_KEY + id);
//        //7. 获取锁成功, 开启独立线程查询数据库
//        if (isLock) {
//            log.info("获取锁成功");
//            CACHE_REBUILD_EXECUTOR.submit(() -> saveShop2Redis(id, CACHE_SHOP_TTL));
//        }
//        //8. 返回旧数据
//        return shop;
//    }


//    @Deprecated
//    public Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //1. 从Redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2. 判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3. 存在, 直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return null;
//        }
//        // 判断命中的是否是空对象
//        if (shopJson != null) {
//            // 空对象
//            // 返回一个错误信息
//            return null;
//        }
//
//        //4. 不存在, 根据id查询数据库
//        Shop shop = getById(id);
//        //5. 不存在, 返回错误
//        if (shop == null) {
//            // 缓存空对象
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6. 存在, 写入redis, 返回
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
//        return shop;
//    }
}
