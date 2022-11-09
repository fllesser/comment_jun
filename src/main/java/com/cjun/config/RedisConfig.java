package com.cjun.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
public class RedisConfig {

    /**
     * Redis 开启事务支持
     * @param factory factory
     * @return StringRedisTemplate
     */
    //@Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(factory);
        stringRedisTemplate.setEnableTransactionSupport(true);
        return stringRedisTemplate;
    }

    /**
     * 配置Redisson客户端
     * @return .
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("261806594zyj");
        return Redisson.create(config);
    }

    //@Bean
    public RedissonClient redissonClient2() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6380").setPassword("261806594zyj");
        return Redisson.create(config);
    }

    //@Bean
    public RedissonClient redissonClient3() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6381").setPassword("261806594zyj");
        return Redisson.create(config);
    }
}
