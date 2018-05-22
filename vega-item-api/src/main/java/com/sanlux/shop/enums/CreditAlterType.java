/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * 信用额度操作类型
 *
 * @author : panxin
 */
public enum  CreditAlterType {

    ADMIN_ADD(1, "运营添加"),
    ADMIN_REDUCE(2, "运营减少"),
    PERSONAL_CONSUME(3, "个人消费"),
    PERSONAL_REFUND(4, "个人退款"),
    PERSONAL_REPAYMENT(5, "个人还款");

    private final int value;

    private final String desc;

    CreditAlterType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static CreditAlterType from(int value) {
        for (CreditAlterType type : CreditAlterType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("credit.alter.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
