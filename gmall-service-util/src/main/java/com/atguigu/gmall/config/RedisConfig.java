package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @Description 将redis的工具类放入spring容器中
 * @auther CQ
 * @create 2020-01-03 下午 8:00
 */
@SpringBootConfiguration
public class RedisConfig {

    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private Integer port;

    @Value("${spring.redis.timeOut:10000}")
    private Integer timeOut;

    @Bean
    public RedisUtil getRedisUtil(){
        //判断地址是否为空
        if("disabled".equals(host)){
            return null;
        }
        //创建一个redis工具类并初始化
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,timeOut);
        return redisUtil;
    }

}
