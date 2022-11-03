package com.cjun.service;

import com.cjun.dto.Result;
import com.cjun.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author chowyijiu
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);
}
