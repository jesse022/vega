package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.web.front.core.events.trade.VegaOrderCreatedEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.component.item.component.ItemSnapshotFactory;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 创建交易快照
 * Author:cp
 * Created on 5/31/16.
 */
@Slf4j
@Component
public class VegaOrderCreationListener {

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @Autowired
    private ItemSnapshotFactory itemSnapshotFactory;


    @Autowired
    private EventBus eventBus;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onCreated(VegaOrderCreatedEvent orderCreatedEvent) {
        Long shopOrderId = orderCreatedEvent.getOrderId();
        List<SkuOrder> skuOrders = findSkuOrders(shopOrderId);

        //创建交易快照
        createTradeSnapshot(skuOrders);

    }

    private List<SkuOrder> findSkuOrders(Long shopOrderId) {
        Response<List<SkuOrder>> skuOrdersResp = skuOrderReadService.findByShopOrderId(shopOrderId);
        if (!skuOrdersResp.isSuccess()) {
            log.error("fail to find sku order by shop order id:{} when create trade snapshot,cause:{}", shopOrderId, skuOrdersResp.getError());
            throw new JsonResponseException(skuOrdersResp.getError());
        }
        return skuOrdersResp.getResult();
    }

    private void createTradeSnapshot(List<SkuOrder> skuOrders) {
        for (SkuOrder skuOrder : skuOrders) {
            Response<Long> getSnapshotIdResp = itemSnapshotFactory.getItemSnapshotId(skuOrder.getItemId());
            if (!getSnapshotIdResp.isSuccess()) {
                log.error("fail to get item(id={}) snapshot id,cause:{}", skuOrder.getItemId(), getSnapshotIdResp.getError());
                continue;
            }
            Long itemSnapshotId = getSnapshotIdResp.getResult();

            Response<Boolean> setResp = orderWriteService.setItemSnapshot(skuOrder.getId(), itemSnapshotId);
            if (!setResp.isSuccess()) {
                log.error("fail to set item snapshot(id={}) for sku order(id={}),cause:{}",
                        itemSnapshotId, skuOrder.getId(), setResp.getError());
            }
        }
    }





}
