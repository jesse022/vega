package com.sanlux.store.enums;

import com.google.common.base.Objects;

/**
 * 进销存批量导入状态Enum
 * Created by lujm on 2017/3/17.
 */
public enum StoreImportsStatus {
    SUCCESS(1,"执行成功"),
    IN_HAND(0,"执行中"),
    NOT_STARTED(2,"未开始"),
    FAIL(-1,"执行失败");


    private final int value;

    private final String desc;

    StoreImportsStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static StoreImportsStatus from(int value) {
        for (StoreImportsStatus status : StoreImportsStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("storeImportsStatus.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
