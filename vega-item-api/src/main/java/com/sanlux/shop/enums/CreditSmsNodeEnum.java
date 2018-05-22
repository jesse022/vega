package com.sanlux.shop.enums;

import com.google.common.base.Objects;

/**
 * 信用额度短信节点枚举
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/12/16
 * Time: 1:51 PM
 */
public enum  CreditSmsNodeEnum {

    REPAYMENT("repayment.credit", "提前三天还信用额度"),
    USER_REPAYMENT("repayment.user.credit", "专属会员提前三天还信用额度"), // 用于获取短信模板,短信节点控制同"REPAYMENT"
    RECOVERY("recovery.credit", "恢复信用额度"),
    USER_RECOVERY("recovery.user.credit", "专属会员恢复信用额度"), // 用于获取短信模板,短信节点控制同"RECOVERY"
    ALREADY_REPAYMENT("already.repayment.credit", "还信用额度");

    private final String name;

    private final String desc;

    CreditSmsNodeEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static CreditSmsNodeEnum from(String name) {
        for (CreditSmsNodeEnum node : CreditSmsNodeEnum.values()) {
            if (Objects.equal(node.name, name)) {
                return node;
            }
        }
        throw new IllegalArgumentException("credit.sms.node.undefined");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return desc;
    }
}
