/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * Sanlux 店铺(经销商)状态
 *
 * @author : panxin
 */
public enum  VegaShopStatus {

    NORMAL(1, "正常"),
    WAIT(0, "待审核"),
    NO_PASS(-2, "审核未通过"),
    FROZEN(-1, "冻结");

    private final int value;

    private final String desc;

    VegaShopStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaShopStatus from(int value) {
        for (VegaShopStatus status : VegaShopStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.shop.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
