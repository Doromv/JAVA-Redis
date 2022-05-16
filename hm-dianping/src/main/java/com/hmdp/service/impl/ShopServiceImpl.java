package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Doromv
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private CacheClient cacheClient;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        if(shop==null){
//            return  Result.fail("店铺不存在");
//        }
//        return Result.ok(shop);
        //互斥锁解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop==null){
            return  Result.fail("店铺不存在");
        }
        return Result.ok(shop);
        //逻辑过期解决缓存击穿问题
//        Shop shop = queryWithLogicalExpire(id);
//        if(shop==null){
//            return  Result.fail("店铺不存在");
//        }
//        return Result.ok(shop);

    }

    public Shop queryWithLogicalExpire(Long id){
        //1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(ObjectUtil.isEmpty(shopJson)){
            //3.不存在，直接返回
            return null;
        }
        //4.存在，判断是否过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            //4.1未过期，直接返回店铺信息
            return shop;
        }
        //4.2过期，需要缓存重建
        //4.2.1获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //4.2.2获取成功，开启独立线程，实现缓存重建
        if(isLock){
                CACHE_REBUILD_EXECUTOR.submit(()->{
                    try {
                        saveShop2Redis(id,20L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        unlock(lockKey);
                    }
                });
        }
        //4.3.3获取失败，返回过期的店铺信息
        return shop;
    }

    public Shop queryWithMutex(Long id) {
        //1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(ObjectUtil.isNotEmpty(shopJson)){
            //3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        //4.实现缓存重建
        //4.1获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否成功
            if(!isLock){
                //4.3失败，休眠并且重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //模拟重建的延时
            Thread.sleep(200);
            //4.4成功，再次判断redis缓存是否存在
            if(ObjectUtil.isNotEmpty(shopJson)){
                //4.4.1存在，直接返回
                return JSONUtil.toBean(shopJson, Shop.class);
            }
            //4.4.1为空
            if(shopJson!=null){
                return null;
            }
            //4.4.2不存在，查询数据库
            shop = getById(id);
            if(ObjectUtil.isEmpty(shop)){
                //5.数据库也不存在，将空值写入redis
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"", (long) (CACHE_NULL_TTL+(Math.random()*6)),TimeUnit.MINUTES);
                return null;
            }
            //6.存在，写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), (long) (CACHE_SHOP_TTL+(Math.random()*6)),TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw  new RuntimeException(e);
        } finally {
            //7.释放互斥锁
            unlock(lockKey);
        }
        //8.返回结果
        return shop;
    }

    public Shop queryWithPassThrough(Long id){
        //1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(ObjectUtil.isNotEmpty(shopJson)){
            //3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        //4.不存在，根据id查询数据库
        Shop shop = getById(id);
        if(ObjectUtil.isEmpty(shop)){
            //5.数据库也不存在，将空值写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"", (long) (CACHE_NULL_TTL+(Math.random()*6)),TimeUnit.MINUTES);
            return null;
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), (long) (CACHE_SHOP_TTL+(Math.random()*6)),TimeUnit.MINUTES);

        //7.返回结果
        return shop;
    }

    private boolean tryLock(String key){
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(result);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException {
        //1.查询店铺数据
        Shop shop = getById(id);
        //模拟延迟
        Thread.sleep(200);
        //2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if(ObjectUtil.isEmpty(shop.getId())){
            return Result.fail("商店ID不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
        return Result.ok();
    }
}
