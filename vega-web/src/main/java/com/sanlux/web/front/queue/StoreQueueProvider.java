package com.sanlux.web.front.queue;

import com.google.common.base.Throwables;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * 进销存模块入库单/库位导入队列生产者
 * Created by lujm on 2017/4/9.
 */
@Slf4j
@Component
public class StoreQueueProvider {
    @Autowired
    private JedisTemplate jedisTemplate;
    public static byte[] STORE_IMPORT_QUEUE_KEY = "store-import-queue".getBytes();


    /**
     * 生产者/消费者模式
     */
    public void push(final byte[] id) {
        if (log.isDebugEnabled()) {
            log.debug("push id:{} to store-import-queue", id);
        }

        try {
            jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
                @Override
                public void action(Jedis jedis) {
                    jedis.lpush(STORE_IMPORT_QUEUE_KEY, id);
                }
            });
        } catch (Exception e) {
            log.error("publish job id:{} fail,cause: {}", id, Throwables.getStackTraceAsString(e));
        }
    }

}
