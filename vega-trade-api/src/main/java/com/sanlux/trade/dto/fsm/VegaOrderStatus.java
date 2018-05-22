/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.dto.fsm;

import com.google.common.base.Objects;

/**
 * 订单(支付单, 退款单, 发货单)等的共享状态
 *
 * Author:  songrenfei
 * Date: 2016-05-23
 */
public enum VegaOrderStatus {

    NOT_PAID_PLATFORM(0),  //未付款(下给平台)
    NOT_PAID_FIRST_DEALER(1),  //未付款(下给一级)
    NOT_PAID_SECOND_DEALER(2),  //未付款(下给二级)
    PAID_WAIT_CHECK(3),  //已付款待审核
    YC_PAID_WAIT_CHECK(23),  //买家友云采已付款,待平台审核
    WAIT_SUPPLIER_CHECK(4),  //平台审核通过,待供应商审核
    SUPPLIER_CHECKED_WAIT_SHIPPED(5),  //供应商审核通过,待发货
    PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK(6),  //平台审核通过待一级经销商审核
    WAIT_FIRST_DEALER_CHECK(7),  //普通用户支付成功待一级经销商审核
    FIRST_DEALER_CHECKED_WAIT_OUT(18),//一级经销商审核通过,待出库(自己接单)
    FIRST_DEALER_OUT_WAITE_OVER(19),//一级经销商出库,待出库完成(自己接单)
    FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED(8),//一级经销商出库完成,待发货(自己接单)
    WAIT_SECOND_DEALER_CHECK(9),  //普通用户支付成功待二级经销商审核
    FIRST_DEALER_CHECKED_WAIT_OUT_PLATFORM(20),//一级经销商审核通过平台派给自己的单,待出库 (自己接单)
    FIRST_DEALER_OUT_WAIT_OVER_PLATFORM(21),//一级经销商出库平台派给自己的单,待完成 (自己接单)
    FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM(10),//一级经销商出库完成平台派给自己的单,待发货 (自己接单)
    FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK(11),//一级经销商审核通过用户下给自己的单,待二级经销商审核 (派单)
    FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM(12),//一级经销商审核通过平台派给自己的单,待二级经销商审核(派单)
    SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM(13),//二级经销商审核通过,待发货(来自平台)
    SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER(14),//二级经销商审核通过,待发货(来自一级)
    SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER(15),//二级经销商审核通过,待发货(来自二级)
    SHIPPED(16),   //已发货
    CONFIRMED(17),  //已确认收货
    PUT_STORAGE(22),  //已入库
    BUYER_CANCEL(-1),  // 买家关闭订单
    PLATFORM_CANCEL(-2),  // 平台取消
    PLATFORM_REJECT(-3),  // 平台拒绝
    YC_PLATFORM_REJECT(-60),  // 友云采订单平台拒绝
    SUPPLIER_REJECT(-4),  //商家拒绝订单
    FIRST_DEALER_CANCEL(-5),  //一级经销商取消订单
    FIRST_DEALER_REJECT(-6),  //一级经销商取消订单
    FIRST_DEALER_REJECT_RECEIVE(-7),  //一级经销商拒绝订单(平台派单)
    SECOND_DEALER_REJECT_RECEIVE(-8),  //二级经销商拒绝订单(一级派单,下给一级的单)
    SECOND_DEALER_REJECT(-9),  //二级经销商拒绝订单(下给自己的单,已支付)
    SECOND_DEALER_CANCEL(-10),  //二级经销商拒绝订单(下给自己的单,未支付)
    SECOND_DEALER_REJECT_RECEIVE_PLATFORM(-11),  //二级经销商拒绝订单(一级派单,下给平台的单)
    REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT(-12),    //申请退款(待平台审核-审核拒绝节点)
    REFUND_APPLY_AGREED_WAIT_PLATFORM_CHECK_REJECT(-13),          //同意退款(待平台审核-审核拒绝节点)
    REFUND_APPLY_REJECTED_WAIT_PLATFORM_CHECK_REJECT(-14),   //拒绝退款(待平台审核-审核拒绝节点)
    REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT(-15),    //申请退款(待供应商审核-审核拒绝节点)
    REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT(-16),          //供应商同意退款(待供应商审核-审核拒绝节点)
    REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_CHECK_REJECT(-53),          //运营同意退款(待供应商审核-审核拒绝节点)
    REFUND_APPLY_REJECTED_WAIT_SUPPLIER_CHECK_REJECT(-17),   //拒绝退款(待供应商审核-审核拒绝节点)
    REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_CHECK_REJECT(-54),   //运营拒绝退款(待供应商审核-审核拒绝节点)
    REFUND_APPLY_WAIT_SUPPLIER_SHIPP(-18),    //申请退款(待供应商审核-通过待发货节点)
    REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP(-19),          //同意退款(待供应商审核-通过待发货节点)
    REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_SHIPP(-55),          //运营同意退款(待供应商审核-通过待发货节点)
    REFUND_APPLY_REJECTED_WAIT_SUPPLIER_SHIPP(-20),   //拒绝退款(待供应商审核-通过待发货节点)
    REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_SHIPP(-56),   //运营拒绝退款(待供应商审核-通过待发货节点)
    REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP(-21),    //申请退款(待一级审核-通过待发货节点,下给一级)
    REFUND_APPLY_WAIT_FIRST_DEALER_CHECK(-57),    //申请退款(待一级审核节点,下给一级)
    REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP(-22),          //同意退款(待一级审核-通过待发货节点, 下给一级)
    REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK(-58),          //同意退款(待一级审核节点, 下给一级)
    REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP(-23),   //拒绝退款(待一级审核-通过待发货节点, 下给一级)
    REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK(-59),   //拒绝退款(待一级审核节点, 下给一级)
    REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT(-24),    //申请退款(待一级审核-拒绝节点,下给一级)
    REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK_REJECT(-25),          //同意退款(待一级审核-拒绝节点,下给一级)
    REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK_REJECT(-26),   //拒绝退款(待一级审核-拒绝节点,下给一级)
    REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE(-27),    //申请退款(待一级审核-通过待发货节点,派给一级)
    REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP_RECEIVE(-28),          //同意退款(待一级审核-通过待发货节点, 派给一级)
    REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP_RECEIVE(-29),   //拒绝退款(待一级审核-通过待发货节点, 派给一级)
    REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP(-30),    //申请退款(待二级审核-通过待发货节点,下给二级)
    REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP(-31),          //同意退款(待二级审核-通过待发货节点, 下给二级)
    REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP(-32),   //拒绝退款(待二级审核-通过待发货节点, 下给二级)
    REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT(-33),    //申请退款(待二级审核-拒绝节点,下给二级)
    REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_CHECK_REJECT(-34),          //同意退款(待二级审核-拒绝节点, 下给二级)
    REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_CHECK_REJECT(-35),   //拒绝退款(待二级审核-拒绝节点, 下给二级)
    REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM(-36),    //申请退款(待二级级审核-通过待发货节点,下给平台,派给自己)
    REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM(-37),          //同意退款(待二级审核-通过待发货节点, 下给平台,派给自己)
    REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM(-38),   //拒绝退款(待二级审核-通过待发货节点, 下给平台,派给自己)
    REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER(-39),    //申请退款(待二级审核-通过待发货节点,下给一级,派给自己)
    REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER(-40),          //同意退款(待二级审核-通过待发货节点, 下给一级,派给自己)
    REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER(-41),   //拒绝退款(待二级审核-通过待发货节点, 下给一级,派给自己)
    REFUND(-42), //已退款
    RETURN_REFUND(-50), //已退货退款
    RETURN_APPLY(-43),    //申请退货
    RETURN_APPLY_AGREED(-44),   //同意退款退货
    RETURN_APPLY_REJECTED(-45),   //拒绝退款退货
    RETURN(-46),         //买家已退货
    RETURN_CONFIRMED(-47), //商家确认退货
    RETURN_REJECTED(-48), //商家拒绝退货
    TIMEOUT_CANCEL(-49),  //超时关闭订单,平台接单
    TIMEOUT_FIRST_DEALER_CANCEL(-51),  //超时关闭订单,一级接单
    TIMEOUT_SECOND_DEALER_CANCEL(-52),  //超时关闭订单,二级接单
    BUYER_DELETE(-99); //买家删除订单


    private final int value;

    VegaOrderStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static VegaOrderStatus fromInt(int value){
        for (VegaOrderStatus orderStatus : VegaOrderStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown status: "+value);
    }
}
