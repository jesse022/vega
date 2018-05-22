/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.service;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.PaymentCriteria;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.settle.api.OtherCommissionCalculator;
import io.terminus.parana.settle.api.PlatformCommissionCalculator;
import io.terminus.parana.settle.api.SettleFeeDetailCalculator;
import io.terminus.parana.settle.dto.OtherCommission;
import io.terminus.parana.settle.dto.OtherCommissionTree;
import io.terminus.parana.settle.dto.PlatformCommissionTree;
import io.terminus.parana.settle.dto.RefundEntry;
import io.terminus.parana.settle.dto.SettleFeeDetail;
import io.terminus.parana.settle.dto.SettleFeeDetailTree;
import io.terminus.parana.settle.dto.SettleParams;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.dto.TradeCriteria;
import io.terminus.parana.settle.enums.AbnormalType;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.model.SettleAbnormalTrack;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.service.PayChannelDetailReadService;
import io.terminus.parana.settle.service.SettleAbnormalTrackWriteService;
import io.terminus.parana.settle.service.SettleRichOrderReadService;
import io.terminus.parana.web.core.settle.impl.SettleCreateServiceImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : panxin
 */
@Slf4j
public class VegaSettleCreateServiceImpl extends SettleCreateServiceImpl {

    @RpcConsumer
    private SettleRichOrderReadService settleRichOrderReadService;
    @RpcConsumer
    private SettleAbnormalTrackWriteService abnormalTrackWriteService;
    @RpcConsumer
    private PaymentReadService paymentReadService;
    @Autowired
    private SettleFeeDetailCalculator settleFeeDetailCalculator;
    @Autowired
    private PlatformCommissionCalculator platformCommissionCalculator;
    @Autowired
    private OtherCommissionCalculator otherCommissionCalculator;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private PayChannelDetailReadService payChannelDetailReadService;

    /**
     * 找到指定时间段内的应进入结算的Payment, 并生成相应的结算基础数据。
     *
     * @param startAt payment.paidAt的最小值
     * @param endAt   payment.paidAt的最大值
     */
    @Override
    public void settlePayments(Date startAt, Date endAt) {
        PaymentCriteria criteria = new PaymentCriteria();
        criteria.setPageSize(300);
        criteria.setPaidStartAt(startAt);
        criteria.setPaidEndAt(endAt);

        while (criteria.hasNext()) {
            val rPayments = paymentReadService.pagingPayments(criteria);
            if (!rPayments.isSuccess()) {
                log.error("settle payment fail, startAt={}, endAt={}, cause={}", startAt, endAt, rPayments.getError());
                return;
            }
            for (Payment payment : rPayments.getResult().getData()) {
                doSettlePayment(payment.getId());
            }
            if (rPayments.getResult().getData().size() == criteria.getPageSize()) {
                criteria.nextPage();
            } else {
                criteria.setHasNext(false);
            }
        }
    }

