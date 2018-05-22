package com.sanlux.pay.allinpay.enums;

import com.google.common.base.Objects;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/19/16
 * Time: 9:25 PM
 */
public enum  AlinnpayRefundHandleStatus {


    TKSUCC0001("TKSUCC0001", "退款未受理"),
    TKSUCC0002("TKSUCC0002", "待通联审核"),
    TKSUCC0003("TKSUCC0003", "通联审核通过"),
    TKSUCC0004("TKSUCC0004", "退款冲销"),
    TKSUCC0005("TKSUCC0005", "处理中"),
    TKSUCC0006("TKSUCC0006", "退款成功"),
    TKSUCC0007("TKSUCC0007", "退款失败"),
    TKSUCC0008("TKSUCC0008", "通联审核不通过");

    private final String value;

    private final String desc;

    AlinnpayRefundHandleStatus(String number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static AlinnpayRefundHandleStatus from(String value) {
        for (AlinnpayRefundHandleStatus status : AlinnpayRefundHandleStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.shop.status.undefined");
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
