/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.web.front.core.events.trade.VegaOrderShipmentEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
@Slf4j
@Component
public class VegaOrderShipmentListener {

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @Autowired
    private VegaOrderStatusUpdater orderStatusUpdater;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onShipment(VegaOrderShipmentEvent orderShipmentEvent) {
        Long shipmentId = orderShipmentEvent.getShipmentId();
        Response<List<OrderShipment>> rOrderShipments = shipmentReadService.findOrderIdsByShipmentId(shipmentId);
        if (!rOrderShipments.isSuccess()) {
            log.error("failed to find orderIds for shipment(id={}), error code:{}", shipmentId, rOrderShipments.getError());
            return;
        }
        List<OrderShipment> orderShipments = rOrderShipments.getResult();

        if (CollectionUtils.isEmpty(orderShipments)) {
            return;
        }
        orderStatusUpdater.update(orderShipments, VegaOrderEvent.SHIP.toOrderOperation());
    }
}
