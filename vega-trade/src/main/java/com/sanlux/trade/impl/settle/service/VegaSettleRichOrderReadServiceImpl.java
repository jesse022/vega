/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.service;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.OrderPaymentDao;
import io.terminus.parana.order.impl.dao.OrderRefundDao;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.impl.dao.PaymentDao;
import io.terminus.parana.order.impl.dao.RefundDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderPayment;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.settle.dto.RefundEntry;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.dto.TradeCriteria;
import io.terminus.parana.settle.enums.PayType;
import io.terminus.parana.settle.enums.RefundLevelType;
import io.terminus.parana.settle.impl.dao.SettleOrderDetailDao;
import io.terminus.parana.settle.impl.dao.SettleRefundOrderDetailDao;
import io.terminus.parana.settle.impl.service.SettleRichOrderReadServiceImpl;
import io.terminus.parana.settle.service.SettleRichOrderReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author : panxin
 */
@Slf4j
@Primary
@Service
@RpcProvider
public class VegaSettleRichOrderReadServiceImpl extends SettleRichOrderReadServiceImpl implements SettleRichOrderReadService {

    private final RefundDao refundDao;

    private final PaymentDao paymentDao;

    private final SkuOrderDao skuOrderDao;

    private final ShopOrderDao shopOrderDao;

    private final OrderRefundDao orderRefundDao;

    private final OrderPaymentDao orderPaymentDao;

    private final OrderShipmentDao orderShipmentDao;

    private final SettleOrderDetailDao settleOrderDetailDao;

    private final SettleRefundOrderDetailDao refundOrderDetailDao;

    @Autowired
    public VegaSettleRichOrderReadServiceImpl(RefundDao refundDao,
                                              PaymentDao paymentDao,
                                              SkuOrderDao skuOrderDao,
                                              ShopOrderDao shopOrderDao,
                                              OrderRefundDao orderRefundDao,
                                              OrderPaymentDao orderPaymentDao,
                                              OrderShipmentDao orderShipmentDao,
                                              SettleOrderDetailDao settleOrderDetailDao,
                                              SettleRefundOrderDetailDao refundOrderDetailDao) {
        super(refundDao, paymentDao, skuOrderDao, shopOrderDao, orderRefundDao, orderPaymentDao,
                orderShipmentDao, settleOrderDetailDao, refundOrderDetailDao);
        this.refundDao = refundDao;
        this.paymentDao = paymentDao;
        this.skuOrderDao = skuOrderDao;
        this.shopOrderDao = shopOrderDao;
        this.orderRefundDao = orderRefundDao;
        this.orderPaymentDao = orderPaymentDao;
        this.orderShipmentDao = orderShipmentDao;
        this.settleOrderDetailDao = settleOrderDetailDao;
        this.refundOrderDetailDao = refundOrderDetailDao;
    }

