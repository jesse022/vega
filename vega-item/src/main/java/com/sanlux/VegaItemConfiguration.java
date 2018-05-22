package com.sanlux;

import com.sanlux.item.impl.manager.VegaStorageManager;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.ItemAutoConfig;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.storage.impl.service.DefaultStorageServiceImpl;
import io.terminus.parana.storage.service.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * @author Effet
 */
@Configuration
@ComponentScan({
        "com.sanlux.category",
        "com.sanlux.item",
        "com.sanlux.search",
        "com.sanlux.shop",
})
@Import({ItemAutoConfig.class})
public class VegaItemConfiguration {


    @Configuration
    public static class ItemImplConfiguration {
        @Bean
        public StorageService storageService(ItemDao itemDao, SkuDao skuDao) {
            VegaStorageManager storageManager = new VegaStorageManager(itemDao, skuDao);
            return new DefaultStorageServiceImpl(storageManager);
        }

        @Bean
        public JedisTemplate jedisTemplate(Pool<Jedis> pool) {
            return new JedisTemplate(pool);
        }
    }

}
