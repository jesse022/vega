package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.service.PurchaseSkuOrderWriteService;
import com.sanlux.web.front.core.events.trade.VegaOrderCreatedEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 重置采购单 所有商品数量归0并且状态为未选中
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Deprecated
@Component
@Slf4j
public class ResetPurchaseOrderListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private PurchaseSkuOrderWriteService purchaseSkuOrderWriteService;



    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void resetQuantityByPurchaseOrderId(VegaOrderCreatedEvent orderCreatedEvent) {
        Long purchaseOrderId = orderCreatedEvent.getPurchaseOrderId();
        Response<Boolean> updateRes = purchaseSkuOrderWriteService.resetQuantityByPurchaseOrderId(purchaseOrderId);
        if (!updateRes.isSuccess()) {
            log.error("fail to reset quantity by  purchase order Id={} ,error:{}",
                    purchaseOrderId, updateRes.getError());
        }
    }
}
