/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.settlement.event.VegaOrderAcceptEvent;
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
import io.terminus.parana.settle.service.SettlementWriteService;
import io.terminus.parana.web.core.events.settle.SettleAbnormalEvent;
import io.terminus.parana.web.core.settle.SettleCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 监听确认接单操作, 生成结算单
 *
 * @author : panxin
 */
@Slf4j
public class VegaPaymentSettlementListener {

    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private SettleRichOrderReadService settleRichOrderReadService;
    @RpcConsumer
    private SettlementWriteService settlementWriteService;
    @RpcConsumer
    private CommissionRuleReadService commissionRuleReadService;
    @Autowired
    private SettleCreateService settleCreateService;
    @Autowired
    private SettleFeeDetailCalculator settleFeeDetailCalculator;
    @Autowired
    private PlatformCommissionCalculator platformCommissionCalculator;
    @Autowired
    private OtherCommissionCalculator otherCommissionCalculator;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    public VegaPaymentSettlementListener() {
    }

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onOrderAccept(VegaOrderAcceptEvent orderAcceptEvent) {
        try {
            log.info("handle VegaOrderAcceptEvent={}", orderAcceptEvent);

            Long paymentId = orderAcceptEvent.getPaymentId();
            String channel = orderAcceptEvent.getChannel();

            Long orderId = orderAcceptEvent.getOrderId();
            ShopOrder shopOrder = findShopOrderById(orderId);
            shopOrder.setDiscount(MoreObjects.firstNonNull(shopOrder.getDiscount(), 0));

            SettleTrade settleTrade = findSettleTradeByPaymentId(paymentId);
            for (Payment payment : settleTrade.getPaymentByIdMap().values()) {
                payment.setDiscount(MoreObjects.firstNonNull(payment.getDiscount(), 0));
            }
            for (SkuOrder skuOrder : settleTrade.getSkuOrderList()) {
                skuOrder.setDiffFee(MoreObjects.firstNonNull(skuOrder.getDiffFee(), 0));
            }

            log.info("settle tree1 : {}", settleTrade);
            Map<Long, ShopOrder> shopOrderMap = Maps.newTreeMap();
            shopOrderMap.put(orderId, shopOrder);
            settleTrade.setShopOrderByIdMap(shopOrderMap);

            log.info("settle tree2 : {}", settleTrade);

            // 订单明细以及佣金计算
            SettleFeeDetailTree feeDetailTree = settleFeeDetailCalculator.calculate(settleTrade);
            PlatformCommissionTree platformCommissionTree = platformCommissionCalculator.calculate(settleTrade, feeDetailTree);
            OtherCommissionTree otherCommissionTree = otherCommissionCalculator.calculate(settleTrade, feeDetailTree);

            settleCreateService.createPayChannelDetail(settleTrade.getPaymentByIdMap().get(paymentId));
            settleCreateService.createOrderDetail(new SettleParams(
                    TradeType.Pay, paymentId, settleTrade, feeDetailTree, platformCommissionTree, otherCommissionTree
            ));
            if (channel.contains("mockpay")) {
                settleCreateService.createPaymentSettlement(new SettleParams(
                        TradeType.Pay, paymentId, settleTrade, feeDetailTree, platformCommissionTree, otherCommissionTree
                ));
            }
        } catch (Exception e) {
            log.error("handle PaymentSettleEvent fail, event = {}, cause = {}", orderAcceptEvent, Throwables.getStackTraceAsString(e));
            eventBus.post(new SettleAbnormalEvent(String.valueOf(orderAcceptEvent.getPaymentId()), e.getMessage(), AbnormalType.PAYMENT_TO_SETTLEMENT));
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
