/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * 还款履历类型
 *
 * @author : panxin
 */
public enum CreditRepaymentType {

    REPAYMENT(1, "还款"),
    REFUND(2, "退款");

    private final int value;

    private final String desc;

    CreditRepaymentType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static CreditRepaymentType from(int value) {
        for (CreditRepaymentType type : CreditRepaymentType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("credit.repayment.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
