package com.cjun.utils;

import org.apache.ibatis.jdbc.SQL;

import java.util.Map;

/**
 * SQL Provider
 * 有三点需要注意:
     * 方法入参必须为 Map
     * 方法的权限修饰符 必须是 public
     * 方法返回的必须是拼接好的 sql 字符串
 */

public class SubjectSqlProvider {

    public String queryVoucherOfShop(Map<String, Object> params) {
        return new SQL() {{
            SELECT("v.id, v.shop_id, v.title, v.sub_title, v.rules, v.pay_value, v.actual_value, v.type");
            SELECT("sv.stock, sv.begin_time, sv.end_time");
            FROM("tb_voucher v");
            LEFT_OUTER_JOIN("tb_seckill_voucher sv on v.id = sv.voucher_id");
            if (params.get("shopId") != null) {
                WHERE("v.shop_id = " + params.get("shopId") + AND() + "v.status = 1");
            }
        }}.toString();
    }
}
