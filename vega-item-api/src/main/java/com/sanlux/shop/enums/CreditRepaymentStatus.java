/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * 还款履历状态
 *
 * @author : panxin
 */
public enum  CreditRepaymentStatus {

    WAIT(0, "待运营审核"),
    PASS(1, "运营审核通过"),
    REFUSED(-1, "运营审核拒绝");

    private final int value;

    private final String desc;

    CreditRepaymentStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static CreditRepaymentStatus from(int value) {
        for (CreditRepaymentStatus status : CreditRepaymentStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("credit.repayment.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
