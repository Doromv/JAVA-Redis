package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Doromv
 * @create 2022-05-17-8:43
 */
@Component
public class RedisIdWorker {

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final int COUNT_BITS=32;

    private static final long BEGIN_TIMESTAMP = LocalDateTime.of(2022, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);

    public long nextID(String keyPrefix){
        //1.生成时间戳
        long timestamp=LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)-BEGIN_TIMESTAMP;
        //2.生成序列号
        SimpleDateFormat sdf = new SimpleDateFormat("yy:MM:dd");
        long date = System.currentTimeMillis();
        String formatDate = sdf.format(date);
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + formatDate);
        //3.拼接并返回
        return timestamp<<COUNT_BITS | increment;
    }

}
