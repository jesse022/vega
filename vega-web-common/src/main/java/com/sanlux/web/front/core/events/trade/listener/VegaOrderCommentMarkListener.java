package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderCommentFlag;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.web.core.events.trade.OrderConfirmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单确认监听事件,主要目的是标记子订单可以评价
 * Mail: F@terminus.io
 * Data: 16/7/7
 * Author: yangzefeng
 */
@Slf4j
@Component
public class VegaOrderCommentMarkListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void batchMarkComment(OrderConfirmEvent event) {
        Long orderId = event.getOrderId();
        Integer orderType = event.getOrderType();



        List<SkuOrder> skuOrders = new ArrayList<>();
        if (Objects.equal(orderType, OrderLevel.SHOP.getValue())) {
            ShopOrder shopOrder =  getShopOrderById(orderId);
            Response<List<SkuOrder>> skuOrderR = skuOrderReadService.findByShopOrderId(orderId);
            if (!skuOrderR.isSuccess()) {
                log.error("fail to find sku order by shopOrderId {}, error code:{}",
                        orderId, skuOrderR.getError());
                return;
            }
            for (SkuOrder skuOrder : skuOrderR.getResult()){
                if(Objects.equal(skuOrder.getStatus(),shopOrder.getStatus())){
                    skuOrders.add(skuOrder);
                }
            }
        } else {
            Response<SkuOrder> skuOrderR = skuOrderReadService.findById(orderId);
            if (!skuOrderR.isSuccess()) {
                log.error("fail to find sku order by id {}, error code:{}",
                        orderId, skuOrderR.getError());
                return;
            }
            skuOrders.add(skuOrderR.getResult());
        }

        List<Long> skuOrderIds = new ArrayList<>();
        for (SkuOrder skuOrder : skuOrders) {
            skuOrderIds.add(skuOrder.getId());
        }
        Response<Boolean> updateR = orderWriteService.batchMarkCommented(skuOrderIds, OrderCommentFlag.CAN_COMMENT.getValue());
        if (!updateR.isSuccess()) {
            log.error("fail to batch mark commented by skuOrderIds {}, commentFlag {}, error code:{}",
                    skuOrderIds, 1, updateR.getError());
        }




    }



    private ShopOrder getShopOrderById(Long shopOrderId){
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(shopOrderId);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",shopOrderId,shopOrderRes.getError());

            throw new JsonResponseException(shopOrderRes.getError());
        }

        return shopOrderRes.getResult();
    }
}
