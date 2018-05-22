package com.sanlux.web.front.component.item;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.common.enums.OrderUserType;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 根据主键获取普通用户sku价格
 * 主键格式:skuId@sellerShopId@userId
 * Created by lujm on 2018/4/2.
 */
@Slf4j
@Component
public class VegaSkuPriceCacher {
    private LoadingCache<String, Integer> vegaSkuPriceCacher;

    @Autowired
    private ReceiveShopParser receiveShopParser;

    @Value("${api.cache.duration.in.minutes: 360}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.vegaSkuPriceCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(@Nonnull String key) throws Exception {
                        if (Strings.isNullOrEmpty(key)) {
                            log.error("fail to find sku price cache by key:{},because key is null or format is wrong",key);
                            return null;
                        }
                        List<Long> keyList = Splitters.splitToLong(key, Splitters.AT);
                        if (Arguments.isNullOrEmpty(keyList) || keyList.size() != 3) {
                            log.error("fail to find sku price cache by key:{},because key is null or format is wrong",key);
                            return null;
                        }

                        Response<Integer> skuPriceResp =
                                receiveShopParser.findSkuPrice(keyList.get(0),keyList.get(1), keyList.get(2), OrderUserType.NORMAL_USER);
                        if (!skuPriceResp.isSuccess()){
                            log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                                    keyList.get(0), keyList.get(1), keyList.get(2), skuPriceResp.getError());
                            return null;
                        }

                        return skuPriceResp.getResult();
                    }
                });
    }


    public Integer findByKey(String key) {
        try {
            return this.vegaSkuPriceCacher.getUnchecked(key);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    public Boolean invalidByKey(String key) {
        try {
            this.vegaSkuPriceCacher.invalidate(key);
            return Boolean.TRUE;
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return Boolean.FALSE;
        }
    }
}
