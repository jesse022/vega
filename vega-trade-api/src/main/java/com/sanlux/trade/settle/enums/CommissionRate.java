/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.enums;

import com.google.common.base.Objects;

/**
 * @author : panxin
 */
public enum CommissionRate {

    FIRST_DEALER_COMMISSION_RATE("firstDealerCommissionRate", "一级经销商派单给二级经销商"),
    PLATFORM_COMMISSION_RATE("platformCommissionRate", "平台派单给一级经销商");

    private final String name;

    private final String desc;

    CommissionRate(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static CommissionRate from(String name) {
        for (CommissionRate type : CommissionRate.values()) {
            if (Objects.equal(type.name, name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("commission.rate.undefined");
    }

    public String value() {
        return name;
    }

    @Override
    public String toString() {
        return desc;
    }

}
