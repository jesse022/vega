/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * @author : panxin
 */
public enum  CreditAlterStatus {

    NO_PASS(-1, "审核未通过"),
    WAIT(0, "待还款"),
    WAIT_AUDIT(1, "已还款待审核"),
    PART_REPAYMENT(2, "已部分还款"),
    COMPLETE(3, "已还清");

    private final int value;

    private final String desc;

    CreditAlterStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static CreditAlterStatus from(int value) {
        for (CreditAlterStatus status : CreditAlterStatus.values()) {
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
