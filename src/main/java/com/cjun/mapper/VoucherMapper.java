package com.cjun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cjun.entity.Voucher;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    //@Select("SELECT * FROM tb_voucher WHERE shop_id=#{shopId}")
    @Select("SELECT " +
            "v.`id`, v.`shop_id`, v.`title`, v.`sub_title`, v.`rules`, v.`pay_value`,v.`actual_value`, v.`type`," +
            " sv.`stock` , sv.begin_time , sv.end_time " +
            "FROM tb_voucher v LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id " +
            "WHERE v.shop_id=#{shopId} AND v.status=1")
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
