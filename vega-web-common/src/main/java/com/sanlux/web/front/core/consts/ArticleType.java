package com.sanlux.web.front.core.consts;

import com.google.common.base.Objects;

/**
 * 文章类型枚举
 *
 * Author  : panxin
 * Date    : 5:23 PM 3/21/16
 */
public enum ArticleType {

    SHOPPING_GUIDE(1, "购物指南"),
    DELIVERY_INFO(2, "配送信息"),
    PAY_METHOD(3, "支付方式"),
    AFTER_SALE_SERVICE(4, "售后服务"),
    SPECIAL_SERVICE(5, "特色服务");

    private final int value;

    private final String desc;

    ArticleType(int number, String desc) {
        this.value = number;
        this.desc = desc;
    }

    public static ArticleType from(int value) {
        for (ArticleType type : ArticleType.values()) {
            if (Objects.equal(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("article.type.undefined");
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
