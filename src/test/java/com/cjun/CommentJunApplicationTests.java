package com.cjun;

import com.cjun.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static com.cjun.utils.RedisConstants.CACHE_SHOP_TTL;


@SpringBootTest
class CommentJunApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void testSaveRedisShop() {
        //new Thread(() -> shopService.saveShop2Redis(1L, CACHE_SHOP_TTL)).start();
    }


}
