/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaShopCacher {

    private LoadingCache<Long, VegaShopExtra> vegaShopExtraCache;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    // TODO: 8/24/16 约定还是配置
    private final Long duration = 60L;

    @PostConstruct
    public void init() {
        this.vegaShopExtraCache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, VegaShopExtra>() {
                    @Override
                    public VegaShopExtra load(Long shopId) throws Exception {
                        Response<VegaShopExtra> resp = vegaShopReadService.findVegaShopExtraByShopId(shopId);
                        return resp.getResult();
                    }
                });
    }

    public VegaShopExtra findVegaShopExtraByShopId(Long shopId) {
        return this.vegaShopExtraCache.getUnchecked(shopId);
    }

}
