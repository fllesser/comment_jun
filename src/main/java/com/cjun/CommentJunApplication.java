package com.cjun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.cjun.mapper")
@SpringBootApplication
public class CommentJunApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentJunApplication.class, args);
    }

}
