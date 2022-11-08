package com.cjun;

import com.cjun.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootTest
class CommentJunApplicationTests {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    private final ExecutorService es = Executors.newFixedThreadPool(500);

    //@Test
    @Deprecated
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

    //@Test
    @Deprecated
    void testRedisson() throws InterruptedException {
        //获取锁对象(可重入)
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁 获取锁的最大等待时间(期间会重试), 锁自动释放时间 时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (isLock) {
            try {
                System.out.println("执行业务");
            } finally {
                lock.unlock();
            }
        }
    }


    private RLock lock;

    @BeforeEach
    void setUp() {
        lock = redissonClient.getLock("order");
    }

    @Test
    void method1() {
        //尝试获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取锁失败...1");
            return;
        }
        try {
            log.info("获取锁成功...1");
            method2();
            log.info("开始执行业务...1");
        } finally {
            log.warn("准备释放锁...1");
            lock.unlock();
        }
    }

    void method2() {
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取锁失败...2");
            return;
        }
        try {
            log.info("获取锁成功...2");
            log.info("开始执行业务...2");
        } finally {
            log.warn("准备释放锁...2");
            lock.unlock();
        }
    }

    @Test
    void testString() {
        String s1 = "java";
        System.out.println(s1.hashCode());
        s1 = "jvav";
        System.out.println(s1.hashCode());
        String s2 = new String(s1);
        System.out.println(s2.hashCode());
        s2 = "java";
        System.out.println(s2.hashCode());
    }


}
