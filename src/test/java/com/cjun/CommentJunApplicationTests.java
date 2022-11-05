package com.cjun;

import com.cjun.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootTest
class CommentJunApplicationTests {

    @Resource
    private RedisIdWorker redisIdWorker;


    private final ExecutorService es = Executors.newFixedThreadPool(500);

    //@Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(500);

        Runnable task = () -> {
            long id = redisIdWorker.nextId("order");
            try {
                Thread.sleep((long) (Math.random() * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("id = " + id);
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            es.submit(task);
        }
        latch.await();
        es.shutdown();
        long end = System.currentTimeMillis();
        System.out.println("time " + (end - begin));
    }

}
