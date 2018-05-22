package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * 三力士相关费率定义类型
 *
 * Created by lujm on 2017/11/16.
 */
public enum VegaRateType {
    NEW_MEMBER_ORDER_COMMISSION(1, "newMemberOrderCommission", "新会员收取订单金额提成费率"),
    OLD_MEMBER_ORDER_COMMISSION(1, "oldMemberOrderCommission", "老会员收取订单金额提成费率");


    private final int type;

    private final String rateName;

    private final String desc;

    VegaRateType(int type, String rateName, String desc) {
        this.type = type;
        this.rateName = rateName;
        this.desc = desc;
    }

    public static VegaRateType from(int type, String rateName) {
        for (VegaRateType status : VegaRateType.values()) {
            if (Objects.equal(status.type, type) && Objects.equal(status.rateName, rateName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("rate.type.undefined");
    }

    public int type() {
        return type;
    }

    public String rateName() {
        return rateName;
    }

    @Override
    public String toString() {
        return desc;
    }
}
