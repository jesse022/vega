package com.sanlux.item.service;

import com.sanlux.item.model.ShopItemDeliveryFee;
import io.terminus.common.model.Response;
import io.terminus.parana.delivery.dto.RichItemDeliveryFee;

import java.util.List;

/**
 * Author:cp
 * Created on 8/25/16.
 */
public interface ShopItemDeliveryFeeReadService {

    /**
     * 根据店铺Id和商品id列表查询店铺商品详细运费信息
     *
     * @param shopId  店铺id
     * @param itemIds 商品id列表
     * @return 店铺商品详细运费信息
     */
    Response<List<RichItemDeliveryFee>> findDeliveryFeeDetailByShopIdAndItemIds(Long shopId, List<Long> itemIds);

}
