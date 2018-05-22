package com.sanlux.common.enums;

import com.google.common.base.Objects;

/**
 * 各类备注枚举
 * Created by lujm on 2018/1/25.
 */
public enum VegaNoteType {

    OPERATION_ORDER_NOTE(1, "运营后台订单备注"),
    SELLER_ORDER_NOTE(2, "卖家中心订单备注"),
    ORDER_INVOICE_NOTE(3, "订单发票信息备注");

    private final int value;

    private final String desc;

    VegaNoteType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaNoteType from(int value) {
        for (VegaNoteType type : VegaNoteType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("vega.note.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
