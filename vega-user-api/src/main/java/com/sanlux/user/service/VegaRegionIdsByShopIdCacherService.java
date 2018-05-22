package com.sanlux.user.service;

import com.google.common.base.Optional;
import com.sanlux.user.dto.scope.DeliveryScopeDto;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
public interface VegaRegionIdsByShopIdCacherService {

    /**
     * 根据店铺ID获取供货区域List
     * @param shopId 店铺ID
     * @return 供货区域List
     */
    Response<Optional<List<Integer>>> findByShopId(Long shopId);


    /**
     * 根据收货地址区ID匹配到店铺IDList
     * @param regionId 区ID
     * @return List<ShopId>
     */
    Response<Optional<List<Long>>> findShopIdsByRegionId(Integer regionId);


    /**
     *  根据店铺ID失效缓存中的配送范围
     * @param shopId 店铺ID
     * @return Boolean
     */
    Response<Boolean> invalidByShopId(Long shopId);
}
