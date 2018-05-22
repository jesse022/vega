/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.service.VegaOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRelation;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.web.core.events.trade.listener.OrderStatusUpdater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 因为支付, 发货, 退款等的原因, 需要更新对应的(子)订单状态
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-24
 */
@Slf4j
@Component
public class VegaOrderStatusUpdater extends OrderStatusUpdater{
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;
    @Autowired
    private FlowPicker flowPicker;

    @Override
    public void update(List<? extends OrderRelation> orderRelations, OrderOperation orderOperation) {
        List<Long> orderIds = Lists.newArrayListWithCapacity(orderRelations.size());
        for (OrderRelation orderRelation : orderRelations) {
            orderIds.add(orderRelation.getOrderId());
        }

        OrderRelation orderRelation = orderRelations.get(0);

        if (isReverseApply(orderOperation)) {
            Response<Boolean> rsp = orderWriteService.batchMarkHasRefund(orderIds, orderRelation.getOrderLevel(), Boolean.TRUE);
            if (!rsp.isSuccess()) {
                log.error("failed to mark order(ids={}, level={}) has refund, error code:{}", orderIds, orderRelation.getOrderLevel(), rsp.getError());
            }
        }

        if (isCancelReverseApply(orderOperation)) {
            Response<Boolean> rsp = orderWriteService.batchMarkHasRefund(orderIds, orderRelation.getOrderLevel(), Boolean.FALSE);
            if (!rsp.isSuccess()) {
                log.error("failed to mark order(ids={}, level={}) not refund, error code:{}", orderIds, orderRelation.getOrderLevel(), rsp.getError());
            }
        }


        if (Objects.equal(orderRelation.getOrderLevel(), OrderLevel.SHOP)) {
            Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(orderRelation.getOrderId());
            if (!shopOrderResponse.isSuccess()) {
                log.error("failed to find shopOrder(id={}), error code:{}", orderRelation.getOrderId(), shopOrderResponse.getError());
                return;
            }

            ShopOrder shopOrder = shopOrderResponse.getResult();
            Flow flow = flowPicker.pick(shopOrder, OrderLevel.SHOP);
            Integer targetStatus = flow.target(shopOrder.getStatus(), orderOperation);
            Response<List<Long>> listResponse = vegaOrderWriteService.batchShopOrderStatusChanged(orderIds, shopOrder.getStatus(), targetStatus);
            if (!listResponse.isSuccess()) {
                log.error("failed to batch update shopOrders(ids={}) status to {}, error code:{}", orderIds, targetStatus, listResponse.getError());
            }
        } else {
            Response<SkuOrder> skuOrderResponse = this.skuOrderReadService.findById(orderRelation.getOrderId());
            if (!skuOrderResponse.isSuccess()) {
                log.error("failed to find skuOrder(id={}), error code:{}", orderRelation.getOrderId(), skuOrderResponse.getError());
                return;
            }

            SkuOrder skuOrder = skuOrderResponse.getResult();
            Flow flow = flowPicker.pick(skuOrder, OrderLevel.SKU);
            Integer targetStatus = flow.target(skuOrder.getStatus(), orderOperation);
            Response<List<Long>> listResponse = orderWriteService.batchSkuOrderStatusChanged(orderIds, skuOrder.getStatus(), targetStatus);
            if (!listResponse.isSuccess()) {
                log.error("failed to batch update skuOrders(ids={}) status to {}, error code:{}", orderIds, targetStatus, listResponse.getError());
            }
        }
    }

    //是否为逆向申请
    @Override
    protected Boolean isReverseApply(OrderOperation orderOperation){

        return Objects.equal(orderOperation.getValue(), VegaOrderEvent.REFUND_APPLY.getValue())
                || Objects.equal(orderOperation.getValue(), VegaOrderEvent.RETURN_APPLY.getValue());
    }

    //是否是取消逆向流程
    protected Boolean isCancelReverseApply(OrderOperation orderOperation){

        return Objects.equal(orderOperation.getValue(), VegaOrderEvent.REFUND_APPLY_CANCEL.getValue())
                || Objects.equal(orderOperation.getValue(), VegaOrderEvent.RETURN_APPLY_CANCEL.getValue());
    }
}
