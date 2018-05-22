package com.sanlux.trade.enums;

import com.google.common.base.Objects;

/**
 * 交易短信节点枚举
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/12/16
 * Time: 1:51 PM
 */
public enum TradeSmsNodeEnum {



    CREATE("order.create", "创建订单"),
    PAID("order.paid", "付款成功"),
    BUYER_CANCEL("order.buyer.cancel", "买家取消订单"),
    SELLER_CANCEL("order.seller.cancel", "卖家取消订单"),
    CHECKED("order.checked", "订单审核通过"),//对买家而言
    REJECT("order.reject", "订单审核不通过"),//对买家而言
    DISPATCHER("order.dispatcher", "审核通过并派单"),//发给所派给的人(派单而言)
    DISPATCHER_RECEIVE("order.dispatcher.receive", "审核通过并接单"),//发给派单人(派单而言)
    DISPATCHER_REJECT("order.dispatcher.reject", "拒绝接单"),//发给派单人(派单而言)
    APPLY_REFUND("order.apply.refund", "申请退款"),
    AGREE_REFUND("order.agree.refund", "商家同意退款"),
    REJECT_REFUND("order.reject.refund", "商家拒绝退款"),
    BUYER_CANCEL_REFUND("order.buyer.cancel.refund", "买家取消退款"),
    APPLY_RETURN("order.apply.return", "申请退货退款"),
    AGREE_RETURN("order.agree.return", "商家同意退货退款"),
    REJECT_RETURN("order.reject.return", "商家拒绝退货退款"),
    BUYER_CANCEL_RETURN("order.buyer.cancel.return", "买家取消退货退款"),
    BUYER_RETURN("order.buyer.return", "买家退货"),
    SELLER_RECEIVE_RETURN("order.seller.receive.return", "商家收到买家已退货货物"),
    SHIPPED("order.shipped", "发货"),
    CONFIRMED("order.confirmed", "确认收货");

    private final String name;

    private final String desc;

    TradeSmsNodeEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static TradeSmsNodeEnum from(String name) {
        for (TradeSmsNodeEnum node : TradeSmsNodeEnum.values()) {
            if (Objects.equal(node.name, name)) {
                return node;
            }
        }
        throw new IllegalArgumentException("trade.sms.node.undefined");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return desc;
    }
}
