package com.sanlux.item.service;

import com.sanlux.item.dto.ShopItemDeliveryFeeTemplate;
import io.terminus.common.model.Response;

/**
 * Author:cp
 * Created on 8/17/16.
 */
public interface VegaDeliveryFeeTemplateReadService {

    /**
     * 检查是否有商品绑定该运费模板
     *
     * @param deliveryFeeTemplateId 运费模板id
     * @return 有返回true, 否则false
     */
    Response<Boolean> checkIfHasItemBindTemplate(Long deliveryFeeTemplateId);

    /**
     * 查询店铺商品的运费模板信息
     *
     * @param shopId 店铺id
     * @param itemId 商品id
     * @return 店铺商品运费模板信息
     */
    Response<ShopItemDeliveryFeeTemplate> findShopItemDeliveryFeeTemplate(Long shopId, Long itemId);

}
