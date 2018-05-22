package com.sanlux.web.front.core.settlement.service;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.OrderPayment;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.settle.api.SettleFeeDetailCalculator;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.model.PayTrans;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.model.Settlement;
import io.terminus.parana.web.core.settle.impl.SettleUpdateServiceImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * DATE: 16/11/13 下午2:59 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
@Component
public class VegaSettleUpdateServiceImpl extends SettleUpdateServiceImpl {

    @RpcConsumer
    private RefundReadService refundReadService;

    @Override
    public void refreshPlatformCommission(Long shopOrderId) throws JsonResponseException {

    }

    @Override
    public void checkSettleDetails(PayTrans payTrans) throws JsonResponseException {
        if (payTrans.getTradeType().equals(TradeType.Pay.value())) {
            checkPayChannelDetail(payTrans);
            // checkPaymentSettlement(payTrans);
            checkOrderDetail(payTrans);
        } else {
            checkPayChannelDetail(payTrans);
            // checkRefundSettlement(payTrans);
            checkRefundOrderDetail(payTrans);
        }
    }

    @Override
    public void checkRefundSettlement(PayTrans payTrans) {
        try {
            log.debug("check settlement for refund, payTrans={}", payTrans);

            Response<Settlement> rSettlement = settlementReadService.findSettlementByRefundNo(payTrans.getRefundNo());
            if (!rSettlement.isSuccess()) {
                log.error("findSettlementByRefundNo fail, refundNo={}, cause={}", payTrans.getRefundNo(), rSettlement.getError());
                return;
            }
            Settlement toUpdate = new Settlement();
            toUpdate.setId(rSettlement.getResult().getId());
            toUpdate.setChannelAccount(payTrans.getAccountNo());
            toUpdate.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
            toUpdate.setCheckFinishedAt(payTrans.getLoadAt());
            toUpdate.setGatewayCommission(payTrans.getCommission());
            val rUpdate = settlementWriteService.updateSettlement(toUpdate);
            if (!rUpdate.isSuccess()) {
                log.error("updateSettlement fail, toUpdate={}, cause={}", toUpdate, rUpdate.getError());
            }

        } catch (Exception e) {
            log.error("check settlement for refund fail, payTrans={}, cause={}", payTrans, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void checkOrderDetail(PayTrans payTrans) {
        try {
            log.debug("check SettleOrderDetail, payTrans={}", payTrans);

            Response<Payment> rPayment = paymentReadService.findByOutId(payTrans.getTradeNo());
            if (!rPayment.isSuccess()) {
                log.error("findPaymentByOutId fail, tradeNo={}, cause={}", payTrans.getTradeNo(), rPayment.getError());
                return;
            }
            Payment payment = rPayment.getResult();

            Response<List<OrderPayment>> rOrderList = paymentReadService.findOrderIdsByPaymentId(payment.getId());
            if (!rOrderList.isSuccess()) {
                log.error("findOrderIdsByPaymentId fail, paymentId={}, cause={}", payment.getId(), rOrderList.getError());
                return;
            }
            for (OrderPayment orderPayment : rOrderList.getResult()) {
                Response<SettleOrderDetail> rOrderDetail = settleOrderDetailReadService.findSettleOrderDetailByOrderId(orderPayment.getOrderId(), orderPayment.getOrderLevel().getValue());
                if (!rOrderDetail.isSuccess()) {
                    log.error("findSettleOrderDetailByOrderId fail, orderId={}, cause={}", orderPayment.getOrderId(), rOrderDetail.getError());
                    continue;
                }
                SettleOrderDetail orderDetail = rOrderDetail.getResult();
                Long splitCommission = SettleFeeDetailCalculator.split(payTrans.getCommission(), payment.getFee(), orderDetail.getActualPayFee());

                SettleOrderDetail toUpdate = new SettleOrderDetail();
                toUpdate.setId(orderDetail.getId());
                toUpdate.setGatewayCommission(splitCommission);
                toUpdate.setChannelAccount(payTrans.getAccountNo());
                toUpdate.setCheckAt(payTrans.getLoadAt());

                Long platformCommission = orderDetail.getPlatformCommission();
                Long commission1 = orderDetail.getCommission1();
                Long sellerReceivableFee = orderDetail.getActualPayFee() + orderDetail.getPlatformDiscount() -
                        (platformCommission + commission1);
                toUpdate.setSellerReceivableFee(sellerReceivableFee);
                toUpdate.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());

                log.info("settle order detail, id = {}, checkStatus = {}, sumAt = {}"
                        , orderDetail.getId(), orderDetail.getCheckStatus(), orderDetail.getSumAt());
                //汇总的时间应该是对账时间的前一天, 这样才能在对账完成后,立马汇总刚对账的订单。
                if (Arguments.isNull(orderDetail.getSumAt())) {
                    Date sumAt = new DateTime(payTrans.getLoadAt()).minusDays(1).toDate();
                    log.info("settle order detail sumAt is null, to set sumAt = {}", sumAt);
                    toUpdate.setSumAt(sumAt);
                }

                val rUpdate = settleOrderDetailWriteService.updateSettleOrderDetail(toUpdate);
                if (!rUpdate.isSuccess()) {
                    log.error("updateSettleOrderDetail fail, toUpdate={}, cause={}", toUpdate, rUpdate.getError());
                }
            }

        } catch (Exception e) {
            log.error("check SettleOrderDetail fail, payTrans={}, cause={}", payTrans, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void checkPayChannelDetail(PayTrans payTrans) throws JsonResponseException {
        try {
            log.debug("check PayChannelDetail, payTrans={}", payTrans);

            if (payTrans.getRefundNo() != null) { //退款对账
                //非通联
                if(!payTrans.getChannel().contains("allinpay")){

                    handleRefundPaychannelDetail(payTrans);

                }else {
                    //通联特殊处理，通联退款账务返回的退款流水号全部和正向的一样
                    //根据正向流水号找到所有的逆向refund
                    Response<List<Refund>> refundRes = refundReadService.findByTradeNo(payTrans.getRefundNo());
                    if(!refundRes.isSuccess()){
                        log.error("find refund by trade no:{} fail,error:{}",payTrans.getRefundNo(),refundRes.getError());
                        return;
                    }
                    if(CollectionUtils.isEmpty(refundRes.getResult())){
                        return;
                    }
                    //这里会存在，假设多条退款单的金额相等的话，则会全部标记位对账成功（只能这样，暂时没有更好的方法，通联的账务信息无法确定到具体的退款单）
                    for (Refund refund : refundRes.getResult()){
                        if(refund.getFee().equals(payTrans.getFee())){
                            payTrans.setRefundNo(refund.getOutId());//塞入真实的退款内部流水号
                            handleRefundPaychannelDetail(payTrans);
                        }
                    }

                }

            } else { //支付对账
                Response<PayChannelDetail> rPayChannelDetail = payChannelDetailReadService.findPayChannelDetailByTradeNo(payTrans.getTradeNo());
                if (!rPayChannelDetail.isSuccess()) {
                    log.error("findPayChannelDetailByTradeNo fail, payTrans={}, cause={}", payTrans, rPayChannelDetail.getError());
                    return;
                }
                PayChannelDetail payChannelDetail = rPayChannelDetail.getResult();
                if (payChannelDetail == null) {
                    log.error("payChannelDetail not exist, payTrans={}", payTrans);
                    return;
                }
                updatePaychannelDetail(payChannelDetail,payTrans);
            }

        } catch (Exception e) {
            log.error("check PayChannelDetail fail, payTrans={}, cause={}",
                    payTrans, Throwables.getStackTraceAsString(e));
        }
    }

    private void updatePaychannelDetail(PayChannelDetail payChannelDetail,PayTrans payTrans){
        PayChannelDetail toUpdate = new PayChannelDetail();
        toUpdate.setId(payChannelDetail.getId());
        toUpdate.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        toUpdate.setCheckFinishedAt(new Date());
        toUpdate.setGatewayCommission(payTrans.getCommission());
        toUpdate.setGatewayRate(payTrans.getRate().intValue());
        toUpdate.setChannelAccount(payChannelDetail.getChannelAccount());
            /*
             * 计算实际收入: tradeFee-gatewayCommission
             */
        toUpdate.setActualIncomeFee(payChannelDetail.getTradeFee() - payTrans.getCommission());

        Response<Boolean> rUpdate = payChannelDetailWriteService.updatePayChannelDetail(toUpdate);
        if (!rUpdate.isSuccess()) {
            log.error("updatePayChannelDetail fail, toUpdate={}, cause={}", toUpdate, rUpdate.getError());
        }
    }


    private void handleRefundPaychannelDetail(PayTrans payTrans){
        Response<PayChannelDetail> rPayChannelDetail = payChannelDetailReadService.findPayChannelDetailByRefundNo(payTrans.getRefundNo());
        if (!rPayChannelDetail.isSuccess()) {
            log.error("findPayChannelDetailByRefundNo fail, payTrans={}, cause={}", payTrans, rPayChannelDetail.getError());
            return;
        }
        PayChannelDetail payChannelDetail = rPayChannelDetail.getResult();

        if (payChannelDetail == null) {
            log.error("payChannelDetail not exist, payTrans={}", payTrans);
            return;
        }
        updatePaychannelDetail(payChannelDetail,payTrans);
    }

    @Override
    public void checkRefundOrderDetail(PayTrans payTrans) {
        try {
            log.debug("check SettleRefundOrderDetail, payTrans={}", payTrans);

            Response<SettleRefundOrderDetail> rRefund = refundOrderDetailReadService.findByRefundNo(payTrans.getRefundNo());
            if (rRefund.isSuccess()) {
                SettleRefundOrderDetail exist = rRefund.getResult();
                if(Arguments.isNull(exist)){
                    log.warn("not find settle refund order detail by refund no:{}",payTrans.getRefundNo());
                    return;
                }

                SettleRefundOrderDetail toUpdate = new SettleRefundOrderDetail();
                toUpdate.setId(rRefund.getResult().getId());
                toUpdate.setChannelAccount(payTrans.getAccountNo());
                toUpdate.setCheckAt(payTrans.getLoadAt());

                toUpdate.setGatewayCommission(payTrans.getCommission());

                Long sellerDeductFee = exist.getActualRefundFee() + exist.getPlatformDiscount() -
                        (exist.getPlatformCommission() + exist.getCommission1());
                log.info("商家应退 = {}, refundId = {}", sellerDeductFee, exist.getRefundId());
                toUpdate.setSellerDeductFee(sellerDeductFee);
                toUpdate.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());

                //汇总的时间应该是对账时间的前一天, 这样才能在对账完成后,立马汇总刚对账的退款单。
                toUpdate.setSumAt(new DateTime(payTrans.getLoadAt()).minusDays(1).toDate());

                refundOrderDetailWriteService.updateSettleRefundOrderDetail(toUpdate);
            }
        } catch (Exception e) {
            log.error("check SettleRefundOrderDetail fail, payTrans={}, cause={}", payTrans, Throwables.getStackTraceAsString(e));
        }
    }
}
