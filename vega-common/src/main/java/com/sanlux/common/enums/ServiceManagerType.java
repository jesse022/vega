package com.sanlux.common.enums;

import com.google.common.base.Objects;

/**
 * 业务经理类型
 * Created by lujm on 2017/9/21.
 */

public enum ServiceManagerType {
    PLATFORM(0, "平台"),
    DEALER_FIRST(1, "一级经销商"),
    DEALER_SECOND(2, "二级经销商");

    private final int value;

    private final String desc;

    ServiceManagerType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static ServiceManagerType from(int value) {
        for (ServiceManagerType type : ServiceManagerType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("service.manager.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
