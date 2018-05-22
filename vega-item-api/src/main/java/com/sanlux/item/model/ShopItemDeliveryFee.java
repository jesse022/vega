package com.sanlux.item.model;

import io.terminus.parana.delivery.model.ItemDeliveryFee;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 店铺商品运费
 * Author:cp
 * Created on 8/16/16.
 */
@Data
@EqualsAndHashCode(of = "shopId", callSuper = true)
public class ShopItemDeliveryFee extends ItemDeliveryFee implements Serializable {

    private static final long serialVersionUID = 5042541473454482580L;

    /**
     * 店铺id
     */
    private Long shopId;

}
