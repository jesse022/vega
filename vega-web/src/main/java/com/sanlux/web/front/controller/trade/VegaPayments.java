/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.trade;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.sanlux.pay.allinpay.dto.VegaPaymentParams;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.web.front.core.wechat.WxConstants;
import com.sanlux.web.front.core.wechat.WxRequestor;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.utils.UserOpenIdUtil;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.web.core.component.order.PaymentLogic;
import io.terminus.parana.web.core.util.HttpServletUtils;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.constants.Channels;
import io.terminus.pay.constants.RequestParams;
import io.terminus.pay.model.Redirect;
import io.terminus.pay.model.TradeRequest;
import io.terminus.pay.model.TradeResult;
import io.terminus.pay.service.PayChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 支付相关操作
 *
 * Author:  songrenfei
 * Date: 2016-05-13
 */
@RestController
@Slf4j
public class VegaPayments {

    private final ChannelRegistry channelRegistry;

    private final PaymentLogic paymentLogic;

    private final WxRequestor wxRequestor;

    private final EventBus eventBus;

    @Value("${wechat.redirect.url}")
    private  String redirectUrl ;


    @RpcConsumer
    private PaymentReadService paymentReadService;

    @Autowired
    public VegaPayments(ChannelRegistry channelRegistry, PaymentLogic paymentLogic, WxRequestor wxRequestor, EventBus eventBus) {
        this.channelRegistry = channelRegistry;
        this.paymentLogic = paymentLogic;
        this.wxRequestor = wxRequestor;
        this.eventBus = eventBus;
    }

    @Data
    public static class PayParams implements Serializable{

        private static final long serialVersionUID = -4079603486664032735L;

        /**
         * 支付渠道
         */
        @NotNull
        private String channel;
        /**
         * 平台级的营销活动, 可空
         */
        private Long promotionId;
        /**
         * (子)订单id列表
         */
        @NotNull
        private List<Long> orderIds;
        /**
         * 订单级别
         */
        private Integer orderType=1;
        /**
         * 微信code
         */
        private String code;
    }

    /**
     * 首先根据传入的参数, 创建支付单, 并做必要的费用计算以及冻结相应的营销
     * <p
     * 然后根据选中的支付渠道, 生成支付请求url
     *
     * @param payParams 支付参数的dto
     * @return  是否成功
     */
    @RequestMapping(value = "/api/vega/order/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object vegaPay(@RequestBody PayParams payParams,HttpServletRequest req,HttpServletResponse resp) {
        try {

            String redirectUrl = authWechatForJsapi(payParams,req,resp);
            if(!Strings.isNullOrEmpty(redirectUrl)){
                TradeRequest tradeRequest = new TradeRequest();
                Redirect redirect = new Redirect(payParams.getChannel(),Boolean.TRUE,redirectUrl);
                tradeRequest.setResult(redirect);
                return tradeRequest;

            }
            //todo: 判断平台级的优惠是否适用
            OrderLevel orderLevel = OrderLevel.fromInt(payParams.getOrderType());
            Payment payment = paymentLogic.prePay(payParams.getChannel(), payParams.getPromotionId(),
                    payParams.getOrderIds(), orderLevel, VegaOrderEvent.PAY.toOrderOperation(), 1);
            PayChannel paymentChannel = channelRegistry.findChannel(payParams.getChannel());

            VegaPaymentParams result = new VegaPaymentParams();
            result.setChannel(payParams.getChannel());
            result.setSubject(Joiners.COMMA.join(payParams.getOrderIds())); //todo subject
            result.setContent(Joiners.COMMA.join(payParams.getOrderIds())); //todo content
            result.setExpiredAt(DateTime.now().plusDays(1).toDate());
            result.setFee(payment.getFee());
            result.setTradeNo(payment.getOutId()); //交易流水号
            result.setSellerNo(payment.getPayAccountNo());
            result.setSystemNo(payment.getId().toString()); //todo ? 这个用于在前端回调等时使用
            result.setOpenId(UserOpenIdUtil.getOpenId());
            result.setPayAt(payment.getCreatedAt());

            return paymentChannel.paymentRequest(result);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("failed to pay for order(params={}), cause:{}", payParams, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.pay.fail");
        }
    }


    @RequestMapping(value = "/api/vega/order/paid/{channel}/account/{accountNo}", method = RequestMethod.POST, headers = "accept=text/*",produces = MediaType.APPLICATION_JSON_VALUE)
    public String paid(@PathVariable("channel") String channel, @PathVariable("accountNo") String accountNo, HttpServletRequest request) {
        try {
            log.info("allinpay pay request param map: {}", request.getParameterMap());

            PayChannel paymentChannel = channelRegistry.findChannel(channel);

            request.setAttribute(RequestParams.ACCOUNT, accountNo);
            paymentChannel.verify(request);

            TradeResult tradeResult = paymentChannel.paymentCallback(request);
            if (tradeResult.isFail()) {
                log.warn("payment callback result={}", tradeResult);
                return "fail";
            }

            Payment payment = new Payment();
            payment.setPaidAt(tradeResult.getTradeAt());
            payment.setOutId(tradeResult.getMerchantSerialNo());
            payment.setPaySerialNo(tradeResult.getGatewaySerialNo());
            paymentLogic.postPay(payment);

//            Long paymentId = payment.getId();
//            String tradeNo = payment.getOutId();
//            String paymentCode = payment.getPaySerialNo();;
//            Date paidAt = payment.getPaidAt();
//
//            eventBus.post(new PaymentPayChannelDetailEvent(channel, paymentId, tradeNo, paymentCode, paidAt, null));


            return tradeResult.getCallbackResponse();

        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);

            log.error("failed to handle payment callback, request params is:{}, cause is:{}",
                    HttpServletUtils.convertRequestToMap(request), Throwables.getStackTraceAsString(e));
            return "fail";
        }

    }



