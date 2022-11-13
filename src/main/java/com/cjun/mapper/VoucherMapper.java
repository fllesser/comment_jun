package com.cjun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cjun.entity.Voucher;
import com.cjun.utils.SubjectSqlProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    /*@Select("SELECT " +
            "v.`id`, v.`shop_id`, v.`title`, v.`sub_title`, v.`rules`, v.`pay_value`,v.`actual_value`, v.`type`," +
            " sv.`stock` , sv.begin_time , sv.end_time " +
            "FROM tb_voucher v LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id " +
            "WHERE v.shop_id=#{shopId} AND v.status=1")*/
    @SelectProvider(type = SubjectSqlProvider.class, method = "queryVoucherOfShop")
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);

}
