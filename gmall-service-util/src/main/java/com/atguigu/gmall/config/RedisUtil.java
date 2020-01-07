package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Description 创建redis的工具类
 * @auther CQ
 * @create 2020-01-03 下午 8:05
 */
public class RedisUtil {

    //创建连接池
    private JedisPool jedisPool;

    /**
     * 线程池初始化操作
     * @param host
     * @param port
     * @param timeOut
     */
    public void initJedisPool(String host,int port,int timeOut){
        //2.1.创建配置连接池的参数类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //2.2.设置连接池最大核心数
        jedisPoolConfig.setMaxTotal(200);
        //2.3.设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //2.4.最少剩余数
        jedisPoolConfig.setMinIdle(10);
        //2.5.排队等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        //2.6.设置当用户获取到jedis时，做自检看当前获取到的jedis 是否可以使用！
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }

    /**
     * 从线程池中获取jedis
     * @return
     */
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}
