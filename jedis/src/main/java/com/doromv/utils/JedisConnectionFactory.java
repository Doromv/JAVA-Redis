package com.doromv.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Doromv
 * @create 2022-05-15-9:24
 */
public class JedisConnectionFactory {
    private static final JedisPool JEDIS_POOL;

    static {
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(8);
        jedisPoolConfig.setMaxIdle(8);
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxWaitMillis(1000);
        JEDIS_POOL =new JedisPool(jedisPoolConfig,"192.168.200.130",6379,1000,"QAQm..02r");
    }
    public static Jedis getJedis(){
        return JEDIS_POOL.getResource();
    }
}