    @Override
    public Response<SettleTrade> findBy(TradeCriteria criteria) {
        try {
            SettleTrade trade = null;
            if (criteria.getPaymentId() != null) {
                trade = buildByPaymentId(criteria.getPaymentId());
            }
            if (criteria.getRefundId() != null) {
                trade = buildByRefundId(criteria.getRefundId());
            }
            return Response.ok(trade);
        } catch (Exception e) {
            log.error("findBy TradeCriteria fail, criteria={}, cause={}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("find.by.trade.criteria.fail");
        }
    }

    @Override
    protected SettleTrade buildByPaymentId(Long paymentId) {
        SettleTrade trade = new SettleTrade();

        //payment
        Payment payment = paymentDao.findById(paymentId);
        if (payment.getPaidAt() == null) { //如果支付单并未成功, 则报错
            log.error("payment status invalid to settle, payment={}", payment);
            throw new ServiceException("payment.status.invalid");
        }

        List<OrderPayment> orderPaymentList = orderPaymentDao.findByPaymentId(paymentId);

        if (payment.getStage() > 0) {//分阶段支付
            trade.setPayType(PayType.StagePay.value());
            OrderPayment orderPayment = orderPaymentList.get(0);
            List<OrderPayment> allStages = orderPaymentDao.findByOrderIdAndOrderType(
                    orderPayment.getOrderId(), orderPayment.getOrderType());
            for (OrderPayment stage : allStages) {
                Payment currentPayment = paymentDao.findById(stage.getPaymentId());
                if (Objects.equals(currentPayment.getStatus(), 1)) { //只有支付成功的订单才返回
                    trade.getPaymentByIdMap().put(stage.getPaymentId(), currentPayment);
                }
            }
        } else {
            if (orderPaymentList.size() > 1) { //合并支付
                trade.setPayType(PayType.MergePay.value());
            } else { //普通支付
                trade.setPayType(PayType.NormalPay.value());
            }
            trade.getPaymentByIdMap().put(paymentId, payment);
        }

        //shopOrder and skuOrder, 支付只能在订单级别.
        for (OrderPayment orderPayment : orderPaymentList) {
            trade.getShopOrderByIdMap().put(orderPayment.getOrderId(), shopOrderDao.findById(orderPayment.getOrderId()));
            trade.getSkuOrderList().addAll(skuOrderDao.findByOrderId(orderPayment.getOrderId()));
        }

        //shipment
        for (ShopOrder shopOrder : trade.getShopOrderByIdMap().values()) {
            val shopOrderShipments = orderShipmentDao.findByOrderIdAndOrderType(shopOrder.getId(), OrderLevel.SHOP.getValue());
            trade.getShopOrderShipmentList().addAll(shopOrderShipments);

            val skuOrderGroups = trade.getSkuOrdersGroupByShopOrderId();
            for (SkuOrder skuOrder : skuOrderGroups.get(shopOrder.getId())) {
                val skuOrderShipments = orderShipmentDao.findByOrderIdAndOrderType(skuOrder.getId(), OrderLevel.SKU.getValue());
                trade.getSkuOrderShipmentList().addAll(skuOrderShipments);
            }
        }

        //shop refund
        for (ShopOrder shopOrder : trade.getShopOrderByIdMap().values()) { //查询是否存在店铺退款
            List<OrderRefund> orderRefunds = orderRefundDao.findByOrderIdAndOrderType(shopOrder.getId(), OrderLevel.SHOP);
            if (orderRefunds.isEmpty()) {
                continue;
            }
            for (OrderRefund orderRefund : orderRefunds) {
                Refund refund = refundDao.findById(orderRefund.getRefundId());
                // 已退款、已退货款才返回数据
                Integer status = refund.getStatus();
                if (!refundCompleteStatus().contains(status)) { //只有退款完成的订单才返回
                    continue;
                }
                RefundEntry refundEntry = new RefundEntry();
                refundEntry.setRefund(refund);
                refundEntry.setPayment(trade.getPaymentByIdMap().get(refund.getPaymentId()));
                refundEntry.setShopOrder(shopOrder);
                if (trade.getPayType().equals(PayType.StagePay.value())) {
                    refundEntry.setRefundType(RefundLevelType.StagePerShopOrder.value());
                } else {
                    refundEntry.setRefundType(RefundLevelType.PerShopOrder.value());
                }
                trade.getRefundByIdMap().put(refund.getId(), refundEntry);
            }
        }

        //sku refund
        List<OrderRefund> allRefunds = new ArrayList<>();
        for (SkuOrder skuOrder : trade.getSkuOrderList()) { //查询是否存在子订单退款
            allRefunds.addAll(orderRefundDao.findByOrderIdAndOrderType(skuOrder.getId(), OrderLevel.SKU));
        }
        Map<Long, List<OrderRefund>> groups = allRefunds.stream().collect(Collectors.groupingBy(OrderRefund::getRefundId));
        for (Long refundId : groups.keySet()) {
            RefundEntry refundEntry = new RefundEntry();

            Refund refund = refundDao.findById(refundId);
            Integer status = refund.getStatus();
            if (!refundCompleteStatus().contains(status)) { //只有退款完成的订单才返回
                continue;
            }
            refundEntry.setRefund(refund);
            refundEntry.setPayment(paymentDao.findById(refund.getPaymentId()));

            List<OrderRefund> orderRefundList = groups.get(refundId).stream().distinct().collect(Collectors.toList());
            if (orderRefundList.size() > 1) { //多个sku退款
                List<SkuOrder> skuOrderList = orderRefundList.stream()
                        .map(x -> skuOrderDao.findById(x.getOrderId()))
                        .collect(Collectors.toList());
                refundEntry.setSkuOrderList(skuOrderList);
                refundEntry.setShopOrder(shopOrderDao.findById(skuOrderList.get(0).getOrderId()));
                refundEntry.setRefundType(RefundLevelType.MultiSkuOrder.value());
            } else { //单个sku退款
                OrderRefund orderRefund = orderRefundList.get(0);
                SkuOrder skuOrder = skuOrderDao.findById(orderRefund.getOrderId());
                refundEntry.setRefundType(RefundLevelType.PerSkuOrder.value());
                refundEntry.setSkuOrder(skuOrder);
                refundEntry.setShopOrder(shopOrderDao.findById(skuOrder.getOrderId()));
            }
            trade.getRefundByIdMap().put(refundId, refundEntry);
        }

        trade.getPaymentByIdMap().values().forEach(this::normalize);
        trade.getRefundByIdMap().values().forEach(x -> normalize(x.getRefund()));
        trade.getShopOrderByIdMap().values().forEach(this::normalize);
        trade.getSkuOrderList().forEach(this::normalize);
        return trade;
    }

    private SettleTrade buildByRefundId(Long refundId) {
        Refund refund = refundDao.findById(refundId);
        return buildByPaymentId(refund.getPaymentId());
    }

    private List<Integer> refundCompleteStatus() {
        return ImmutableList.of(
                VegaOrderStatus.REFUND.getValue(),
                VegaOrderStatus.RETURN_REFUND.getValue()
        );
    }

}
