package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * Created by liangfujie on 16/10/28
 */
public enum VegaDirectPayInfoStatus {
    WAIT_PAY(0,"待打款"),
    WAIT_APPROVE(1,"待审核"),
    WAIT_BANK_PAY(2,"银行处理中"),
    PAY_SUCCESS(3,"打款成功"),
    PAY_FAILED(-1,"打款失败"),
    PAY_REJECT(-2,"审核不通过");

    private final int value;

    private final String desc;

    VegaDirectPayInfoStatus(int value, String desc) {
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

    public static VegaDirectPayInfoStatus from(int value) {
        for (VegaDirectPayInfoStatus status : VegaDirectPayInfoStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.direct.pay.Info.status.undefined");
    }

}
