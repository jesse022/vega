package com.sanlux.common.enums;

import com.google.common.base.Objects;

/**
 * Sanlux 用户类型
 * Created by lujm on 2017/3/28.
 */
public enum VegaUserType {
    SUPPLIER("SUPPLIER", "供应商"),
    DEALER_FIRST("DEALER_FIRST", "一级经销商"),
    DEALER_SECOND("DEALER_SECOND", "二级经销商"),
    ADMIN("ADMIN", "管理员"),
    BUYER("BUYER", "普通用户"),
    OPERATOR("OPERATOR", "运营用户"),
    OTHERS("OTHERS", "其他");

    private final String value;

    private final String desc;

    VegaUserType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static VegaUserType from(String value) {
        for (VegaUserType type : VegaUserType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("vega.user.type.undefined");
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
