package com.sanlux.item.enums;

import com.google.common.base.Objects;

/**
 * 土猫网数据同步类型
 * Created by lujm on 2018/4/18.
 */
public enum ToolMallItemImportType {
    CATEGORY(1, "商品分类"),
    ITEM_ALL(2, "商品属性"),
    ITEM_OTHER(3,"商品(价格/库存/上下架)");

    private final int value;

    private final String desc;

    ToolMallItemImportType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static ToolMallItemImportType from(int value) {
        for (ToolMallItemImportType status : ToolMallItemImportType.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.tool.mall.item.import.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

    /**
     * 商品查询类型
     */
    public enum ItemSyncType {
        ALL(1, "整体"),
        PRICE(2, "价格"),
        STOCK(4,"库存"),
        SHELF(8,"上下架"),
        PRICE_STOCK_SHELF(14,"价格|库存|上下架");

        private final int value;

        private final String desc;

        ItemSyncType(int number, String desc) {
            this.value = number;
            this.desc = desc;
        }

        public static ItemSyncType from(int value) {
            for (ItemSyncType status : ItemSyncType.values()) {
                if (Objects.equal(status.value, value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("vega.tool.mall.item.sync.type.undefined");
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
