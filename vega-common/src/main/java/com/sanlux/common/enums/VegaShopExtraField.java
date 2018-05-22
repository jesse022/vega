/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.common.enums;

import com.google.common.base.Objects;

/**
 * @author : panxin
 */
public enum VegaShopExtraField {

    RECEIVER_ACCOUNT_NAME("accountName", "收方账户名"),
    IS_CMBC("isCMBC", "招行标识"),
    RECEIVER_BANK("receiverBank", "收方开户行"),
    RECEIVER_BANK_CODE("receiverBankCode", "收方行号"),
    RECEIVER_BANK_CITY("receiverBankCity","收方城市"),
    RECEIVER_BANK_PROVINCE("receiverBankProvince","收方省份");
    private final String name;
    private final String desc;

    VegaShopExtraField(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static VegaShopExtraField from(String name) {
        for (VegaShopExtraField node : VegaShopExtraField.values()) {
            if (Objects.equal(node.name, name)) {
                return node;
            }
        }
        throw new IllegalArgumentException("vega.shop.extra.field.undefined");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return desc;
    }
}
