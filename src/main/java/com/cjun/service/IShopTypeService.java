package com.cjun.service;

import com.cjun.dto.Result;
import com.cjun.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryListByAscSort();
}
