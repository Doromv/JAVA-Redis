package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Doromv
 * @create 2022-05-18-9:49
 */
public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX= cn.hutool.core.lang.UUID.randomUUID().toString(true)+"-";

    @Override
    public boolean tryLock(long timeoutSec) {
         return BooleanUtil.isTrue(stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name, ID_PREFIX+Thread.currentThread().getId(),timeoutSec, TimeUnit.SECONDS));
    }

//    @Override
//    public void unlock() {
//        String threadID=ID_PREFIX+Thread.currentThread().getId();
//        if(stringRedisTemplate.opsForValue().get(KEY_PREFIX+name).equals(threadID)){
//            stringRedisTemplate.delete(KEY_PREFIX+name);
//        }
//    }

    @Override
    public void unlock() {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId()
        );
    }
}
