package com.sanlux.item.impl.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.category.service.VegaFrontCategoryReaderService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 前台类目ID获取前台类目缓存
 * Created by syf
 * on 17/6/29
 */
@Slf4j
@Component
public class VegaFrontCategoriesCacher {

    private LoadingCache<Long, List<FrontCategory>> vegaFrontCategoriesCacher;

    @RpcConsumer
    private VegaFrontCategoryReaderService vegaFrontCategoryReaderService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.vegaFrontCategoriesCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, List<FrontCategory>>() {
                    @Override
                    public List<FrontCategory> load(Long id) throws Exception {
                        Response<List<FrontCategory>> resp = vegaFrontCategoryReaderService.findAncestorsOf(id);
                        if(!resp.isSuccess()) {
                            log.error("fail to find category for item cache by id:{},cause:{}",id,resp.getError());
                            throw new ServiceException("fail to find category for item cache,cause:{}" + resp.getError());
                        }
                        return resp.getResult();
                    }
                });
    }


    public List<FrontCategory> findByCategoryId(Long categoryId) {
        try {
            return this.vegaFrontCategoriesCacher.getUnchecked(categoryId);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }
}
