package com.sanlux.item.service;

import com.sanlux.item.model.ShopItemDeliveryFee;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:cp
 * Created on 8/16/16.
 */
public interface ShopItemDeliveryFeeWriteService {

    /**
     * 创建或者更新店铺商品运费信息
     *
     * @param shopItemDeliveryFee 店铺商品运费信息
     * @return 是否操作成功
     */
    Response<Boolean> createOrUpdateShopItemDeliveryFee(ShopItemDeliveryFee shopItemDeliveryFee);

    /**
     * 批量创建或者更新店铺商品运费信息
     * @param toCreate toCreate
     * @param toUpdate toUpdate
     * @return Boolean
     */
    Response<Boolean> batchCreateAndUpdateShopItemDeliveryFee (List<ShopItemDeliveryFee> toCreate,
                                                               List<ShopItemDeliveryFee> toUpdate);

}