    @Override
    protected void doSettlePayment(Long paymentId) {
        try {
            Response<SettleTrade> rRichOrder = settleRichOrderReadService.findBy(
                    TradeCriteria.builder().paymentId(paymentId).build());
            if (!rRichOrder.isSuccess()) {
                log.warn("find settle trade by paymentId fail, paymentId={}, cause={}", paymentId, rRichOrder.getError());
                return;
            }
            SettleTrade settleTrade = rRichOrder.getResult();

            // 确认接单之后才进入结算
//            Map<Long, ShopOrder> shopOrderByIdMap = settleTrade.getShopOrderByIdMap();
//            Map<Long, ShopOrder> tempMap = Maps.newConcurrentMap();
//            for (ShopOrder shopOrder : shopOrderByIdMap.values()) {
//                tempMap.put(shopOrder.getId(), shopOrder);
//            }
//            for (ShopOrder shopOrder : tempMap.values()) {
//                if (noSettleOrderStatus().contains(shopOrder.getStatus())) {
//                    tempMap.remove(shopOrder.getId());
//                }
//            }
            // 如果没有可进入结算的订单则不用管
//            if (tempMap.isEmpty()) {
//                log.warn("failed to settle trade by paymentId = {}, cause shopOrders are not accepted by seller.", paymentId);
//                return;
//            }

            // TODO 合并支付的时候怎么计算

            //出现了重复支付的订单
            if (!settleTrade.getDuplicatePaymentList().isEmpty()) {
                SettleAbnormalTrack abnormalTrack = new SettleAbnormalTrack();
                abnormalTrack.setAbnormalInfo(
                        "paymentIds:" +
                                settleTrade.getDuplicatePaymentList().stream()
                                        .map(x -> x.getId().toString()).collect(Collectors.joining(",")));
                abnormalTrack.setDescription("重复支付异常");
                abnormalTrack.setAbnormalType(AbnormalType.PAYMENT_TO_SETTLEMENT.value());
                abnormalTrack.setIsHandle(false);
                abnormalTrackWriteService.createSettleAbnormalTrack(abnormalTrack);
                log.warn("duplicate pay detect when settle payment={}, duplicatePaymentList={}",
                        paymentId, settleTrade.getDuplicatePaymentList());
            }

            SettleFeeDetailTree feeDetailTree = settleFeeDetailCalculator.calculate(settleTrade);
            PlatformCommissionTree platformCommissionTree = platformCommissionCalculator.calculate(settleTrade, feeDetailTree);
            OtherCommissionTree otherCommissionTree = otherCommissionCalculator.calculate(settleTrade, feeDetailTree);

            createPayChannelDetail(settleTrade.getPaymentByIdMap().get(paymentId));
            createPaymentSettlement(new SettleParams(
                    TradeType.Pay, paymentId, settleTrade, feeDetailTree, platformCommissionTree, otherCommissionTree
            ));
            createOrderDetail(new SettleParams(
                    TradeType.Pay, paymentId, settleTrade, feeDetailTree, platformCommissionTree, otherCommissionTree
            ));
        } catch (Exception e) {
            log.error("settle payment fail, paymentId={}, cause={}", paymentId, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void createOrderDetail(SettleParams params) {
        try {
            log.debug("start create settleOrderDetail, params={}", params);

            Long paymentId = params.getPaymentOrRefundId();

            Payment payment = params.getSettleTrade().getPaymentByIdMap().get(paymentId);

            for (ShopOrder shopOrder : params.getSettleTrade().getShopOrderByIdMap().values()) {

                SettleFeeDetail feeDetail = params.getFeeDetailTree().getShopOrderFeeMap().get(shopOrder.getId());
                SettleOrderDetail detail = feeDetail.toSettleOrderDetail();

                //计算平台佣金
                Long platformCommission = params.getPlatformCommissionTree().getShopOrderCommissionMap().get(shopOrder.getId());
                detail.setPlatformCommission(platformCommission);

                //计算其他佣金
                OtherCommission otherCommission = params.getOtherCommissionTree().getShopOrderCommissionMap().get(shopOrder.getId());
                detail.setCommission1(otherCommission.getCommission1());
                detail.setCommission2(otherCommission.getCommission2());
                detail.setCommission3(otherCommission.getCommission3());
                detail.setCommission4(otherCommission.getCommission4());
                detail.setCommission5(otherCommission.getCommission5());

                //在创建时还未对账, 网关佣金暂时计为0
                detail.setGatewayCommission(0L);

                //商家应收
                Long sellerReceivableFee = detail.getActualPayFee() + detail.getPlatformDiscount() -
                        (platformCommission + otherCommission.getCommission1());
                detail.setSellerReceivableFee(sellerReceivableFee);

                detail.setOrderId(shopOrder.getId());
                detail.setSellerId(shopOrder.getShopId());
                detail.setSellerName(shopOrder.getShopName());
                detail.setPaidAt(payment.getPaidAt());
                detail.setChannel(payment.getChannel());
                detail.setChannelAccount(payment.getPayAccountNo());
                detail.setOrderCreatedAt(shopOrder.getCreatedAt());
                detail.setOrderFinishedAt(shopOrder.getUpdatedAt());
                detail.setTradeNo(payment.getOutId());
                detail.setGatewayTradeNo(payment.getPaySerialNo());
                detail.setCheckAt(null);
                detail.setCheckStatus(CheckStatus.WAIT_CHECK.value());

                log.info("createOrderDetail = {}", detail);

                // 查询PayChannelDetail
                Response<PayChannelDetail> respPcd = payChannelDetailReadService.findPayChannelDetailByTradeNo(payment.getOutId());
                if (!respPcd.isSuccess()) {
                    log.error("failed to find PayChannelDetail by tradeNo = {}, cause : {}",
                            payment.getOutId(), respPcd.getError());
                }
                PayChannelDetail channelDetail = respPcd.getResult();
                if (channelDetail != null) {
                    Integer checkStatus = channelDetail.getCheckStatus();

                    // 如果支付明细已对账
                    if (Objects.equal(checkStatus, CheckStatus.CHECK_SUCCESS.value())) {
                        log.info("PayChannelDetail is not null and has checked succeed.");

                        detail.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
                        // 对账日期需要加一天
                        detail.setCheckAt(DateTime.now().plusDays(1).toDate());
                        // 汇总日期为当天
                        detail.setSumAt(DateTime.now().toDate());

                        Long splitCommission = SettleFeeDetailCalculator.split(
                                //payment.getCommission(), payment.getFee(), orderDetail.getActualPayFee()
                                channelDetail.getGatewayCommission(), payment.getFee(), detail.getActualPayFee()
                        );
                        detail.setGatewayCommission(splitCommission);
                        detail.setChannelAccount(channelDetail.getChannelAccount());
                        detail.setSellerReceivableFee(sellerReceivableFee);
                    }
                }

                Response<Long> rCreate = settleOrderDetailWriteService.createSettleOrderDetail(detail);
                if (!rCreate.isSuccess()) {
                    log.error("createSettleOrderDetail fail, toCreate={}, cause={}", detail, rCreate.getError());
                }
            }
        } catch (Exception e) {
            log.error("create settleOrderDetail fail, params={}, cause={}", params, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void createRefundDetail(SettleParams params) {
        try {
            log.debug("start create SettleRefundOrderDetail, params={}", params);
            Long refundId = params.getPaymentOrRefundId();
            RefundEntry refundEntry = params.getSettleTrade().getRefundByIdMap().get(refundId);
            Refund refund = refundEntry.getRefund();
            SettleFeeDetail feeDetail = params.getFeeDetailTree().getRefundFeeMap().get(refundId);

            //开始构建退款单明细
            SettleRefundOrderDetail detail = feeDetail.toSettleRefundOrderDetail();

            //计算平台佣金
            Long platformCommission = params.getPlatformCommissionTree().getRefundCommissionMap().get(refundId);
            detail.setPlatformCommission(platformCommission);

            //计算其他佣金
            OtherCommission otherCommission = params.getOtherCommissionTree().getRefundCommissionMap().get(refundId);
            detail.setCommission1(otherCommission.getCommission1());
            detail.setCommission2(otherCommission.getCommission2());
            detail.setCommission3(otherCommission.getCommission3());
            detail.setCommission4(otherCommission.getCommission4());
            detail.setCommission5(otherCommission.getCommission5());

            //在创建时还未对账, 网关佣金暂时计为0
            detail.setGatewayCommission(0L);

            Long deductFee = detail.getActualRefundFee() + detail.getPlatformDiscount() -
                    (platformCommission + otherCommission.getCommission1());
            log.info("商家应退 = {}, refundId = {}", deductFee, detail.getRefundId());
            //商家应退
            detail.setSellerDeductFee(deductFee);

            detail.setCheckAt(null);
            detail.setRefundAt(refund.getRefundAt());
            detail.setRefundNo(refund.getOutId());
            detail.setGatewayRefundNo(refund.getRefundSerialNo());
            detail.setRefundId(refundId);
            detail.setChannel(refund.getChannel());
            detail.setChannelAccount(refund.getRefundAccountNo());
            detail.setRefundCreatedAt(refund.getCreatedAt());
            detail.setRefundAgreedAt(refund.getRefundAt());
            detail.setCheckStatus(CheckStatus.WAIT_CHECK.value());

            ShopOrder shopOrder = refundEntry.getShopOrder();
            detail.setOrderId(shopOrder.getId());
            detail.setSellerId(shopOrder.getShopId());
            detail.setSellerName(shopOrder.getShopName());

            Response<Long> rCreate = refundOrderDetailWriteService.createSettleRefundOrderDetail(detail);
            if (!rCreate.isSuccess()) {
                log.error("create SettleRefundOrderDetail fail, toCreate={}, cause={}", detail, rCreate.getError());
            }

        } catch (Exception e) {
            log.error("create SettleRefundOrderDetail fail, params={}, cause={}", params, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * <p>
     *     目前有3个地方需要调用到该方法
     * <br>
     *     1. com.sanlux.web.front.core.settlement.service.VegaSettleCreateServiceImpl#doSettlePayment(java.lang.Long)
     * <br>
     *     没用到
     * <br>
     *     2. com.sanlux.web.front.core.settlement.listener.VegaPaymentPayChannelDetailListener#onPaySucceed(com.sanlux.web.front.core.settlement.event.PaymentPayChannelDetailEvent)
     * <br>
     *     支付成功生成支付渠道明细，用于对账
     * <br>
     *     这个时候是肯定会生成支付渠道明细，不需要其他判断
     * <br>
     *     3. com.sanlux.web.front.core.settlement.listener.VegaPaymentSettlementListener#onOrderAccept(com.sanlux.web.front.core.settlement.event.VegaOrderAcceptEvent)
     * <br>
     *     商家接单，生成支付渠道明细、订单明细。这个时候需要判断是否已生成支付渠道明细，
     *     如果是信用额度，因为在支付的时候就默认对账成功，则在接单的时候，可不需要修改什么值，只需要去生成对应的订单明细即可
     * <br>
     *
     * </p>
     * @param payment 支付单信息
     */
    @Override
    public void createPayChannelDetail(Payment payment) {
        try {
            log.debug("start create payChannelDetail for payment={}", payment);

            PayChannelDetail paymentDetail = new PayChannelDetail();
            paymentDetail.setChannel(payment.getChannel());
            paymentDetail.setGatewayCommission(0L);
            paymentDetail.setTradeFee(payment.getFee());
            paymentDetail.setTradeType(TradeType.Pay.value());
            paymentDetail.setTradeNo(payment.getOutId());
            paymentDetail.setGatewayTradeNo(payment.getPaySerialNo()); //可能为空
            paymentDetail.setCheckStatus(CheckStatus.WAIT_CHECK.value());
            paymentDetail.setTradeFinishedAt(payment.getPaidAt());

            // 信用额度默认对账成功
//            if (Objects.equal(payment.getChannel(), CreditPayConstants.PAY_CHANNEL) ||
//                    Objects.equal(payment.getChannel(), CreditPayConstants.WAP_PAY_CHANNEL)) {
//                paymentDetail.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
//            }

            Response<PayChannelDetail> resp = payChannelDetailReadService.findPayChannelDetailByTradeNo(paymentDetail.getTradeNo());
            if (!resp.isSuccess()) {
                log.error("failed to find pay channel detail by tradeNo = {}, cause : {}",
                        paymentDetail.getTradeNo(), resp.getError());
                throw new RuntimeException(resp.getError());
            }

            PayChannelDetail original = resp.getResult();
            createOrUpdatePayChannelDetail(paymentDetail, original);
        }catch (Exception e){
            log.error("create PayChannelDetail fail, params={}, cause={}", payment, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void createPayChannelDetail(Refund refund) {
        try {
            log.debug("start create PayChannelDetail for refund={}", refund);

            PayChannelDetail refundDetail = new PayChannelDetail();
            refundDetail.setChannel(refund.getChannel());
            refundDetail.setGatewayCommission(0L);
            refundDetail.setTradeFee(refund.getFee());
            refundDetail.setTradeType(TradeType.Refund.value());
            refundDetail.setTradeNo(refund.getOutId());
            refundDetail.setGatewayTradeNo(refund.getRefundSerialNo());//可能为空
            refundDetail.setCheckStatus(CheckStatus.WAIT_CHECK.value());
            refundDetail.setTradeFinishedAt(refund.getRefundAt());

            // 信用额度默认对账成功
//            if (Objects.equal(refund.getChannel(), CreditPayConstants.PAY_CHANNEL) ||
//                    Objects.equal(refund.getChannel(), CreditPayConstants.WAP_PAY_CHANNEL)) {
//                refundDetail.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
//            }

            Response<PayChannelDetail> resp = payChannelDetailReadService.findPayChannelDetailByTradeNo(refundDetail.getTradeNo());
            if (!resp.isSuccess()) {
                log.error("failed to find pay channel detail by tradeNo = {}, cause : {}",
                        refundDetail.getTradeNo(), resp.getError());
                throw new RuntimeException(resp.getError());
            }

            PayChannelDetail original = resp.getResult();
            createOrUpdatePayChannelDetail(refundDetail, original);
        }catch (Exception e){
            log.error("createPayChannelDetail fail fail, params={}, cause={}", refund, Throwables.getStackTraceAsString(e));
        }
    }

    private void createOrUpdatePayChannelDetail(PayChannelDetail payChannelDetail, PayChannelDetail original) {
        if (original != null) {
            // 更新
            payChannelDetail.setId(original.getId());
            Response<Boolean> updateResp = payChannelDetailWriteService.updatePayChannelDetail(payChannelDetail);
            if (!updateResp.isSuccess()) {
                log.error("update PayChannelDetail failed, toUpdate = {}, cause = {}", payChannelDetail, updateResp.getError());
            }
        }else {
            // 创建
            Response<Long> rCreate = payChannelDetailWriteService.createPayChannelDetail(payChannelDetail);
            if (!rCreate.isSuccess()) {
                log.error("create PayChannelDetail fail, toCreate={}, cause={}", payChannelDetail, rCreate.getError());
            }
        }
    }

    /**
     * 不进入结算的订单状态
     * 5、18、20、13、14、15 处于接单的状态
     *
     * @return 状态列表
     */
    private List<Integer> noSettleOrderStatus() {
        return ImmutableList.of(
                VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue(),
                VegaOrderStatus.PAID_WAIT_CHECK.getValue(),
                VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),
                VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue(),
                VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),
                VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue(),
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue(),
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue()
        );
    }
}
