/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.utils;

import com.google.common.base.Objects;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import io.terminus.pay.constants.Channels;

/**
 * @author : panxin
 */
public enum ChannelEnums {

    // 模拟支付
    MOCKPAY(Channels.MOCKPAY, "模拟支付"),
    MOCKPAY_WAP(Channels.MOCKPAY_WAP, "模拟支付网页版"),

    // 信用额度
    CREDIT_PAY(CreditPayConstants.PAY_CHANNEL, "信用额度"),
    CREDIT_PAY_WAP(CreditPayConstants.WAP_PAY_CHANNEL, "信用额度网页版"),

    // 支付宝
    ALIPAY_APP(Channels.Alipay.APP, "支付宝APP"),
    ALIPAY_PC(Channels.Alipay.PC, "支付宝"),
    ALIPAY_WAP(Channels.Alipay.WAP, "支付宝网页版"),

    // 微信
    WECHATPAY_APP(Channels.Wechatpay.APP, "微信支付APP"),
    WECHATPAY_PC(Channels.Wechatpay.QR, "微信支付"),
    WECHATPAY_WAP(Channels.Wechatpay.JSAPI, "微信支付网页版"),

    // 通联支付 TODO

    UNKNOWN_CHANNELS("unknownChannel", "未知的支付方式");

    private final String name;

    private final String desc;

    ChannelEnums(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static ChannelEnums from(String name) {
        for (ChannelEnums ch : ChannelEnums.values()) {
            if (Objects.equal(ch.name, name)) {
                return ch;
            }
        }
        return ChannelEnums.UNKNOWN_CHANNELS;
    }

    public String value() {
        return name;
    }

    @Override
    public String toString() {
        return desc;
    }

}
