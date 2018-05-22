package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * Created by lujm on 2017/3/27.
 */
public enum VegaShipmentStatus {
    WAIT_SHIPMENT(0,"待发货"),
    SUCCESS_SHIPMENT(1,"已发货"),
    SUCCESS_CONFIRM(2,"已收货"),
    DELETE_SHIPMENT(-1,"已删除");

    private final int value;

    private final String desc;

    VegaShipmentStatus(int value, String desc) {
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

    public static VegaShipmentStatus from(int value) {
        for (VegaShipmentStatus status : VegaShipmentStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.shipment.status.undefined");
    }
}

