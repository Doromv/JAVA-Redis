package com.hmdp.service.impl;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_LIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_LIST_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopType() {
        //1.先从redis中查询商品列表
        String shopTpyeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_LIST_KEY);
        //2.存在，直接返回结果
        if(ObjectUtil.isNotEmpty(shopTpyeJson)){
            List<ShopType> shopTypeList = JSONUtil.toList(shopTpyeJson, ShopType.class);
            return Result.ok(shopTypeList);
        }
        //3.不存在，从数据库中查询商品列表
        List<ShopType> listShop = query().orderByAsc("sort").list();
        //4.不存在，返回错误
        if(ObjectUtil.isEmpty(listShop)){
            return Result.fail("商品列表不存在");
        }
        //5.存在，存入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_LIST_KEY,JSONUtil.toJsonStr(listShop), (long) (CACHE_SHOP_LIST_TTL+(Math.random()*6)), TimeUnit.MINUTES);
        //6.返回结果
        return Result.ok(listShop);
    }
}
