/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.service.VegaOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.settle.api.OtherCommissionCalculator;
import io.terminus.parana.settle.api.PlatformCommissionCalculator;
import io.terminus.parana.settle.api.SettleFeeDetailCalculator;
import io.terminus.parana.settle.dto.OtherCommissionTree;
import io.terminus.parana.settle.dto.PlatformCommissionTree;
import io.terminus.parana.settle.dto.SettleFeeDetailTree;
import io.terminus.parana.settle.dto.SettleParams;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.dto.TradeCriteria;
import io.terminus.parana.settle.enums.AbnormalType;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.service.CommissionRuleReadService;
import io.terminus.parana.settle.service.SettleRichOrderReadService;
import io.terminus.parana.web.core.events.settle.PaymentSettleEvent;
import io.terminus.parana.web.core.events.settle.SettleAbnormalEvent;
import io.terminus.parana.web.core.settle.SettleCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * 监听确认接单操作, 生成结算单
 *
 * @author : panxin
 */
@Slf4j
public class VegaPaymentPayChannelDetailListener {

    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private SettleRichOrderReadService settleRichOrderReadService;
    @RpcConsumer
    private CommissionRuleReadService commissionRuleReadService;
    @Autowired
    private SettleCreateService settleCreateService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private SettleFeeDetailCalculator settleFeeDetailCalculator;
    @Autowired
    private PlatformCommissionCalculator platformCommissionCalculator;
    @Autowired
    private OtherCommissionCalculator otherCommissionCalculator;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPaySucceed(PaymentSettleEvent paymentSettleEvent) {
        try {
            log.info("handle vega pay succeed = {}", paymentSettleEvent);

            Long paymentId = paymentSettleEvent.getPaymentId();

            SettleTrade settleTrade = findSettleTradeByPaymentId(paymentId);
            for (Payment payment : settleTrade.getPaymentByIdMap().values()) {
                payment.setDiscount(MoreObjects.firstNonNull(payment.getDiscount(), 0));
            }
            for (SkuOrder skuOrder : settleTrade.getSkuOrderList()) {
                skuOrder.setDiffFee(MoreObjects.firstNonNull(skuOrder.getDiffFee(), 0));
            }

            settleCreateService.createPayChannelDetail(settleTrade.getPaymentByIdMap().get(paymentId));

            String channel = paymentSettleEvent.getChannel();
            if (channel.contains("mockpay")) {
                // 订单明细以及佣金计算
                SettleFeeDetailTree feeDetailTree = settleFeeDetailCalculator.calculate(settleTrade);
                PlatformCommissionTree platformCommissionTree = platformCommissionCalculator.calculate(settleTrade, feeDetailTree);
                OtherCommissionTree otherCommissionTree = otherCommissionCalculator.calculate(settleTrade, feeDetailTree);

                settleCreateService.createPaymentSettlement(new SettleParams(
                        TradeType.Pay, paymentId, settleTrade, feeDetailTree, platformCommissionTree, otherCommissionTree
                ));
            }
        } catch (Exception e) {
            log.error("handle PayChannelDetailEvent fail, event = {}, cause = {}", paymentSettleEvent, Throwables.getStackTraceAsString(e));
            eventBus.post(new SettleAbnormalEvent(String.valueOf(paymentSettleEvent.getPaymentId()), e.getMessage(), AbnormalType.PAYMENT_TO_SETTLEMENT));
        }
    }

    private SettleTrade findSettleTradeByPaymentId(Long paymentId) {
        Response<SettleTrade> resp = settleRichOrderReadService.findBy(
                TradeCriteria.builder().paymentId(paymentId).build()
        );
        if (!resp.isSuccess()) {
            log.error("failed to find settle trade by paymentId = {}, cause : {}", paymentId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    private ShopOrder findShopOrderById(Long orderId) {
        Response<ShopOrder> resp = shopOrderReadService.findById(orderId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop order by orderId = {}, cause : {}", orderId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

}
