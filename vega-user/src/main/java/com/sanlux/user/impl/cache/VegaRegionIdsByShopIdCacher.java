package com.sanlux.user.impl.cache;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.sanlux.user.dto.scope.DeliveryScopeCityDto;
import com.sanlux.user.dto.scope.DeliveryScopeDto;
import com.sanlux.user.dto.scope.DeliveryScopeRegionDto;
import com.sanlux.user.service.DeliveryScopeReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.user.address.model.Address;
import io.terminus.parana.user.address.service.AddressReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
@Slf4j
@Component
public class VegaRegionIdsByShopIdCacher {

    private LoadingCache<Long, List<Integer>> regionIdsByShopIdCacher;

    @RpcConsumer
    private DeliveryScopeReadService deliveryScopeReadService;

    @RpcConsumer
    private AddressReadService addressReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;


    @PostConstruct
    public void init() {
        this.regionIdsByShopIdCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES).maximumSize(10000)
                .build(new CacheLoader<Long, List<Integer>>() {
                    @Override
                    public List<Integer> load(Long shopId) throws Exception {
                        Response<Optional<List<DeliveryScopeDto>>> resp = deliveryScopeReadService.findByShopId(shopId);
                        if (!resp.isSuccess()) {
                            log.error("fail to find regionIds by shopId:{},cause:{}", shopId, resp.getError());
                            throw new ServiceException("find regionIds fail, error code:" + resp.getError());
                        }
                        if (resp.getResult().isPresent() && !CollectionUtils.isEmpty(resp.getResult().get())) {
                            return getRegionIds(resp.getResult().get());

                        }
                        log.error("regionIds empty. shopId:{}", shopId);
                        return Collections.<Integer>emptyList();
                    }
                });
    }


    private List<Integer> getRegionIds(List<DeliveryScopeDto> scopeDtos) {

        List<Integer> regionIds = Lists.newArrayList();
        for (DeliveryScopeDto scope : scopeDtos) {
            if (CollectionUtils.isEmpty(scope.getCitiesMap())) {
                for (Address city : citiesOf(scope.getProvinceId())) {
                    regionIds.addAll(fromRegionListgetRegionIdList(regionsOf(city.getId())));
                }
            } else {
                List<DeliveryScopeCityDto> cityList = scope.getCitiesMap();
                for (DeliveryScopeCityDto cityDto : cityList) {
                    if (CollectionUtils.isEmpty(cityDto.getRegionMap())) {
                        regionIds.addAll(fromRegionListgetRegionIdList(regionsOf(cityDto.getCityId())));
                    } else {
                        regionIds.addAll(Lists.transform(cityDto.getRegionMap(), DeliveryScopeRegionDto::getRegionId));
                    }
                }
            }
        }
        return regionIds;
    }

    private List<Address> citiesOf(Integer provinceId) {
        Response<List<Address>> cityResp = addressReadService.citiesOf(provinceId);
        if (!cityResp.isSuccess()) {
            log.error("get city ids fail, provinceId:{}, cause:{}", provinceId, cityResp.getError());
            throw new ServiceException("get cityIds fail,error code" + cityResp.getError());
        }
        return cityResp.getResult();
    }

    private List<Address> regionsOf(Integer cityId) {
        Response<List<Address>> regionResp = addressReadService.regionsOf(cityId);

        if (!regionResp.isSuccess()) {
            log.error("get region ids fail, cityId:{}, cause:{}", cityId, regionResp.getError());
            throw new ServiceException("get regionIds fail,error code" + regionResp.getError());

        }
        return regionResp.getResult();
    }

    private List<Integer> fromRegionListgetRegionIdList(List<Address> regions) {
        return Lists.transform(regions, Address::getId);
    }



    public List<Integer> findByShopId(Long shopId) {
        try {
            return this.regionIdsByShopIdCacher.getUnchecked(shopId);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }


    public Boolean invalidByShopId(Long shopId) {
        try {
            this.regionIdsByShopIdCacher.invalidate(shopId);
            return Boolean.TRUE;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return Boolean.FALSE;
        }
    }
}
