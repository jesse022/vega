package com.sanlux.item.enums;

import com.google.common.base.Objects;

/**
 * Created by lujm on 2018/4/18.
 */
public enum ToolMallItemImportStatus {
    INIT(0, "未同步"),
    SUCCESS(1, "同步成功"),
    FAIL(2,"同步失败");

    private final int value;

    private final String desc;

    ToolMallItemImportStatus(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static ToolMallItemImportStatus from(int value) {
        for (ToolMallItemImportStatus status : ToolMallItemImportStatus.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.tool.mall.item.import.status.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

    /**
     * 土猫网商品状态
     */
    public enum ToolMallItemStatus {
        SHELF(1, "上架"),
        NO_SHELF(0,"下架");

        private final int value;

        private final String desc;

        ToolMallItemStatus(int number, String desc) {
            this.value = number;
            this.desc = desc;
        }

        public static ToolMallItemStatus from(int value) {
            for (ToolMallItemStatus status : ToolMallItemStatus.values()) {
                if (Objects.equal(status.value, value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("vega.tool.mall.item.status.undefined");
        }

        public int value() {
            return value;
        }

        @Override
        public String toString() {
            return desc;
        }
    }
}
