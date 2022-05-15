package com.doromv.springdataredis;

import com.doromv.springdataredis.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SpringDataRedisApplicationTests {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Test
    void testString() {
        redisTemplate.opsForValue().set("name","Doromv");
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name="+name);
    }
    @Test
    void testSaveUser(){
        redisTemplate.opsForValue().set("user:100",new User("Doromv",21));
        User o = (User) redisTemplate.opsForValue().get("user:100");
        System.out.println(o);
    }
}
