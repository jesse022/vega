package com.sanlux.web.front.queue;

import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;



/**
 * 进销存模块入库单/库位导入队列消费者
 * Created by lujm on 2017/4/9.
 */
@Slf4j
@Repository
public class StoreQueueConsumer {
    @Autowired
    private JedisTemplate jedisTemplate;
    public static byte[] STORE_IMPORT_QUEUE_KEY = "store-import-queue".getBytes();

    public byte[] pop() {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<byte[]>() {
            @Override
            public byte[] action(Jedis jedis) {
                return jedis.rpop(STORE_IMPORT_QUEUE_KEY);
            }
        });
    }
}
