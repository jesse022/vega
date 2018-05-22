package com.sanlux.item.impl.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.category.service.VegaCategoryReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 商品ID获取一二级后台类目缓存
 * Created by cuiwentao
 * on 16/8/19
 */
@Slf4j
@Component
public class VegaCategoryByItemIdCacher {

    private LoadingCache<Long, Long> vegaCategoryByItemIdCacher;

    @RpcConsumer
    private VegaCategoryReadService vegaCategoryReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.vegaCategoryByItemIdCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, Long>() {
                    @Override
                    public Long load(Long itemId) throws Exception {
                        Response<Long> resp = vegaCategoryReadService.findBackCategoryId(itemId);
                        if(!resp.isSuccess()) {
                            log.error("fail to find category for item cache by itemId:{},cause:{}",itemId,resp.getError());
                            throw new ServiceException("fail to find category for item cache,cause:{}" + resp.getError());
                        }

                        return resp.getResult();
                    }
                });
    }


    public Long findByItemId(Long itemId) {
        try {
            return this.vegaCategoryByItemIdCacher.getUnchecked(itemId);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }
}
