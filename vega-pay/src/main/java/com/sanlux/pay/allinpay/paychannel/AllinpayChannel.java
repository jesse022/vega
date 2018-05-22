package com.sanlux.pay.allinpay.paychannel;

import com.allinpay.ets.client.PaymentResult;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.dto.VegaPaymentParams;
import com.sanlux.pay.allinpay.dto.VegaRefundParams;
import com.sanlux.pay.allinpay.enums.AlinnpayRefundHandleStatus;
import com.sanlux.pay.allinpay.request.*;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.constants.Tokens;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.enums.TradeType;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.*;
import io.terminus.pay.service.PayChannel;
import io.terminus.pay.util.DateUtil;
import io.terminus.pay.util.URLUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.equalWith;
import static io.terminus.pay.util.DateUtil.timeoutMinutes;
import static io.terminus.pay.util.DateUtil.toCompactDateTime;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 4:53 PM
 */
@Slf4j
public class AllinpayChannel<T extends AllinpayToken> implements PayChannel {


    protected final TokenProvider<T> tokenProvider;
    protected final String channel;
    protected final static JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    protected AllinpayChannel(TokenProvider<T> tokenProvider, String channel) {
        this.tokenProvider = tokenProvider;
        this.channel=channel;
    }


    @Override
    public TradeRequest paymentRequest(PaymentParams paymento) {
        if(!Objects.equals(channel, paymento.getChannel())){
            return TradeRequest.fail("pay.channel.mismatch");
        }
        VegaPaymentParams payment =(VegaPaymentParams)paymento;

        AllinpayToken token= tokenProvider.findToken(payment.getSellerNo());
        String notifyUrl= URLUtil.backNofityUrl(token.getNotifyUrl(), payment.getChannel(), payment.getSellerNo());
        String returnUrl= URLUtil.addDomain(token.getReturnUrl(), payment.getDomain());

        switch (channel){
            case AllinpayChannels.PC:
                AllinpayPaymentRequest paymentRequest = AllinpayPaymentRequest.build(token);
                paymentRequest
                        .title(payment.getSubject())
                        .content(payment.getContent())
                        .orderNo(payment.getTradeNo())
                        .total(payment.getFee().intValue())
                        .notify(notifyUrl)
                        .forward(returnUrl, payment.getSystemNo())
                        .orderDatetime(toCompactDateTime(payment.getPayAt()))
                        .payerName(payment.getBuyerNo())
                        .tradeNature("GOODS");
                if(payment.getExpiredAt()!=null){
                    paymentRequest.orderExpireDatetime(timeoutMinutes(payment.getExpiredAt()).intValue());
                }
                paymentRequest.sign();
                return TradeRequest.ok(new Redirect(channel, false, token.getGateway(), paymentRequest.param()));
            case AllinpayChannels.WAP:
                WapAllinpayPaymentRequest wapPaymentRequest = WapAllinpayPaymentRequest.build(token);
                wapPaymentRequest
                        .title(payment.getSubject())
                        .content(payment.getContent())
                        .orderNo(payment.getTradeNo())
                        .total(payment.getFee().intValue())
                        .notify(notifyUrl)
                        .forward(returnUrl, payment.getSystemNo())
                        .orderDatetime(toCompactDateTime(payment.getPayAt()))
                        .payerName(payment.getBuyerNo())
                        .tradeNature("GOODS");
                if(payment.getExpiredAt()!=null){
                    wapPaymentRequest.orderExpireDatetime(timeoutMinutes(payment.getExpiredAt()).intValue());
                }
                wapPaymentRequest.sign();
                return TradeRequest.ok(new Redirect(channel, false, token.getWapPayGateway(), wapPaymentRequest.param()));
            case AllinpayChannels.APP:
                AppAllinpayPaymentRequest appPaymentRequest = AppAllinpayPaymentRequest.build(token);
                appPaymentRequest
                        .title(payment.getSubject())
                        .content(payment.getContent())
                        .orderNo(payment.getTradeNo())
                        .total(payment.getFee().intValue())
                        .notify(notifyUrl)
                        .orderDatetime(toCompactDateTime(payment.getPayAt()))
                        .payerName(payment.getBuyerNo())
                        .tradeNature("GOODS");
                if(payment.getExpiredAt()!=null){
                    appPaymentRequest.orderExpireDatetime(timeoutMinutes(payment.getExpiredAt()).intValue());
                }
                appPaymentRequest.sign();
                return TradeRequest.ok(new Redirect(channel, false, token.getWapPayGateway(), appPaymentRequest.param()));

        }

        return null;

    }


