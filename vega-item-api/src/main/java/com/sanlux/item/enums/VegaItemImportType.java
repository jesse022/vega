package com.sanlux.item.enums;

import com.google.common.base.Objects;

/**
 * 商品导入类型
 * Created by lujm on 2018/3/23.
 */
public enum VegaItemImportType {
    API(1, "API导入"),
    EXCEL(0, "Excel导入"),
    TOOL_MALL_API(2,"土猫网API导入");

    private final int value;

    private final String desc;

    VegaItemImportType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static VegaItemImportType from(int value) {
        for (VegaItemImportType status : VegaItemImportType.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.item.import.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
