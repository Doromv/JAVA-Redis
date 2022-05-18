package com.hmdp.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Transactional
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (seckillVoucher.getBeginTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        if(seckillVoucher.getStock()<1){
            return Result.fail("库存不足");
        }
        return createVoucherOrder(voucherId);
    }
    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        //5.一人一单
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        //获取锁
        boolean islLock = simpleRedisLock.tryLock(1200);
        //判断
        if(!islLock){
            return Result.fail("不允许重复下单");
        }
        try {
            //5.1查询订单
            //5.2判断是否存在
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("用户已经购买过此优惠券");
            }
            //5.3扣减库存
            boolean success = seckillVoucherService
                    .update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //6.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            long id = redisIdWorker.nextID("order");
            voucherOrder.setId(id);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(UserHolder.getUser().getId());
            //7.保存订单到数据包并且返回订单ID
            save(voucherOrder);
            return Result.ok(id);
        } finally {
            simpleRedisLock.unlock();
        }
    }
//    @Transactional
//    public  Result createVoucherOrder(Long voucherId) {
//        //5.一人一单
//        //5.1查询订单
//        Long userId = UserHolder.getUser().getId();
//        //5.2判断是否存在
//        synchronized (userId.toString().intern()) {
//            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//            if (count > 0) {
//                return Result.fail("用户已经购买过此优惠券");
//            }
//            //5.3扣减库存
//            boolean success = seckillVoucherService
//                    .update()
//                    .setSql("stock=stock-1")
//                    .eq("voucher_id", voucherId)
//                    .gt("stock", 0)
//                    .update();
//            if (!success) {
//                return Result.fail("库存不足");
//            }
//            //6.创建订单
//            VoucherOrder voucherOrder = new VoucherOrder();
//            long id = redisIdWorker.nextID("order");
//            voucherOrder.setId(id);
//            voucherOrder.setVoucherId(voucherId);
//            voucherOrder.setUserId(UserHolder.getUser().getId());
//            //7.保存订单到数据包并且返回订单ID
//            save(voucherOrder);
//            return Result.ok(id);
//        }
//    }
}