    @Override
    public void verify(HttpServletRequest request) throws PayException {
        log.info("ALLINPAY PAID NOTIFY REQUEST VALUE:{}", request.getParameterMap());
        Map<String, String> params=verifyParams(request);
        AllinpayToken token= tokenProvider.findToken(Tokens.DEFAULT_ACCOUNT);
        PaymentResult paymentResult = JSON_MAPPER.fromJson(JSON_MAPPER.toJson(params), PaymentResult.class);
        //signType为"1"时，必须设置证书路径。
        paymentResult.setCertPath(token.getCertPath());
        //验证签名：返回true代表验签成功；否则验签失败。
        boolean verifyResult = paymentResult.verify();
        if (!verifyResult) {
            throw new PayException("allinpay.sign.invalid");
        }
    }

    @Override
    public TradeResult paymentCallback(HttpServletRequest request) {
        TradeResult result=new TradeResult();
        result.setType(TradeType.PAYMENT.value());
        result.setCallbackResponse("success");
        result.setStatus(TradeStatus.SUCCESS.value());
        result.setMerchantSerialNo(request.getParameter("orderNo"));
        result.setGatewaySerialNo(request.getParameter("paymentOrderId"));

        String paidTime = request.getParameter("payDatetime");
        if(Strings.isNullOrEmpty(paidTime)){
            result.setTradeAt(DateUtil.fromIsoDateTime(paidTime));
        }else{
            result.setTradeAt(new Date());
        }
        result.setChannel(channel);
        return result;
    }

    @Override
    public TradeRequest refundRequest(RefundParams refundParams) {

        try{
            AllinpayToken alipayToken= tokenProvider.findToken(refundParams.getSellerNo());

            VegaRefundParams vegaRefundParams =(VegaRefundParams)refundParams;
            AllinpayRefundRequest refundRequest = AllinpayRefundRequest.build(alipayToken);
            refundRequest
                    .refundAmount(vegaRefundParams.getRefundAmount())
                    .orderDatetime(toCompactDateTime(vegaRefundParams.getPayAt()))//todo 订单提交时间
                     .orderNo(vegaRefundParams.getTradeNo())
                    .refundNo(vegaRefundParams.getRefundNo())
                    .sign();

            Response<Boolean> response = refundRequest.refund();
            if(response.isSuccess()) {
                return TradeRequest.ok(null);
            }else{
                return TradeRequest.fail(response.getError());
            }

        }catch(Exception e){
            log.error("assembly refund request fail, refund={}, cause:{}",refundParams, Throwables.getStackTraceAsString(e));
            return TradeRequest.fail("assembly.refund.request.fail");
        }
    }

    @Override
    public TradeResult refundCallback(HttpServletRequest httpServletRequest) {
        return null;
    }


    public Response<AlinnpayRefundHandleStatus> refundQuery(String tradeNo, Long refundAmount, String refundNo){
        try{
            AllinpayToken alipayToken= tokenProvider.findToken(Tokens.DEFAULT_ACCOUNT);

            AllinpayRefundQueryRequest refundQueryRequest = AllinpayRefundQueryRequest.build(alipayToken);
            refundQueryRequest
                    .refundAmount(refundAmount)
                    .orderNo(tradeNo)
                    .refundNo(refundNo)
                    .sign();

           return refundQueryRequest.refundQuery();

        }catch(Exception e){
            log.error("assembly refund request fail, tradeNo={},refundAmount={},refundNo={}, error:{}",tradeNo,refundAmount,refundNo, e.getMessage());
            return Response.fail("assembly.refund.query.request.fail");
        }
    }


    public Map<String, String> verifyParams(HttpServletRequest request) throws PayException {

        //判断订单状态，为"1"表示支付成功。
        if(!isTradeSucceed(request.getParameter("payResult"))){
            throw new PayException("allinpay.pay.fail");
        }

        if(Strings.isNullOrEmpty(request.getParameter("signMsg"))){
            throw new PayException("allinpay.sign.empty");
        }
        if(Strings.isNullOrEmpty(request.getParameter("signType"))){
            throw new PayException("allinpay.sign.type.empty");
        }

        Map<String, String> params = Maps.newTreeMap();
        for (String key : request.getParameterMap().keySet()) {
            String value = request.getParameter(key);
            if (Strings.isNullOrEmpty(value)
                    || Objects.equals(key, "sign")
                    || Objects.equals(key, "sign_type")) {
                continue;
            }
            params.put(key, value);
        }
        return params;
    }

    private boolean isTradeSucceed(String tradeStatus) {
        return equalWith(tradeStatus, "1");
    }
}
