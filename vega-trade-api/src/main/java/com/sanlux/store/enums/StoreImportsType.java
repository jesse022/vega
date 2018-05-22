package com.sanlux.store.enums;

import com.google.common.base.Objects;

/**
 * 进销存批量导入类型Enum
 * Created by lujm on 2017/3/17.
 */
public enum StoreImportsType {
    LOCATION(1,"库位导入"),
    IN_STORE(2,"入库单导入");


    private final int value;

    private final String desc;

    StoreImportsType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static StoreImportsType from(int value) {
        for (StoreImportsType status : StoreImportsType.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("storeImportsType.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
