package com.sanlux.web.front.core.trade;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.service.OrderDispatchRelationReadService;
import com.sanlux.trade.service.OrderDispatchRelationWriteService;
import com.sanlux.trade.service.VegaOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Mail: F@terminus.io
 * Data: 16/7/19
 * Author: yangzefeng
 */
@Component
@Slf4j
public class VegaOrderWriteLogic {

    @Autowired
    private FlowPicker flowPicker;

    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;
    @RpcConsumer
    private OrderDispatchRelationWriteService orderDispatchRelationWriteService;
    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;
    @RpcConsumer
    private ShopReadService shopReadService;

    @Autowired
    private EventBus eventBus;


    public boolean dispatchOrder(OrderBase orderBase, OrderLevel orderLevel, VegaOrderEvent orderEvent,Shop shop,ParanaUser paranaUser){
        Flow flow = flowPicker.pick(orderBase, orderLevel);
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());

        Response<Boolean> response = orderDispatchRelationWriteService.dispatchOrder(orderBase, shop, paranaUser,targetStatus);
        if(!response.isSuccess()){
            log.error("create order dispatch for shop order id:{} shop id:{} fail,error:{}",orderBase.getId(),shop.getId());
            throw new JsonResponseException(response.getError());
        }

        return Boolean.TRUE;
    }

    public boolean updateOrder(OrderBase orderBase, OrderLevel orderLevel, VegaOrderEvent orderEvent) {
        Flow flow = flowPicker.pick(orderBase, orderLevel);
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());
        // todo 事件--->三力士订单取消不需要做库存回滚

    /*  if (Objects.equals(orderEvent.getValue(), VegaOrderEvent.BUYER_CANCEL.getValue())
                ||Objects.equals(orderEvent.getValue(), VegaOrderEvent.DEALER_CANCEL.getValue())
                || Objects.equals(orderEvent.getValue(), VegaOrderEvent.PLATFORM_CANCEL.getValue())) {
            eventBus.post(new OrderCancelEvent(orderBase.getId(), orderLevel.getValue(), orderEvent));
        }*/

        switch (orderLevel) {
            case SHOP:

                if(isDealerReject(orderEvent)){

                    Long orderId = orderBase.getId();
                    Long dispatchShopId = orderBase.getShopId();
                    OrderDispatchRelation relation = getOrderDispatchRelationByOrderIdAndDispatchShopId(orderId,dispatchShopId);
                    Shop shop = getShopById(relation.getReceiveShopId());

                    Response<Boolean> updateShopOrderForRejectRes = vegaOrderWriteService.shopOrderStatusChangedForDealerReject(
                            orderId,relation.getId(),shop.getId(),shop.getName(),orderBase.getStatus(),targetStatus);
                    if (!updateShopOrderForRejectRes.isSuccess()) {
                        log.error("fail to update shop order(id={}) from current status:{} to target:{},cause:{}",
                                orderBase.getId(), orderBase.getStatus(), targetStatus, updateShopOrderForRejectRes.getError());
                        throw new JsonResponseException(updateShopOrderForRejectRes.getError());
                    }
                    return updateShopOrderForRejectRes.getResult();
                }else {

                    Response<Boolean> updateShopOrderResp = orderWriteService.shopOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                    if (!updateShopOrderResp.isSuccess()) {
                        log.error("fail to update shop order(id={}) from current status:{} to target:{},cause:{}",
                                orderBase.getId(), orderBase.getStatus(), targetStatus, updateShopOrderResp.getError());
                        throw new JsonResponseException(updateShopOrderResp.getError());
                    }
                    return updateShopOrderResp.getResult();
                }
            case SKU:
                Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateSkuOrderResp.isSuccess()) {
                    log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus);
                    throw new JsonResponseException(updateSkuOrderResp.getError());
                }
                return updateSkuOrderResp.getResult();
            default:
                throw new IllegalArgumentException("unknown.order.type");
        }
    }

    private Boolean isDealerReject(VegaOrderEvent orderEvent){
        if(VegaOrderEvent.FIRST_DEALER_REJECT_RECEIVE.equals(orderEvent)||VegaOrderEvent.SECOND_DEALER_REJECT_RECEIVE.equals(orderEvent)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Shop getShopById(Long shopId){
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if(!shopRes.isSuccess()){
            log.error("find shop by id:{} fail,error:{}",shopId,shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }

    private OrderDispatchRelation getOrderDispatchRelationByOrderIdAndDispatchShopId(Long orderId,Long dispatchShopId){

        Response<Optional<OrderDispatchRelation>> relationRes = orderDispatchRelationReadService.findByOrderIdAndDispatchShopId(orderId,dispatchShopId);
        if(!relationRes.isSuccess()){
            throw new JsonResponseException(relationRes.getError());
        }
        Optional<OrderDispatchRelation> relationOptional = relationRes.getResult();
        if(!relationOptional.isPresent()){
            throw new JsonResponseException("order.dispatch.relation.not.exist");
        }

         return relationOptional.get();
    }

}
