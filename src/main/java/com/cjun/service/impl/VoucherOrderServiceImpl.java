package com.cjun.service.impl;

import com.cjun.dto.Result;
import com.cjun.entity.SeckillVoucher;
import com.cjun.entity.VoucherOrder;
import com.cjun.mapper.VoucherOrderMapper;
import com.cjun.service.ISeckillVoucherService;
import com.cjun.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cjun.utils.RedisIdWorker;
import com.cjun.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
    private RedisIdWorker redisIdWorker;

    @Resource IVoucherOrderService voucherOrderService;

    @Override
    public Result seckillVoucher(Long voucherId) {
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
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //return this.createVoucherOrder(voucherId);
            return proxy.createVoucherOrder(userId, voucherId);
        }
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
            return Result.fail("用户已经购买过一次! ");
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
