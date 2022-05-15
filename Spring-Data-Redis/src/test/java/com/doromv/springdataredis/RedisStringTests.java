package com.doromv.springdataredis;

import com.doromv.springdataredis.pojo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

@SpringBootTest
class RedisStringTests {
    @Autowired
   private StringRedisTemplate redisTemplate;

    @Test
    void testString(){
        redisTemplate.opsForValue().set("name","Doromv");
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name="+name);
    }

    private static final ObjectMapper mapper=new ObjectMapper();

    @Test
    void testSaveUser() throws JsonProcessingException {
        //创建对象
        User user = new User("zr", 22);
        //手动序列化
        String json = mapper.writeValueAsString(user);
        //写入数据
        redisTemplate.opsForValue().set("user:101",json);
        //获取数据
        String s = redisTemplate.opsForValue().get("user:101");
        //手动反序列化
        User o = mapper.readValue(s, User.class);
        //输出到控制台
        System.out.println(o);
    }
    @Test
    void testHash(){
        redisTemplate.opsForHash().put("user:103","name","qbhn");
        redisTemplate.opsForHash().put("user:103","age","23");
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("user:103");
        System.out.println(entries);
    }
}
