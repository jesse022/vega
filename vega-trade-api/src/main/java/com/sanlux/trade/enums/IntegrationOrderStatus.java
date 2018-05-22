package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * Created by cuiwentao
 * on 16/10/11
 */
public enum IntegrationOrderStatus {

    WAIT_DELIVERY(1,"待发货"),
    DONE(2,"已发货");


    private final int value;

    private final String desc;

    IntegrationOrderStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static IntegrationOrderStatus from(int value) {
        for (IntegrationOrderStatus status : IntegrationOrderStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("integration.order.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
