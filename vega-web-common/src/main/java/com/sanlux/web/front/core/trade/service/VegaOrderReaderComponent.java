package com.sanlux.web.front.core.trade.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 16/12/16
 */
@Component
@Slf4j
public class VegaOrderReaderComponent {

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;


    public void groupSkuOrderByShopOrderId(Multimap<Long, SkuOrder> byShopOrderId, List<OrderGroup> orderGroups) {
        List<Long> shopOrderIds = Lists.transform(orderGroups, new Function<OrderGroup, Long>() {
            @Override
            public Long apply(OrderGroup input) {
                return input.getShopOrder().getId();
            }
        });
        Response<List<SkuOrder>> skuOrdersR = skuOrderReadService.findByShopOrderIds(shopOrderIds);
        if (!skuOrdersR.isSuccess()) {
            log.error("fail to find skuOrder by shopOrderIds {}, error code:{}",
                    shopOrderIds, skuOrdersR.getError());
            throw new JsonResponseException(skuOrdersR.getError());
        }
        List<SkuOrder> skuOrders = skuOrdersR.getResult();
        for (SkuOrder skuOrder : skuOrders) {
            byShopOrderId.put(skuOrder.getOrderId(), skuOrder);
        }
    }

}
