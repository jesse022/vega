package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * 支付方式
 *
 * Created by lujm on 2017/8/21.
 */
public enum VegaPayType {
    ONLINE_PAYMENT(1,"在线支付"),
    DELIVERY_ON_CASH(2,"货到付款"),
    OFFLINE_PAYMENT(3,"线下转账");


    private final int value;

    private final String desc;

    VegaPayType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaPayType from(int value) {
        for (VegaPayType status : VegaPayType.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("order.pay.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
