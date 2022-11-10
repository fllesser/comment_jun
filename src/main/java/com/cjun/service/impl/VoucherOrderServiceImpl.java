package com.cjun.service.impl;

import com.cjun.dto.Result;
import com.cjun.entity.SeckillVoucher;
import com.cjun.entity.VoucherOrder;
import com.cjun.mapper.VoucherOrderMapper;
import com.cjun.service.ISeckillVoucherService;
import com.cjun.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cjun.utils.RedisIdWorker;
import com.cjun.utils.SimpleRedisLock;
import com.cjun.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.cjun.utils.RedisConstants.LOCK_ORDER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * lua脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 阻塞队列
     */
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource IVoucherOrderService voucherOrderService;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        //1. 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());

        //2. 判断结果为0
        assert result != null;
        int r = result.intValue();
        if (r != 0) {
            //2.1 不为0,代表没有购买资格
            return Result.fail( r == 1 ? "库存不足" : "不能重复下单");
        }
        //2.2 为0, 把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.3 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.4 用户id
        voucherOrder.setUserId(userId);
        //2.5 代金劵id
        voucherOrder.setVoucherId(voucherId);
        //2.6 放入阻塞队列
        orderTasks.add(voucherOrder);

        //3. 返回订单id
        return Result.ok(orderId);
    }

    @Deprecated
    public Result seckillVoucherOld(Long voucherId) {
        //1. 查询优惠券
        SeckillVoucher voucher= seckillVoucherService.getById(voucherId);
        //2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //没开始
            return Result.fail("秒杀尚未开始! ");
        }
        //3. 判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //已经结束
            return Result.fail("秒杀已经结束! ");
        }
        //4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足! ");
        }
        //5. 一人一单
        Long userId = UserHolder.getUser().getId();
        //6. 创建锁对象
        //SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock(LOCK_ORDER_KEY + userId);
        //7. 获取锁
        //boolean isLock = simpleRedisLock.tryLock(1200);
        boolean isLock = lock.tryLock();
        //8. 判断是否成功获取锁
        if (!isLock) {
            // 获取锁失败, 返回错误, 或者尝试
            return Result.fail("非法请求! 不允许重复下单");
        }
        // 获取锁成功
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(userId, voucherId);
        } finally {
            //simpleRedisLock.unlock();
            //simpleRedisLock.unlockWithLua();
            //simpleRedisLock.unlockWithWatch();
            lock.unlock();
        }
//        synchronized 集群jvm锁不住(哈哈
//        synchronized (userId.toString().intern()) {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            //return this.createVoucherOrder(voucherId);
//            return proxy.createVoucherOrder(userId, voucherId);
//        }
    }

    @Transactional
    @Override
    public Result createVoucherOrder(Long userId, Long voucherId) {
        //5.1 查询订单
        long count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        //5.2 判断是否存在
        if (count > 0) {
            //用户已经购买过了
            return Result.fail("你已经购买过一次! ");
        }

        //6. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                /*.eq("stock", voucher.getStock())*/
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("扣减库存失败");
        }


        //7. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //7.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //7.2 用户id
        //Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        //7.3 代金券
        voucherOrder.setVoucherId(voucherId);
        voucherOrderService.save(voucherOrder);
        //8. 返回订单id
        return Result.ok(orderId);
    }
}
