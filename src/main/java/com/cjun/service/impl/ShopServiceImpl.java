package com.cjun.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cjun.dto.Result;
import com.cjun.entity.Shop;
import com.cjun.mapper.ShopMapper;
import com.cjun.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);
        // 互斥锁 解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 返回
        return Result.ok(shop);
    }



    @Override
    @Transactional
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
        String lockKey = "lock:shop:" + id;
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
}
