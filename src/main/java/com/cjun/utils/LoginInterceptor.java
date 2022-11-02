package com.cjun.utils;

import com.cjun.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取session
        HttpSession session = request.getSession();

        //2. 获取session中的用户
        User user = (User) session.getAttribute("user");
        //3. 判断用户是否存在
        if (user == null) {
            //4. 不存在, 拦截 返回401
            response.setStatus(401);
        }
        //5. 存在, 保存用户信息到ThreadLocal
        UserHolder2.saveUser(user);
        //6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder2.removeUser();
    }
}
