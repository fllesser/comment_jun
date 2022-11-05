package com.cjun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
@MapperScan("com.cjun.mapper")
@SpringBootApplication
public class CommentJunApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentJunApplication.class, args);
    }

}
