/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.web.core.component.order.RefundLogic;
import io.terminus.pay.constants.Channels;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.event.RefundCallbackEvent;
import io.terminus.pay.model.TradeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaWeChatRefundCallbackListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private RefundLogic refundLogic;
    @RpcConsumer
    private RefundReadService refundReadService;
    @RpcConsumer
    private ShopReadService shopReadService;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPayCallback(RefundCallbackEvent event) {
        TradeResult tradeResult = event.getTradeResult();
        String channel = tradeResult.getChannel();
        Integer status = tradeResult.getStatus();
        if (!isRefundFailed(status)) {
            return;
        }
        if (!isWeChatQRPay(channel)) {
            return;
        }

        // doRefundCallback(tradeResult);
    }

    /**
     * 退款单操作
     * @param tradeResult 交易结果
     */
    private void doRefundCallback(TradeResult tradeResult) {
        //根据outId查询退款单
        Response<Refund> refundR = refundReadService.findByOutId(tradeResult.getMerchantSerialNo());
        if (!refundR.isSuccess()) {
            log.error("fail to find refund by out id {}, error code:{}, return directly",
                    tradeResult.getMerchantSerialNo(), refundR.getError());
            // TODO something1
            return;
        }
        Refund refund = refundR.getResult();

        Response<Shop> shopRes = shopReadService.findById(refund.getShopId());
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", refund.getShopId(), shopRes.getError());
            // TODO something2
            return;
        }
        OrderOperation orderOperation;
        if(Objects.equal(shopRes.getResult().getType(), VegaShopType.PLATFORM.value())) {
            orderOperation = VegaOrderEvent.REFUND_ADMIN.toOrderOperation();
        }else {
            orderOperation = VegaOrderEvent.REFUND.toOrderOperation();
        }

        String outId = tradeResult.getMerchantSerialNo();
        Date tradeAt = tradeResult.getTradeAt();

        refundLogic.postRefund(outId, tradeAt, orderOperation);
    }

    /**
     * 交易失败
     * @param status 状态
     * @return bool
     */
    private boolean isRefundFailed(Integer status) {
        return Objects.equal(status, TradeStatus.FAIL.value());
    }

    /**
     * 暂时只有微信QR支付退款时需要单独处理
     * @param channel 支付渠道
     * @return bool
     */
    private boolean isWeChatQRPay(String channel) {
        return Objects.equal(channel, Channels.Wechatpay.QR);
    }

}
