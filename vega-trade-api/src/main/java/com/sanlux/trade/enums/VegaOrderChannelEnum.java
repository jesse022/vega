package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * 订单来源
 * Created by lujm on 2017/9/22.
 */
public enum VegaOrderChannelEnum {
    MOBILE(2,"手机"),
    PC(1,"电脑"),
    WE_CHAT(3,"微信"),
    YOU_YUN_CAI(4,"友云采");


    private final int value;

    private final String desc;

    VegaOrderChannelEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }


    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

    public static VegaOrderChannelEnum from(int value) {
        for (VegaOrderChannelEnum status : VegaOrderChannelEnum.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("vega.order.channel.undefined");
    }
}
