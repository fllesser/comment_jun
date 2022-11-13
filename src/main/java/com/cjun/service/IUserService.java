package com.cjun.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cjun.dto.LoginFormDTO;
import com.cjun.dto.Result;
import com.cjun.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
