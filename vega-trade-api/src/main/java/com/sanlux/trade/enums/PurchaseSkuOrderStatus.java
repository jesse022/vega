/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * 采购单商品 状态
 *
 * @author : songrenfei
 */
public enum PurchaseSkuOrderStatus {

    NO_CHOOSE(0,"未选中"),
    CHOOSED(1,"已选中");


    private final int value;

    private final String desc;

    PurchaseSkuOrderStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static PurchaseSkuOrderStatus from(int value) {
        for (PurchaseSkuOrderStatus status : PurchaseSkuOrderStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("purchase.sku.order.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
    
}
