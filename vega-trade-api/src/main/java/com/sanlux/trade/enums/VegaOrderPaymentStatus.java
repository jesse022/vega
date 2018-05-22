package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * Created by syf on 2017/8/10.
 */
public enum VegaOrderPaymentStatus {

    WAIT_PAYMENT(0,"待支付"),
    SUCCESS_PAYMENT(1,"已支付"),
    DELETE_PAYMENT(-1,"删除");

    private final int value;

    private final String desc;

    VegaOrderPaymentStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

    public static VegaOrderPaymentStatus from(int value) {
        for (VegaOrderPaymentStatus status : VegaOrderPaymentStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.order.payment.status.undefined");
    }
}
