package com.cjun.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cjun.dto.Result;
import com.cjun.entity.ShopType;
import com.cjun.mapper.ShopTypeMapper;
import com.cjun.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.cjun.utils.RedisConstants.TYPE_LIST;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author chowyijiu
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryListByAscSort() {
        //1. 先从redis中查
        String typeListStr = stringRedisTemplate.opsForValue().get(TYPE_LIST);
        //2. 存在, 返回
        if (StrUtil.isNotBlank(typeListStr)) {
            return Result.ok(JSONUtil.toList(typeListStr, ShopType.class));
        }
        //3. 不存在, 查数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //4. 不存在, 报错
        if (typeList == null) {
            return Result.fail("typeList 查询失败");
        }
        //5. 存在, 写入redis
        stringRedisTemplate.opsForValue().set(TYPE_LIST, JSONUtil.toJsonStr(typeList));
        //6. 返回
        return Result.ok(typeList);
        //return Result.fail("typeList 错误");
    }
}