    /**
     * 支付成功页面查询支付单信息
     * @param data systemNo = payment.id
     * @return 支付单信息
     */
    @RequestMapping(value = "/api/vega/order/payment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Payment findPaymentBySystemNo(@RequestParam("data") String data) {
        if (Strings.isNullOrEmpty(data)) {
            log.error("failed to find payment info by systemNo = ({}), cause systemNo is null.");
            throw new JsonResponseException(500, "systemNo.is.null");
        }
        Response<Payment> resp = paymentReadService.findById(Long.valueOf(data));
        if (!resp.isSuccess()) {
            log.error("failed to find payment info by systemNo(payment.id) = ({}), cause : {}",
                    data, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 根据订单ID, 订单level查询支付单信息
     * @param orderId 订单ID
     * @param orderLevel 订单Level, 默认1 (shop)
     * @return 支付单信息
     */
    @RequestMapping(value = "/api/vega/order/pay_status/{orderId}", method = RequestMethod.GET)
    public Payment findWechatPaymentByOrderNoAndChannel(@PathVariable("orderId") Long orderId,
                                                        @RequestParam(value = "orderLevel", defaultValue = "1") Integer orderLevel) {
        Response<List<Payment>> resp = paymentReadService.findByOrderIdAndOrderLevel(orderId,
                OrderLevel.fromInt(orderLevel));
        if (!resp.isSuccess()) {
            log.error("failed to find payment by orderId = {}, orderLevel = {}, cause : {}",
                    orderId, orderLevel, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        // 查询微信支QR付单信息
        List<Payment> paymentList = resp.getResult();
        for (Payment payment : paymentList) {
            if (isWechatQRpay(payment.getChannel())) {
                return payment;
            }
        }

        log.warn("failed to find WechatQRpay's payment by orderId = {}, orderLevel = {}, cause " +
                "there is no any payment by this way.", orderId, orderLevel);
        throw new JsonResponseException(500, "payment.not.exists");
    }

    /**
     * 微信QR支付渠道
     * @param channel 支付渠道
     * @return 结果
     */
    private Boolean isWechatQRpay(String channel) {
        return Objects.equal(channel, Channels.Wechatpay.QR);
    }


    /**
     * 微信QR支付渠道
     * @param channel 支付渠道
     * @return 结果
     */
    private Boolean isWechatJsapi(String channel) {
        return Objects.equal(channel, Channels.Wechatpay.JSAPI);
    }


    private String authWechatForJsapi(PayParams payParams,HttpServletRequest req,HttpServletResponse resp) throws IOException {

        if(isWechatJsapi(payParams.getChannel())){

            Object openId = req.getSession().getAttribute(WxConstants.OPEN_ID);
            if (openId == null){
                // 未登录
                String code = payParams.getCode();
                if (code == null){
                    // 重定向到微信认证
                    JsonMapper.nonDefaultMapper().toJson(payParams);
                   return wxRequestor.toAuthorize(redirectUrl, resp,JsonMapper.nonDefaultMapper().toJson(payParams));
                } else {
                    // 拿openId
                    Map<String, Object> mapResp = wxRequestor.getOpenId(String.valueOf(code));
                    if (mapResp.get("errcode") != null){
                        log.error("failed to get openid, cause: {}", mapResp);
                    } else {
                        String openIdStr = String.valueOf(mapResp.get(WxConstants.OPEN_ID));
                        req.getSession().setAttribute(WxConstants.OPEN_ID, openIdStr);
                        UserOpenIdUtil.putOpenId(openIdStr);
                    }
                }
            }
            else{
                UserOpenIdUtil.putOpenId(openId.toString());
            }

        }
        return null;

    }




}
