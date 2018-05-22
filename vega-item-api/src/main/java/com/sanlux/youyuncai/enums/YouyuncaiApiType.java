package com.sanlux.youyuncai.enums;

import com.google.common.base.Objects;

/**
 * 友云采对接接口类型
 * Created by lujm on 2018/1/30.
 */
public enum  YouyuncaiApiType {

    GET_ACCESS_TOKEN(0,"获取授权token接口"),
    CATEGORY_INIT(1, "商品类目初始化"),
    ITEM_INIT(2, "商品信息初始化"),
    ITEM_ADD(3, "商品信息新增"),
    ITEM_UPDATE(4, "商品信息修改"),
    ITEM_DELETE(5, "商品信息删除"),
    ITEM_ALL(6, "商品新增修改和删除"),

    ORDER_CHECK_OUT(7, "订单check out接口"),
    ORDER_DELIVERY_ORDER(8, "订单交期确认接口"),
    ORDER_SHIP_INFO(9, "订单出货通知接口");


    private final int value;

    private final String desc;

    YouyuncaiApiType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static YouyuncaiApiType from(int value) {
        for (YouyuncaiApiType status : YouyuncaiApiType.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.you.yun.cai.api.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
