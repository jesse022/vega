/*
 * Copyright (c) OrderStatus.SHIPPED.getValue()016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.dto.fsm;

import io.terminus.parana.order.dto.fsm.Flow;

/**
 * Author:  songrenfei
 * Date: 2016-05-13
 */
public class VegaFlowBook {

    /**
     * 一级经销商下单
     */
    public static final Flow firstDealerOrder = new Flow("firstDealerOrder") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {

            //待付款 -->付款 -> 已付款待平台审核
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());
            //待付款 -->线下付款运营跳转 -> 已付款待平台审核
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());
            //待平台审核->审核通过->待供应商审核
            addTransition(VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK.toOrderOperation(),
                    VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue());
            //待平台审核 -> 平台拒绝(买家可申请退款)
            addTransition(VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_REJECT.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());
            //待供应商审核->通过->待发货
            addTransition(VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),
                    VegaOrderEvent.SUPPLIER_CHECK.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //待供应商审核->不通过->审核不通过
            addTransition(VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //待发货-> 商家发货 -> 已发货
            addTransition(VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //已发货 ->买家确认收货 -> 待入库
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.CONFIRM.toOrderOperation(),
                    VegaOrderStatus.CONFIRMED.getValue());
            //待入库 ->入库 -> 入库完成
            addTransition(VegaOrderStatus.CONFIRMED.getValue(),
                    VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation(),
                    VegaOrderStatus.PUT_STORAGE.getValue());


            //在支付之前买卖双方均可取消订单
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.BUYER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.BUYER_CANCEL.getValue()); //待平台审核 -> 买家取消
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PLATFORM_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_CANCEL.getValue()); //待平台审核 -> 平台取消


            //addTransition(0, new OrderOperation("system", "超时关闭"), -3); //待付款 -> 超时取消

            //发货前
            //1、===================在付款后待平台审核-审核拒绝节点, 买家可申请退款=================
            //待平台审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.PLATFORM_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //2、===================在供应商审核-审核拒绝节点, 买家可申请退款=================
            // 待供应商审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.SUPPLIER_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //买家申请退款 -> 供应商同意退款 -> 平台运营审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.SUPPLIER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //平台运营审核 -> 平台运营同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //平台运营审核 -> 平台运营拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue());

            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //平台运营拒绝退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //平台运营同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //3、===================在供应商审核-通过待发货节点, 买家可申请退款=================
            // 待供应商审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> 供应商同意退款 -> 平台运营审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.SUPPLIER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue());
            //平台运营审核 -> 平台运营同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_SHIPP.getValue());
            //平台运营审核 -> 平台运营拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //商家拒绝退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //平台运营拒绝退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //平台运营同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());



            //在发货后, 买家可申请退款退货
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.RETURN_APPLY.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY.getValue());  //已发货 -> 申请退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_AGREED.getValue()); //退款退货-> 商家同意退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_REJECTED.getValue()); //退款退货 -> 商家拒绝退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //申请退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_REJECTED.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //拒绝退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_AGREED.getValue(),
                    VegaOrderEvent.RETURN.toOrderOperation(),
                    VegaOrderStatus.RETURN.getValue()); // 商家同意退款退货 -> 买家已退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_CONFIRM.toOrderOperation(),
                    VegaOrderStatus.RETURN_CONFIRMED.getValue()); //买家已退货 -> 商家已确认收退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_REJECTED.getValue()); //买家已退货 -> 商家拒绝退货
            addTransition(VegaOrderStatus.RETURN_CONFIRMED.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.RETURN_REFUND.getValue()); //商家确认退货 -> 商家已退款


            //4、===================解决支付期间,用户支付成功,但订单状态已变成超时关闭问题=================
            //超时关闭 -->付款 -> 已付款待平台审核
            addTransition(VegaOrderStatus.TIMEOUT_CANCEL.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());

        }
    };


    /**
     * 二级经销商下单
     */
    public static final Flow secondDealerOrder = new Flow("secondDealerOrder") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {

            //待付款 -->付款 -> 已付款待待一级审核
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //待付款 -->线下付款运营跳转 -> 已付款待一级审核
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //已付款待一级审核 -> 审核通过 ->待出库
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue());

            //待出库 -> 出库 ->出库待出库完成
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue());

            //待出库完成 -> 完成 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());

            //待出库完成 -> 手工同步 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER_SYNC.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());
            //已付款待一级审核 一级可拒绝(买家可发起申请退款)
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());
            ///待发货-> 商家发货 -> 已发货
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.CONFIRM.toOrderOperation(),
                    VegaOrderStatus.CONFIRMED.getValue()); //已发货 ->买家确认收货 -> 交易完成

            //在支付之前买卖双方均可取消订单
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.BUYER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.BUYER_CANCEL.getValue()); //待一级审核 -> 买家取消
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CANCEL.getValue()); //待一级审核  -> 一级取消




            //addTransition(0, new OrderOperation("system", "超时关闭"), -3); //待付款 -> 超时取消

            //在付款后发货前, 买家可申请退款
            //0、============== 买家付完款-待卖家审核 可申请退款=====================
            //待一级审核 -> 买家申请退款
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核节点-待一级审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核节点-待一级审核
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());

            //1、============== 待一级出库完成-出库完成待发货 可申请退款=====================
            //待一级审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> (买家撤销退款)待一级出库完成-出库完成待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级出库完成-出库完成待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());



            //2、============== 待一级审核-拒绝 可申请退款=====================
            //待一级审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.FIRST_DEALER_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());





            //在发货后, 买家可申请退款退货
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.RETURN_APPLY.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY.getValue());  //已发货 -> 申请退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_AGREED.getValue()); //退款退货-> 商家同意退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_REJECTED.getValue()); //退款退货 -> 商家拒绝退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //申请退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_REJECTED.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //拒绝退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_AGREED.getValue(),
                    VegaOrderEvent.RETURN.toOrderOperation(),
                    VegaOrderStatus.RETURN.getValue()); // 商家同意退款退货 -> 买家已退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_CONFIRM.toOrderOperation(),
                    VegaOrderStatus.RETURN_CONFIRMED.getValue()); //买家已退货 -> 商家已确认收退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_REJECTED.getValue()); //买家已退货 -> 商家拒绝退货
            addTransition(VegaOrderStatus.RETURN_CONFIRMED.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.RETURN_REFUND.getValue()); //商家确认退货 -> 商家已退款
            addTransition(VegaOrderStatus.RETURN_REFUND.getValue(),
                    VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation(),
                    VegaOrderStatus.PUT_STORAGE.getValue()); //商家已退款 入库 -> 商家已入库


            //3、===================解决支付期间,用户支付成功,但订单状态已变成超时关闭问题=================
            //超时关闭 -->付款 -> 已付款待一级审核
            addTransition(VegaOrderStatus.TIMEOUT_FIRST_DEALER_CANCEL.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());

        }
    };


    /**
     * 普通用户下单
     */
    public static final Flow buyerOrder = new Flow("buyerOrder") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {
            //下单下给了平台

            //友云采买家已付款 -->平台审核通过 -> 已付款待平台审核(正常流程)
            addTransition(VegaOrderStatus.YC_PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_YC.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());
            //友云采买家已付款 -->平台审核不通过 -> 友云采订单平台拒绝
            addTransition(VegaOrderStatus.YC_PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_REJECT_FOR_YC.toOrderOperation(),
                    VegaOrderStatus.YC_PLATFORM_REJECT.getValue());


            //待付款 -->付款 -> 已付款待平台审核
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());
            //待付款 -->线下付款运营跳转 -> 已付款待平台审核
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());

            //-----------派给一级经销商---------
            //待平台审核->审核通过派单->待一级审核
            addTransition(VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_DEALER.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue());


            //已付款待一级审核 -> 审核通过 ->待出库 一级接单
            addTransition(VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT_PLATFORM.getValue());

            //待出库 -> 出库 ->出库待出库完成
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_OUT_WAIT_OVER_PLATFORM.getValue());

            //待出库完成 -> 完成 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAIT_OVER_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue());

            //待出库完成 ->手工同步 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAIT_OVER_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER_SYNC.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue());

            //平台派给了一级经销商 待一级审核  一级拒绝
            addTransition(VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_REJECT_RECEIVE.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue());
            //平台派给了一级经销商  一级拒绝  平台重新派单(to 供应商)
            addTransition(VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK.toOrderOperation(),
                    VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue());
            //平台派给了一级经销商  一级拒绝  平台重新派单(to 经销商)
            addTransition(VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_DEALER.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue());
            //平台派给了一级经销商  一级拒绝 后 平台拒绝
            addTransition(VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.PLATFORM_REJECT.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());

            //-----------派给一级经销商 一级派给了二级 ------------
            //平台派给了一级经销商 一级派给了二级 待二级审核
            addTransition(VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_DISPATCH_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue());

            //平台派给了一级 一级派给了二级 待二级审核 接单 -->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue(),
                    VegaOrderEvent.SECOND_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue());

            //平台派给了一级经销商 一级派给了二级 待二级审核 二级拒绝 --》一级重新派单
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue(),
                    VegaOrderEvent.SECOND_DEALER_REJECT_RECEIVE.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE_PLATFORM.getValue());

            //平台派给了一级经销商 一级派给二级 二级拒绝一级派送 1、一级继续派单
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_DISPATCH_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue());

            //平台派给了一级经销商 一级派给二级 二级拒绝一级派送 2、一级可以拒绝
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_REJECT_RECEIVE.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue());

            //平台派给了一级经销商 一级派给二级 二级拒绝一级派送 3、一级接单 待出库
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT_PLATFORM.getValue());


            //-----------派给供应商---------------
            //待平台审核->审核通过->待供应商审核
            addTransition(VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK.toOrderOperation(),
                    VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue());
            //待供应商审核->通过->待发货
            addTransition(VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),
                    VegaOrderEvent.SUPPLIER_CHECK.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //待供应商审核->不通过->审核不通过
            addTransition(VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());


            //===================解决支付期间,用户支付成功,但订单状态已变成超时关闭问题=================
            //超时关闭 -->付款 -> 已付款待平台审核
            addTransition(VegaOrderStatus.TIMEOUT_CANCEL.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.PAID_WAIT_CHECK.getValue());



            //######################################################################################

            //下单下给了一级经销商

            //下单下给了一级 待付款 -->付款 -> 已付款待待一级审核
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //待付款 -->线下付款运营跳转 -> 已付款待待一级审核
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());


            //已付款待一级审核 -> 审核通过 ->待出库
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue());

            //待出库 -> 出库 ->出库待出库完成
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue());

            //待出库完成 -> 完成 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());

            //待出库完成 -> 手工同步 ->待发货
            addTransition(VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_OUT_OVER_SYNC.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());

            //下单下给了一级经销商 待一级审核 一级拒绝(买家可申请退款)
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());

            //下单下给了一级经销商 一级派给了二级 -->待二级经销商审核
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_DISPATCH_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue());

            //下单下给了一级 一级派给了二级 待二级审核 接单 -->待发货(买家可申请退款)
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SECOND_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue());
            //下单下给了一级经销商 一级派给了二级 待二级审核 二级拒绝(一级重新派单)
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SECOND_DEALER_REJECT_RECEIVE.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE.getValue());

            //下单下给了一级经销商 一级派给了二级 二级拒绝一级派送 1、一级继续派单
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.FIRST_DEALER_DISPATCH_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue());

            //下单下给了一级经销商 一级派给了二级 二级拒绝一级派送 2、一级可以拒绝(买家可申请退款)
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());

            //下单下给了一级经销商 一级派给了二级 二级拒绝一级派送 2、一级可以接单,接单后待出库
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE.getValue(),
                    VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue());

            //===================解决支付期间,用户支付成功,但订单状态已变成超时关闭问题=================
            //超时关闭 -->付款 -> 已付款待一级审核
            addTransition(VegaOrderStatus.TIMEOUT_FIRST_DEALER_CANCEL.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());

            //######################################################################################
            //下单下给了二级经销商
            //下单下给了二级 待付款 -->付款 -> 已付款待二级审核
            addTransition(VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue());
            //待付款 -->线下付款运营跳转 -> 已付款待二级审核
            addTransition(VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue(),
                    VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT.toOrderOperation(),
                    VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue());
            //下单下给了二级 待二级审核 二级接单 待发货(买家可申请退款)
            addTransition(VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SECOND_DEALER_RECEIVE_ORDER.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue());
            //下单下给了二级 待二级审核 二级拒绝(买家可申请退款)
            addTransition(VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue(),
                    VegaOrderEvent.SELLER_REJECT.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_REJECT.getValue());


            //------------ 发货 ----------
            //平台派给供应商 供应商接单 待发货-> 商家发货 -> 已发货
            addTransition(VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //平台派给一级 一级接单 待发货 发货
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //平台派给一级 一级派给了二级 二级接单 -->待发货
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //下单下给一级 一级接单 出库完成 待发货 发货
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //下单下给了一级 一级派给了二级 二级接单 -->待发货
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            //下单下给了二级级 二级接单 -->待发货
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue(),
                    VegaOrderEvent.SHIP.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue());
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.CONFIRM.toOrderOperation(),
                    VegaOrderStatus.CONFIRMED.getValue());

            //===================解决支付期间,用户支付成功,但订单状态已变成超时关闭问题=================
            //超时关闭 -->付款 -> 已付款待二级审核
            addTransition(VegaOrderStatus.TIMEOUT_SECOND_DEALER_CANCEL.getValue(),
                    VegaOrderEvent.PAY.toOrderOperation(),
                    VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue());

            //#################### 逆向 下单下给了平台##########################

            //在支付之前买卖双方均可取消订单
            //下单下给了平台 待支付 买家取消
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.BUYER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.BUYER_CANCEL.getValue()); //待平台审核 -> 买家取消
            //下单下给了平台 待平支付 平台取消
            addTransition(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderEvent.PLATFORM_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_CANCEL.getValue()); //待平台审核 -> 平台取消
            //下单下给了平台 待平台审核 平台拒绝(买家可申请退款)
            addTransition(VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                    VegaOrderEvent.PLATFORM_REJECT.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());



            //发货前申请退款
            //1、===================在付款后待平台审核-审核拒绝节点, 买家可申请退款=================
            //待平台审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.PLATFORM_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_PLATFORM_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.PLATFORM_REJECT.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_PLATFORM_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //2、===================在供应商审核-审核拒绝节点, 买家可申请退款=================
            // 待供应商审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.SUPPLIER_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //买家申请退款 -> 供应商同意退款 -> 平台运营审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.SUPPLIER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //平台运营审核 -> 平台运营同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //平台运营审核 -> 平台运营拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue());

            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //平台运营拒绝退款 -> (买家撤销退款)待供应商审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_REJECT.getValue());
            //平台运营同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //3、===================在供应商审核-通过待发货节点, 买家可申请退款=================
            // 待供应商审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> 供应商同意退款 -> 平台运营审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.SUPPLIER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue());
            //平台运营审核 -> 平台运营同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_SHIPP.getValue());
            //平台运营审核 -> 平台运营拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_SHIPP.getValue());
            //买家申请退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //商家拒绝退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());
            //平台运营拒绝退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_REJECTED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue());

            //平台运营同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_ADMIN_AGREED_WAIT_SUPPLIER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_ADMIN.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());

            //4、===================在平台派单给一级 一级通过待发货节点, 买家可申请退款=================
            // 待一级审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //5、===================在平台派单给一级 一级派给二级 二级通过待发货节点, 买家可申请退款=================
            // 待一级审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue());
            //买家申请退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue());
            //商家拒绝退款 -> (买家撤销退款)待供应商审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());




            //############################# 逆向 下单下给了一级经销商 ##############################

            //下单下给了一级经销商 待支付 买家取消
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.BUYER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.BUYER_CANCEL.getValue()); //待一级审核 -> 买家取消
            //下单下给了一级经销商 待支付 一级取消
            addTransition(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderEvent.FIRST_DEALER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CANCEL.getValue()); //待一级审核  -> 一级取消






            //addTransition(0, new OrderOperation("system", "超时关闭"), -3); //待付款 -> 超时取消


            //发货前申请退款
            //0、============== 买家付完款-待卖家审核 可申请退款=====================
            //待一级审核 -> 买家申请退款
            addTransition(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.FIRST_DEALER_REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核节点-待一级审核
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核节点-待一级审核
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());

            //1、============== 待一级审核-通过待发货 可申请退款=====================
            //待一级审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());
            //2、===================在付款后待一级审核-审核拒绝节点, 买家可申请退款=================
            //待一级审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.FIRST_DEALER_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.FIRST_DEALER_REJECT.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_FIRST_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());

            //3、===================在付款后待一级审核 一级派给二级-二级审核通过节点, 买家可申请退款=================
            //待二级级审核-通过 -> 买家申请退款
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue());
            //买家申请退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue());
            //商家拒绝退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());


            //############################# 逆向 下单下给了二级经销商 ##############################

            //下单下给了二级经销商 待支付 买家取消
            addTransition(VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue(),
                    VegaOrderEvent.BUYER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.BUYER_CANCEL.getValue());
            //下单下给了二级经销商 待支付 二级取消
            addTransition(VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue(),
                    VegaOrderEvent.SECOND_DEALER_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CANCEL.getValue());


            //addTransition(0, new OrderOperation("system", "超时关闭"), -3); //待付款 -> 超时取消


            //发货前申请退款
            //1、============== 待二级审核-通过待发货 可申请退款=====================
            //待二级审核-通过待发货 -> 买家申请退款
            addTransition(VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP.getValue());
            //买家申请退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue());
            //商家拒绝退款 -> (买家撤销退款)待一级审核-通过待发货
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_SHIPP.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());
            //2、===================在付款后待二级审核-审核拒绝节点, 买家可申请退款=================
            //待二级审核-拒绝 -> 买家申请退款
            addTransition(VegaOrderStatus.SECOND_DEALER_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家同意退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> 商家拒绝退款
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_CHECK_REJECT.getValue());
            //买家申请退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_REJECT.getValue());
            //商家拒绝退款 -> (买家撤销退款)待平台审核-拒绝
            addTransition(VegaOrderStatus.REFUND_APPLY_REJECTED_WAIT_SECOND_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SECOND_DEALER_REJECT.getValue());
            //商家同意退款 -> 商家已退款
            addTransition(VegaOrderStatus.REFUND_APPLY_AGREED_WAIT_SECOND_DEALER_CHECK_REJECT.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.REFUND.getValue());




            //在发货后, 买家可申请退款退货
            addTransition(VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderEvent.RETURN_APPLY.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY.getValue());  //已发货 -> 申请退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_AGREE.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_AGREED.getValue()); //退款退货-> 商家同意退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY_REJECTED.getValue()); //退款退货 -> 商家拒绝退款退货
            addTransition(VegaOrderStatus.RETURN_APPLY.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //申请退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_REJECTED.getValue(),
                    VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    VegaOrderStatus.SHIPPED.getValue()); //拒绝退款退货 -> 已发货 (买家撤销退货申请)
            addTransition(VegaOrderStatus.RETURN_APPLY_AGREED.getValue(),
                    VegaOrderEvent.RETURN.toOrderOperation(),
                    VegaOrderStatus.RETURN.getValue()); // 商家同意退款退货 -> 买家已退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_CONFIRM.toOrderOperation(),
                    VegaOrderStatus.RETURN_CONFIRMED.getValue()); //买家已退货 -> 商家已确认收退货
            addTransition(VegaOrderStatus.RETURN.getValue(),
                    VegaOrderEvent.RETURN_REJECT.toOrderOperation(),
                    VegaOrderStatus.RETURN_REJECTED.getValue()); //买家已退货 -> 商家拒绝退货
            addTransition(VegaOrderStatus.RETURN_CONFIRMED.getValue(),
                    VegaOrderEvent.REFUND.toOrderOperation(),
                    VegaOrderStatus.RETURN_REFUND.getValue()); //商家确认退货 -> 商家已退款
            addTransition(VegaOrderStatus.RETURN_REFUND.getValue(),
                    VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation(),
                    VegaOrderStatus.PUT_STORAGE.getValue()); //商家已退款 入库 -> 商家已入库

        }
    };




}
