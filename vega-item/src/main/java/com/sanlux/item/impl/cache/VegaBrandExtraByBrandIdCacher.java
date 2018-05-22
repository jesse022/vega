package com.sanlux.item.impl.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.item.model.VegaBrandExtra;
import com.sanlux.item.service.VegaBrandExtraReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 根据品牌Id获取品牌扩展信息缓存
 * Created by lujm on 2018/1/22.
 */
@Slf4j
@Component
public class VegaBrandExtraByBrandIdCacher {
    private LoadingCache<Long, VegaBrandExtra> vegaBrandExtraByBrandIdCacher;

    @RpcConsumer
    private VegaBrandExtraReadService vegaBrandExtraReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.vegaBrandExtraByBrandIdCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, VegaBrandExtra>() {
                    @Override
                    public VegaBrandExtra load(Long branId) throws Exception {
                        Response<VegaBrandExtra> resp = vegaBrandExtraReadService.findByBrandId(branId);
                        if(!resp.isSuccess()) {
                            log.error("fail to find brand extra for brand cache by branId:{},cause:{}",branId,resp.getError());
                            throw new ServiceException("fail to find brand extra for brand cache,cause:{}" + resp.getError());
                        }

                        return resp.getResult();
                    }
                });
    }


    public VegaBrandExtra findByBrandId(Long branId) {
        try {
            return this.vegaBrandExtraByBrandIdCacher.getUnchecked(branId);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    public Boolean invalidByBranId(Long branId) {
        try {
            this.vegaBrandExtraByBrandIdCacher.invalidate(branId);
            return Boolean.TRUE;
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return Boolean.FALSE;
        }
    }
}
