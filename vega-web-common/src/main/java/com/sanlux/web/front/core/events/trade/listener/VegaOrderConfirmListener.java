package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.service.VegaOrderWriteService;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRelation;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.web.core.events.trade.OrderConfirmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 监听确认收货事件,更新订单状态
 * Author:cp
 * Created on 6/16/16.
 */
@Slf4j
@Component
public class VegaOrderConfirmListener {

    @Autowired
    private VegaOrderStatusUpdater orderStatusUpdater;

    @Autowired
    private EventBus eventBus;
    @Autowired
    private VegaOrderComponent vegaOrderComponent;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;

    @Autowired
    private FlowPicker flowPicker;


    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onConfirm(OrderConfirmEvent event) {
        OrderRelation orderRelation = new OrderRelation() {
        };
        orderRelation.setOrderId(event.getOrderId());
        orderRelation.setOrderType(event.getOrderType());
        if (orderRelation.getOrderLevel() == OrderLevel.SHOP) {
            Response<ShopOrder> rShopOrder = shopOrderReadService.findById(orderRelation.getOrderId());
            if (!rShopOrder.isSuccess()) {
                log.error("failed to find shopOrder(id={}), error code:{}",
                        orderRelation.getOrderId(), rShopOrder.getError());
                return;
            }
            ShopOrder shopOrder = rShopOrder.getResult();
            Flow flow = flowPicker.pick(shopOrder, OrderLevel.SHOP);
            Integer targetStatus = flow.target(shopOrder.getStatus(), VegaOrderEvent.CONFIRM.toOrderOperation());
            Response<Boolean> response =vegaOrderWriteService.shopOrderStatusChanged(shopOrder.getId(),shopOrder.getStatus(),targetStatus);
            if (!response.isSuccess()) {
                log.error("failed to batch update shopOrders(id={}) status to {}, error code:{}",
                        shopOrder.getId(), targetStatus, response.getError());
            }

        }else {

            orderStatusUpdater.update(Arrays.asList(orderRelation), VegaOrderEvent.CONFIRM.toOrderOperation());
        }
        Response<Long> orderIdRes = vegaOrderComponent.getShopOrderId(event.getOrderId(),event.getOrderType());
        if(!orderIdRes.isSuccess()) {
            log.error("send sms message fail,because shop order id not found by order id:{} order type:{}", event.getOrderId(), event.getOrderType());
            return;
        }
        //短信提醒事件
        eventBus.post(new TradeSmsEvent(orderIdRes.getResult(), TradeSmsNodeEnum.CONFIRMED));
    }


}
