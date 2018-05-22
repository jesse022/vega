/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.common.enums;

import com.google.common.base.Objects;

/**
 * Sanlux 店铺类型
 *
 * @author : panxin
 */
public enum VegaShopType {

    PLATFORM(0, "平台店铺"),
    SUPPLIER(1, "供应商"),
    DEALER_FIRST(2, "一级经销商"),
    DEALER_SECOND(3, "二级经销商"),
    OTHERS(4, "其他");

    private final int value;

    private final String desc;

    VegaShopType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaShopType from(int value) {
        for (VegaShopType type : VegaShopType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("vega.shop.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
