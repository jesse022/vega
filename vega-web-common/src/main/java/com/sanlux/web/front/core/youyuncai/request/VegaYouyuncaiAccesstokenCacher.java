package com.sanlux.web.front.core.youyuncai.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sanlux.web.front.core.youyuncai.token.YouyuncaiToken;
import com.sanlux.youyuncai.dto.YouyuncaiTokenReturnStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 友云采获取授权缓存
 * Created by lujm on 2018/5/17.
 */
@Component
@Slf4j
public class VegaYouyuncaiAccesstokenCacher {
    private LoadingCache<String, YouyuncaiTokenReturnStatus> youyuncaiAccesstokenCacher;

    // 友云采定义时间为2小时,集乘网设置为100分钟
    @Value("${api.cache.duration.in.minutes: 100}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.youyuncaiAccesstokenCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<String, YouyuncaiTokenReturnStatus>() {
                    @Override
                    public YouyuncaiTokenReturnStatus load(String key) throws Exception {
                        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
                        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.buildYouyuncaiAccesstoken(youyuncaiToken);

                        return youyuncaiRequest.youyuncaiTokenReturnStatus;
                    }
                });
    }

    public YouyuncaiTokenReturnStatus findByKey(String key) {
        try {
            return this.youyuncaiAccesstokenCacher.getUnchecked(key);
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    public Boolean invalidByKey(String key) {
        try {
            this.youyuncaiAccesstokenCacher.invalidate(key);
            return Boolean.TRUE;
        }catch (CacheLoader.InvalidCacheLoadException e) {
            return Boolean.FALSE;
        }
    }

}
