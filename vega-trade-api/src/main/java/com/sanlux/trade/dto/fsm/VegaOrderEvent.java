/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.dto.fsm;

import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import lombok.Getter;

import java.util.Objects;

/**
 * 交易流程中典型的事件
 * <p>
 * Author:  songrenfei
 * Date: 2016-05-24
 */
public enum VegaOrderEvent {
    PLATFORM_CHECK(1, "platformCheck", "admin"),
    PLATFORM_CHECK_FOR_DEALER(2, "platformCheckForDealer", "admin"),
    PLATFORM_CHECK_FOR_YC(15, "platformCheckForYc", "admin"), // 友云采订单平台接单审核通过
    PLATFORM_CHECK_FOR_OFFLINE_PAYMENT(13, "platformCheckForOfflinePayment", "admin"), //运营后台线下支付跳转
    SUPPLIER_CHECK(3, "supplierCheck", "supplier"),
    FIRST_DEALER_DISPATCH_ORDER(4, "firstDealerDispatchOrder", "dealer_first"),
    FIRST_DEALER_RECEIVE_ORDER(5, "firstDealerReceiveOrder", "dealer_first"),
    FIRST_DEALER_OUT(10, "firstDealerOut", "dealer_first"),
    FIRST_DEALER_OUT_OVER(11, "firstDealerOutOver", "dealer_first"),
    FIRST_DEALER_OUT_OVER_SYNC(14, "firstDealerOutOverSync", "dealer_first"),
    FIRST_DEALER_PUT_STORAGE(12, "firstDealerPutStorage", "dealer_first"),
    SECOND_DEALER_RECEIVE_ORDER(6, "secondDealerReceiveOrder", "dealer_second"),
    PAY(7, "pay", "buyer"),
    SHIP(8, "ship", "seller"),
    CONFIRM(9, "confirm", "buyer"),
    BUYER_CANCEL(-1, "buyerCancel", "buyer"),
    SELLER_REJECT(-2, "sellerReject", "seller"),
    PLATFORM_CANCEL(-3, "platformCancel", "admin"),
    PLATFORM_REJECT(-4, "platformReject", "admin"),
    PLATFORM_REJECT_FOR_YC(-28, "platformRejectForYc", "admin"), // 友云采订单平台接单审核拒绝
    FIRST_DEALER_CANCEL(-5, "firstDealerCancel", "dealer_first"),
    FIRST_DEALER_REJECT_RECEIVE(-8, "firstDealerRejectReceive", "dealer_first"),//订单shopId需回滚(派单)
    SECOND_DEALER_REJECT_RECEIVE(-9, "secondDealerRejectReceive", "dealer_second"),//订单shopId需回滚
    SECOND_DEALER_CANCEL(-10, "secondDealerCancel", "dealer_second"),
    REFUND_APPLY(-11, "refundApply", "buyer"),
    REFUND_APPLY_AGREE(-12, "refundApplyAgree", "seller"),
    FIRST_DEALER_REFUND_APPLY_AGREE(-27, "firstDealerRefundApplyAgree", "dealer_first"),//一级经销商,买家付完款后待审核节点退款申请审核
    SUPPLIER_REFUND_APPLY_AGREE(-26, "supplierRefundApplyAgree", "seller"),//供应商同意退款,提交平台运营审核
    REFUND_APPLY_CANCEL(-13, "refundApplyCancel", "buyer"),
    REFUND_APPLY_REJECT(-14, "refundApplyReject", "seller"),
    REFUND(-15, "refund", "seller"),
    RETURN_APPLY(-16, "returnApply", "buyer"),
    RETURN_APPLY_AGREE(-17, "returnApplyAgree", "seller"),
    RETURN_APPLY_CANCEL(-18, "returnApplyCancel", "buyer"),
    RETURN_APPLY_REJECT(-19,"returnApplyReject", "seller"),
    RETURN(-20, "return", "buyer"),
    RETURN_REJECT(-21, "returnReject", "seller"),
    RETURN_CONFIRM(-22, "returnConfirm", "seller"),
    REFUND_APPLY_ADMIN_AGREE(-23, "refundApplyAdminAgree", "admin"),
    REFUND_APPLY_ADMIN_REJECT(-24, "refundApplyAdminReject", "admin"),
    REFUND_ADMIN(-25, "refundAdmin", "admin");

    @Getter
    private final int value;

    @Getter
    private final String text;

    /**
     * 事件的触发者, 可以有多个角色. 多个角色之间用,分割.
     */
    @Getter
    private final String operator;

    VegaOrderEvent(int value, String text, String operator) {
        this.value = value;
        this.text = text;
        this.operator = operator;
    }

    public static OrderEvent fromInt(Integer value) {
        for (OrderEvent orderEvent : OrderEvent.values()) {
            if (Objects.equals(orderEvent.getValue(), value)) {
                return orderEvent;
            }
        }
        throw new IllegalArgumentException("unknown order event: " + value);
    }

    public OrderOperation toOrderOperation() {
        return new OrderOperation(value, text, operator);
    }

}
