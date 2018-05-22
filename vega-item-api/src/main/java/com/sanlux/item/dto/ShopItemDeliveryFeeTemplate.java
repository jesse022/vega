package com.sanlux.item.dto;

import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopItemDeliveryFee;
import io.terminus.parana.delivery.model.DeliveryFeeTemplate;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:cp
 * Created on 8/17/16.
 */
@Data
public class ShopItemDeliveryFeeTemplate implements Serializable {

    private static final long serialVersionUID = -4019630215132098117L;

    /**
     * 店铺商品
     */
    private ShopItem shopItem;

    /**
     * 店铺商品运费
     */
    private ShopItemDeliveryFee shopItemDeliveryFee;

    /**
     * 店铺所有运费模板
     */
    private List<DeliveryFeeTemplate> deliveryFeeTemplates;
}
