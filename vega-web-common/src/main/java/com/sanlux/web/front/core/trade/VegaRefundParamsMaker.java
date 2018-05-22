package com.sanlux.web.front.core.trade;

import com.sanlux.pay.allinpay.dto.VegaRefundParams;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.web.core.order.api.RefundParamsMaker;
import io.terminus.pay.model.RefundParams;
import lombok.extern.slf4j.Slf4j;

/**
 * DATE: 16/9/9 上午10:40 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class VegaRefundParamsMaker implements RefundParamsMaker {

    @RpcConsumer
    private PaymentReadService paymentReadService;

    @Override
    public RefundParams makeParams(Refund refund) {

        VegaRefundParams refundParams = new VegaRefundParams();

        refundParams.setChannel(refund.getChannel());
        refundParams.setRefundNo(refund.getOutId());
        refundParams.setRefundAmount(refund.getFee());
        refundParams.setRefundReason(refund.getBuyerNote());
        refundParams.setSellerNo(refund.getRefundAccountNo());

        //获取支付单信息
        Response<Payment> paymentR = paymentReadService.findById(refund.getPaymentId());
        if (!paymentR.isSuccess()) {
            log.error("fail to find payment by id {}, error code:{}",
                    refund.getPaymentId(), paymentR.getError());
            throw new JsonResponseException(paymentR.getError());
        }
        Payment payment = paymentR.getResult();

        refundParams.setTradeNo(payment.getOutId()); //内部交易流水号
        refundParams.setPaymentCode(payment.getPaySerialNo()); //外部交易流水号
        refundParams.setTotalFee(payment.getFee());
        refundParams.setPayAt(payment.getCreatedAt());//通联支付：该时间很关键，必须取payment跳往通联网关时的最新时间，即payment记录最新创建时间（如果相同支付渠道点击支付多次，则去最新的点击时间）

        return refundParams;
    }
}
