package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * 店铺是否授权标志
 *
 * Created by lujm on 2017/6/15.
 */
public enum VegaShopAuthorize {

    AUTHORIZE(1, "已授权"),
    NOT_AUTHORIZE(0, "未授权");

    private final int value;

    private final String desc;

    VegaShopAuthorize(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaShopAuthorize from(int value) {
        for (VegaShopAuthorize status : VegaShopAuthorize.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.shop.shopAuthorize.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
