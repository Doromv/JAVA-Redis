package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * @author Doromv
 * @create 2022-05-16-17:02
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setByLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key=keyPrefix+id;
        //1.从redis中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(ObjectUtil.isNotEmpty(json)){
            //3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        if(json!=null){
            return null;
        }
        //4.不存在，根据id查询数据库
        R r=dbFallback.apply(id);
        if(r==null){
            //5.数据库也不存在，将空值写入redis
            this.set(key, "",time, unit);
            return null;
        }
        //6.存在，写入redis
        this.set(key, r,time,unit);
        //7.返回结果
        return r;
    }

    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R> type,Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key=keyPrefix+id;
        //1.从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(ObjectUtil.isEmpty(json)){
            //3.不存在，直接返回
            return null;
        }
        //4.存在，判断是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            //4.1未过期，直接返回店铺信息
            return r;
        }
        //4.2过期，需要缓存重建
        //4.2.1获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //4.2.2获取成功，开启独立线程，实现缓存重建
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    R r1 = dbFallback.apply(id);
                    this.setByLogicalExpire(key,r1,time, unit);
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    unlock(lockKey);
                }
            });
        }
        //4.3.3获取失败，返回过期的店铺信息
        return r;
    }

    private boolean tryLock(String key){
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(result);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
