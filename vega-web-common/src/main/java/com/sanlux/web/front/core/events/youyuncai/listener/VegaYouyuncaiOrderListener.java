package com.sanlux.web.front.core.events.youyuncai.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiOrderEvent;
import com.sanlux.web.front.core.youyuncai.request.VegaYouyuncaiComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 友云采订单
 * Created by lujm on 2018/3/12.
 */
@Component
@Slf4j
public class VegaYouyuncaiOrderListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private VegaYouyuncaiComponent vegaYouyuncaiComponent;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }


    /**
     * 友云采出货通知接口
     */
    @Subscribe
    public void onVegaYouyuncaiOrderShipInfo(VegaYouyuncaiOrderEvent vegaYouyuncaiOrderEvent) {
        vegaYouyuncaiComponent.shipInfo(vegaYouyuncaiOrderEvent.getOrderId(),
                vegaYouyuncaiOrderEvent.getShipmentCompanyName(),
                vegaYouyuncaiOrderEvent.getShipmentSerialNo());
    }





}
