package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    RedisIdWorker redisIdWorker;

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShop2Redis(1L,10L);
    }

    private ExecutorService es=Executors.newFixedThreadPool(100);

    @Test
    void testIdWork() throws InterruptedException {
        Runnable task=()->{
            for (int i = 0; i < 20; i++) {
                long id = redisIdWorker.nextID("order");
                System.out.println("id="+id);
            }
        };
        for (int i = 0; i < 20; i++) {
            es.submit(task);
        }
    }
}
