/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.web.front.core.events.trade.VegaLeaveGodownFinishedEvent;
import com.sanlux.web.front.core.trade.VegaOrderWriteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaLeaveGodownFinishedListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private VegaOrderWriteLogic vegaOrderWriteLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;

    @PostConstruct
    public void init() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onFinished(VegaLeaveGodownFinishedEvent event) {
        List<ShopOrder> orders = event.getAlreadyLeaveOrders();
        orders.stream().forEach(shopOrder -> {
            vegaOrderWriteLogic.updateOrder(shopOrder, OrderLevel.SHOP, VegaOrderEvent.FIRST_DEALER_OUT_OVER);
            Response<Boolean> resp = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, shopOrder.getExtra());
            if (!resp.isSuccess()) {
                log.error("failed to update order extra by orderId = {}, extra = {}, cause : {}" ,
                        shopOrder.getId(), shopOrder.getExtra());
            }
        });
    }

}
