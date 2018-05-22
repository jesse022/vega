package com.sanlux.web.front.open.app;

import com.google.common.base.Throwables;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPClientException;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.web.core.component.order.PaymentLogic;
import io.terminus.parana.web.core.order.api.PaymentParamsMaker;
import io.terminus.parana.web.core.order.dto.PayParams;
import io.terminus.parana.web.front.open.dto.OpenPaymentRequest;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.model.PaymentParams;
import io.terminus.pay.model.TradeRequest;
import io.terminus.pay.service.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Author:cp
 * Created on 9/12/16.
 */
@OpenBean
@Slf4j
public class VegaPaymentApi {

    @Autowired
    private  PaymentLogic paymentLogic;
    @Autowired
    private  ChannelRegistry channelRegistry;
    @Autowired
    private  PaymentParamsMaker paymentParamsMaker;


    @OpenMethod(key = "vega.order.pay", paramNames = {"orderIds", "channel", "promotionId", "orderType"}, httpMethods = RequestMethod.POST)
    public OpenPaymentRequest pay(@NotBlank(message = "order.ids.miss") String orderIds,
                                  @NotBlank(message = "channel.miss") String channel,
                                  Long promotionId,
                                  Integer orderType) {
        BaseUser user = UserUtil.getCurrentUser();
        if (user == null) {
            throw new OPClientException("user.not.login");
        }

        PayParams payParams = new PayParams();
        payParams.setOrderIds(transformOrderIds(orderIds));
        payParams.setChannel(channel);
        payParams.setPromotionId(promotionId);
        if (orderType != null) {
            payParams.setOrderType(orderType);
        }

        try {

            OrderLevel orderLevel = OrderLevel.fromInt(payParams.getOrderType());
            Payment payment = paymentLogic.prePay(payParams.getChannel(), payParams.getPromotionId(),
                    payParams.getOrderIds(), orderLevel, VegaOrderEvent.PAY.toOrderOperation(), 0);
            PayChannel paymentChannel = channelRegistry.findChannel(payParams.getChannel());

            PaymentParams params = paymentParamsMaker.makeParams(payment);

            TradeRequest tradeRequest =paymentChannel.paymentRequest(params);
            OpenPaymentRequest request = new OpenPaymentRequest();
            request.setChannel(payParams.getChannel());
            if (tradeRequest.isSuccess()) {
                request.setSuccess(Boolean.TRUE);
                request.setResult(tradeRequest.getResult().getRedirectInfo());
            } else {
                request.setSuccess(Boolean.FALSE);
                request.setError(tradeRequest.getError());
                request.setErrorMessage(tradeRequest.getError());
            }
            return request;
        } catch (Exception e) {
            log.error("fail to pay for order(params:{}),cause:{}",
                    payParams, Throwables.getStackTraceAsString(e));
            if (e instanceof JsonResponseException) {
                throw new OPServerException(e.getMessage());
            }
            throw new OPServerException("order.pay.fail");
        }
    }


    private List<Long> transformOrderIds(String orderIds) {
        try {
            return Splitters.splitToLong(orderIds, Splitters.COMMA);
        } catch (Exception e) {
            log.error("fail to transform {} to orderIds,cause:{}",
                    orderIds, Throwables.getStackTraceAsString(e));
            throw new OPClientException("illegal.order.ids");
        }
    }

}
