package com.sanlux.store.service;

import io.terminus.common.model.Response;
import io.terminus.parana.store.model.Location;

import java.util.List;

/**
 * Created by lujm on 2017/3/6.
 */
public interface VegaLocationReadService {

    Response<List<Location>> findByRepertoryAndSku(Long repertoryId,Long skuId);

    Response<List<Location>> findByCategoryAndSku(List<Long> categoryIds,List<Long> skuIds,Long repertoryId);

    /**
     * 根据仓库ID获取未绑定商品的库位
     *
     * @param repertoryId 仓库Id
     * @return 库位信息
     */
    Response<List<Location>> findByCategoryAndSkuIsNull(Long repertoryId);
}
