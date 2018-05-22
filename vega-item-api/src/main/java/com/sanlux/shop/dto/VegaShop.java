/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.parana.shop.model.Shop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VegaShop implements Serializable{

    private static final long serialVersionUID = 7308759765559652045L;

    private Shop shop;

    private VegaShopExtra shopExtra;

    // 上级经销商折扣(二级经销商才会设置该值)
    private Integer parentPurchaseDiscount;

    public VegaShop(Shop shop, VegaShopExtra shopExtra) {
        this.shop = shop;
        this.shopExtra = shopExtra;
    }

}
