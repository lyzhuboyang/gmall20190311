package com.zby.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration//可以把本类变成xml
@Component
public class RedisConfig {


    @Value("${spring.redis.host:disabled}")
    //:disabled 表示如果配置文件中没有获取到host,则表示默认值disabled
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:10000}")
    private int timeOut;

    @Bean
    public RedisUtil getRedisUtil(){
        if("disabled".equals(host)){
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,timeOut,database);
        return redisUtil;
    }

}
