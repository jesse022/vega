/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.pay.credit.paychannel;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.pay.credit.dto.CreditPayRefundData;
import com.sanlux.pay.credit.request.*;
import com.thoughtworks.xstream.XStream;
import io.terminus.common.model.Response;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.constants.Tokens;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.enums.TradeType;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.PaymentParams;
import io.terminus.pay.model.Redirect;
import io.terminus.pay.model.RefundParams;
import io.terminus.pay.model.TradeRequest;
import io.terminus.pay.model.TradeResult;
import io.terminus.pay.service.PayChannel;
import io.terminus.pay.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static io.terminus.common.utils.Arguments.isEmpty;
import static io.terminus.common.utils.Arguments.notEmpty;
import static org.joda.time.Minutes.minutesBetween;

/**
 * 信用额度支付渠道
 *
 * @author : panxin
 */
@Slf4j
public class CreditPaymentChannel implements PayChannel {

    private final TokenProvider<CreditPayToken> tokenProvider;
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private static XStream xStream;
    static {
        xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(CreditPaySyncResponse.class);
    }

    @Autowired
    public CreditPaymentChannel(TokenProvider<CreditPayToken> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public TradeRequest paymentRequest(PaymentParams payment) {

        CreditPayToken creditPayToken= tokenProvider.findToken(payment.getSellerNo());

        try {
            String notifyUrl= URLUtil.backNofityUrl(creditPayToken.getNotifyUrl(), payment.getChannel(),
                    payment.getSellerNo());
            if (Objects.equal(payment.getChannel(), CreditPayConstants.WAP_PAY_CHANNEL) ||
                    Objects.equal(payment.getChannel(), CreditPayConstants.MEMBER_WAP_PAY_CHANNEL)) {
                String returnUrl = creditPayToken.getWapReturnUrl();

                CreditPayRequest creditPayRequest = CreditPayRequest
                        .build(creditPayToken)
                        .title(payment.getSubject())
                        .content(payment.getContent())
                        .outerTradeNo(payment.getTradeNo())
                        .total(payment.getFee().intValue())
                        .notify(notifyUrl)
                        .sellerNo(payment.getSellerNo())
                        .timeoutM(minutesBetween(new DateTime(payment.getExpiredAt()), DateTime.now()).getMinutes() - 30)
                        .forward(returnUrl, payment.getSystemNo());

                // log.info("redirect url = {}", creditPayRequest.url());
                return TradeRequest.ok(new Redirect(payment.getChannel(), true, creditPayRequest.url()));
            }else {
                String returnUrl = creditPayToken.getReturnUrl();

                CreditPayRequest creditPayRequest = CreditPayRequest
                        .build(creditPayToken)
                        .title(payment.getSubject())
                        .content(payment.getContent())
                        .outerTradeNo(payment.getTradeNo())
                        .total(payment.getFee().intValue())
                        .notify(notifyUrl)
                        .sellerNo(payment.getSellerNo())
                        .timeoutM(minutesBetween(new DateTime(payment.getExpiredAt()), DateTime.now()).getMinutes() - 30)
                        .forward(returnUrl, payment.getSystemNo());

                // log.info("redirect url = {}", creditPayRequest.url());
                return TradeRequest.ok(new Redirect(payment.getChannel(), true, creditPayRequest.url()));
            }
        }catch (PayException e){
            log.error("assembly pay request fail,error:{}",e.getMessage());
            return TradeRequest.fail(e.getMessage());
        }catch (Exception e){
            log.error("assembly pay request fail,cause:{}", Throwables.getStackTraceAsString(e));
            return TradeRequest.fail("assembly.pay.request.fail");
        }
    }

    @Override
    public TradeRequest refundRequest(RefundParams refund) {
        try {
            CreditPayToken creditPayToken= tokenProvider.findToken(refund.getSellerNo());

            String notifyUrl = URLUtil.backNofityUrl(creditPayToken.getRefundNotifyUrl(), refund.getChannel(),
                    refund.getSellerNo());

            List<CreditPayRefundData> refundDetails = Lists.newArrayList();
            refundDetails.add(new CreditPayRefundData(refund.getPaymentCode(), refund.getRefundAmount().intValue(),
                    isNullOrEmpty(refund.getRefundReason()) ? "退款" : refund.getRefundReason()));

            String url = CreditRefundRequest
                    .buildWithNoPwd(creditPayToken)
                    .batch(refund.getRefundNo())
                    .notify(notifyUrl)
                    .sellerNo(refund.getSellerNo())
                    .detail(refundDetails).refundUrl();

            log.info("refund url: {}", url);

            String body = HttpRequest.get(url).connectTimeout(10000).readTimeout(10000).body();

            log.info("refund result: {}", body);

            Response<Boolean> resp = convertToResponse(body);

            if(resp.isSuccess()) {
                // return TradeRequest.ok(null);
                return TradeRequest.ok(new Redirect(refund.getChannel(), true, url));
            }else{
                return TradeRequest.fail(resp.getError());
            }
        }catch (PayException e){
            log.error("assembly refund request fail, error:{}",e.getMessage());
            return TradeRequest.fail(e.getMessage());

        }catch (Exception e){
            log.error("assembly refund request fail, error:{}",e.getMessage());
            return TradeRequest.fail("assembly.refund.request.fail");
        }

    }

    private Response<Boolean> convertToResponse(String body) {
        Response<Boolean> result = new Response<Boolean>();
        try {
            checkState(!Strings.isNullOrEmpty(body), "creditPay.refund.fail");
            CreditPaySyncResponse refundResponse = (CreditPaySyncResponse)xStream.fromXML(body);
            if (refundResponse.isSuccess()) {
                result.setResult(Boolean.TRUE);
            } else {
                log.error("refund raise fail: {}", refundResponse.getError());
                result.setError(refundResponse.getError());
            }
        }catch (IllegalStateException e){
            log.error("creditPay refund request fail,error: {}", e.getMessage());
            result.setError("creditPay.refund.fail");
        }catch (Exception e){
            log.error("creditPay refund request fail,cause: {}", Throwables.getStackTraceAsString(e));
            result.setError("creditPay.refund.fail");
        }
        return result;
    }

    @Override
    public TradeResult refundCallback(HttpServletRequest request) {
        try{
            String batchNo = request.getParameter("batch_no");
            //String refundDetail = request.getParameter("detail_data");
            //List<String> details = Splitter.on("#").splitToList(refundDetail);
            log.info("\nrefund info : \n" +
                    "batch_no = [{}]\n" + batchNo);

//            checkState(details.size() == 1 || details.size() == 2, "creditPay.refund.detail.num.incorrect");
//            String detail = details.get(0);
            // 详细信息中包含 原付款支付宝交易号^退款总金额^退款状态
//            List<String> fields =Splitter.on("#").splitToList(detail);
//            checkState(fields.size() >= 3, "creditPay.refund.detail.field.num.incorrect");
//            String result = fields.get(2);       // 获取处理结果
//            checkState(StringUtils.equalsIgnoreCase(result, "SUCCESS"), "creditPay.refund.fail");


            TradeResult result = new TradeResult();
            result.setChannel(CreditPayConstants.PAY_CHANNEL); //// TODO: 9/13/16
            result.setType(TradeType.REFUND.value());
            result.setTradeAt(new Date());
            result.setMerchantSerialNo(batchNo);
            result.setStatus(TradeStatus.SUCCESS.value());
            result.setCallbackResponse("success");

            return result;

        }catch (Exception e){
           return TradeResult.fail(TradeType.REFUND, "channel", "credit.pay.refund.fail");
        }
    }

    @Override
    public void verify(HttpServletRequest request) throws PayException {
        try {

            checkArgument(notEmpty(request.getParameter("sign")), "sign.empty");
            checkArgument(notEmpty(request.getParameter("sign_type")), "sign.type.empty");

            String sign = request.getParameter("sign");
            Map<String, String> params = Maps.newTreeMap();
            for (String key : request.getParameterMap().keySet()) {
                String value = request.getParameter(key);
                if (isValueEmptyOrSignRelatedKey(key, value)) {
                    continue;
                }
                params.put(key, value);
            }

            /**
             * 获取token中的key, todo 多账号时怎么构造相应的TokenId
             */
            CreditPayToken creditPayToken = tokenProvider.findToken(Tokens.DEFAULT_ACCOUNT);
            boolean valid = Request.verify(params, sign, creditPayToken);
            if (!valid) {
                throw new PayException("creditPay.pay.sign.invalid");
            }
        }catch (PayException e){
            log.error("verify credit pay fail,error:{}",e.getMessage());
            throw new PayException(e.getMessage());
        }catch (Exception e){
            log.error("verify credit pay fail,cause:{}",Throwables.getStackTraceAsString(e));
            throw new PayException("creditPay.verify.fail");
        }
    }

    @Override
    public TradeResult paymentCallback(HttpServletRequest request) {

        TradeResult result = new TradeResult();

        result.setChannel("");
        result.setType(TradeType.PAYMENT.value());
        result.setCallbackResponse("success");
        result.setMerchantSerialNo(request.getParameter("out_trade_no"));
        result.setGatewaySerialNo(request.getParameter("trade_no"));

        String paidTime = request.getParameter("gmt_payment");
        if(Strings.isNullOrEmpty(paidTime)){
            result.setTradeAt( DFT.parseDateTime(paidTime).toDate());
        }else{
            result.setTradeAt(new Date());
        }

        return result;
    }

    /**
     * 去除多余值
     * @param key key
     * @param value value
     * @return boolean
     */
    private boolean isValueEmptyOrSignRelatedKey(String key, String value) {
        return isEmpty(value) || StringUtils.equalsIgnoreCase(key, "sign")
                || StringUtils.equalsIgnoreCase(key, "sign_type");
    }

}
