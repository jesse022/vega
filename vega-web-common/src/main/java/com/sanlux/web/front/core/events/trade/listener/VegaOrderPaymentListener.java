/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.model.OrderPayment;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.web.core.events.trade.OrderPaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-18
 */
@Slf4j
@Component
public class VegaOrderPaymentListener {

    @RpcConsumer
    private PaymentReadService paymentReadService;

    @Autowired
    private VegaOrderStatusUpdater orderStatusUpdater;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPayment(OrderPaymentEvent orderPaymentEvent) {
        Long paymentId = orderPaymentEvent.getPaymentId();
        Response<Payment> rPayment = paymentReadService.findById(paymentId);
        if(!rPayment.isSuccess()){
            log.error("failed to find Payment(id={}), error code:{}", paymentId, rPayment.getError());
            return;
        }
        final Payment payment = rPayment.getResult();
        if(!Objects.equal(payment.getStatus(), 1)){ //不是支付成功事件, 直接返回吧
            return;
        }
        Response<List<OrderPayment>> rOrderPayments = paymentReadService.findOrderIdsByPaymentId(paymentId);
        if (!rOrderPayments.isSuccess()) {
            log.error("failed to find orderIds for payment(id={}), error code:{}", paymentId, rOrderPayments.getError());
            return;
        }
        List<OrderPayment> orderPayments = rOrderPayments.getResult();

        if (CollectionUtils.isEmpty(orderPayments)) {
            return;
        }
        orderStatusUpdater.update(orderPayments, VegaOrderEvent.PAY.toOrderOperation());

        for (OrderPayment orderPayment : orderPayments){
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderPayment.getOrderId(), TradeSmsNodeEnum.PAID));
        }
    }

}
