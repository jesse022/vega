package com.sanlux.category.impl.cache;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
@Slf4j
@Component
public class VegaCategoryAuthByShopIdCacher {

    private LoadingCache<Long, CategoryAuthe> categoryAuthByShopIdCacher;

    @Autowired
    private CategoryAutheReadService categoryAutheReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.categoryAuthByShopIdCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, CategoryAuthe>() {
                    @Override
                    public CategoryAuthe load(Long shopId) throws Exception {
                        Response<Optional<CategoryAuthe>> resp =
                                categoryAutheReadService.findCategoryAutheByShopId(shopId);
                        if(!resp.isSuccess()) {
                            log.error("fail to find category auth by shopId:{},cause:{}",shopId,resp.getError());
                            throw new ServiceException("find category auth fail,error code:" + resp.getError());
                        }
                        if (!resp.getResult().isPresent()) {
                            log.error("category auth id null. shopId:{}", shopId);
                            return null;
                        }
                        return resp.getResult().get();
                    }
                });
    }


    public CategoryAuthe findByShopId(Long shopId) {
        try {
            return this.categoryAuthByShopIdCacher.getUnchecked(shopId);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }


    public Boolean invalidByShopIds(List<Long> shopIds) {
        try {
            this.categoryAuthByShopIdCacher.invalidateAll(shopIds);
            return Boolean.TRUE;
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return Boolean.FALSE;
        }
    }
}
