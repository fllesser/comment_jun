package com.cjun.config;

import com.cjun.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/voucher/**",
                        "/upload/**",
                        "/shop-type/**",
                        "/shop/**",
                        "/user/code",
                        "/user/login",
                        "/blog/hot"
                );
    }
}
