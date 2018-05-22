package com.sanlux.item.enums;

import com.google.common.base.Objects;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
public enum IntegrationItemStatus {

    ONSHELF(1, "上架"),
    OFFSHELF(-1, "下架"),
    DELETE(-3, "删除");

    private final int value;

    private final String desc;

    IntegrationItemStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static IntegrationItemStatus from(int value) {
        for (IntegrationItemStatus status : IntegrationItemStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.shop.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
