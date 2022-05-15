package com.doromv;

import com.doromv.utils.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * @author Doromv
 * @create 2022-05-15-9:09
 */
public class JedisTest {
    private Jedis jedis;
    @BeforeEach
    public void setUp(){
//        jedis=new Jedis("192.168.200.130",6379);
        jedis= JedisConnectionFactory.getJedis();
//        jedis.auth("QAQm..02r");
        jedis.select(0);
    }
    @Test
    public void testString(){
        String result = jedis.set("name", "Doromv");
        System.out.println("result="+result);
        String name = jedis.get("name");
        System.out.println("name="+name);
    }
    @Test
    public void testHash(){
        jedis.hset("user:1","name","jack");
        jedis.hset("user:1","age","21");
        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println(map);
    }
    @AfterEach
    public void tearDown(){
        if (jedis!=null) {
            jedis.close();
        }
    }
}
