package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.service.OrderDispatchRelationReadService;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.sms.SmsNodeSwitchParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/13/16
 * Time: 1:42 PM
 */

@Slf4j
@Component
public class TradeSmsListener {


    @Autowired
    private SmsNodeSwitchParser smsNodeSwitchParser;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private UserReadService<User> userUserReadService;

    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }


    @Subscribe
    public void sendSmsSub(TradeSmsEvent tradeSmsEvent) {
        Long orderId = tradeSmsEvent.getOrderId();
        String expressCompanyName = tradeSmsEvent.getExpressCompanyName();
        String shipmentSerialNo = tradeSmsEvent.getShipmentSerialNo();
        TradeSmsNodeEnum nodeEnum = tradeSmsEvent.getNodeEnum();
        sendSms(orderId, expressCompanyName, shipmentSerialNo, nodeEnum);
    }


    private void sendSms(Long shopOrderId, String expressCompanyName, String shipmentSerialNo, TradeSmsNodeEnum nodeEnum){

        if(nodeIsOpen(nodeEnum)){

            Response<ShopOrder> shopOrderResp = shopOrderReadService.findById(shopOrderId);
            if (!shopOrderResp.isSuccess()) {
                log.error("fail to find shop order by id:{},cause:{}", shopOrderId, shopOrderResp.getError());
                return;
            }
            ShopOrder shopOrder = shopOrderResp.getResult();
            Long shopId = shopOrder.getShopId();
            //接单 要发给派单人
            if(nodeEnum.equals(TradeSmsNodeEnum.DISPATCHER_RECEIVE)){

                Response<Optional<OrderDispatchRelation>> relationRes = orderDispatchRelationReadService.findByOrderIdAndDispatchShopId(shopOrderId,shopId);
                if(!relationRes.isSuccess()){
                    log.error("find order dispatch relation fail by order id:{} dispatch shop id:{} error:{}",shopOrderId,shopId,relationRes.getError());
                }
                if(!relationRes.getResult().isPresent()){
                    log.error("not find order dispatch relation fail by order id:{} dispatch shop id:{}",shopOrderId,shopId);
                    return;
                }
                shopId = relationRes.getResult().get().getReceiveShopId();//派单店铺id
            }

            Response<User> shopUserRes = smsNodeSwitchParser.getShopUserByShopId(shopId);
            if (!shopUserRes.isSuccess()) {
                log.error("fail to find shop user by order id:{},cause:{}", shopOrderId, shopUserRes.getError());
                return;
            }

            User shopUser = shopUserRes.getResult();

            Response<User> userRes = smsNodeSwitchParser.getBuyerByOrderId(shopOrder);
            if (!userRes.isSuccess()) {
                log.error("fail to find buyer by order id:{},cause:{}", shopOrderId, userRes.getError());
                return;
            }
            User buyer = userRes.getResult();

            Map<String,String> params = Maps.newHashMap();
            params.put("orderId",shopOrderId.toString());
            // 发货环节
            if(nodeEnum.equals(TradeSmsNodeEnum.SHIPPED)){
                if (Arguments.isNull(expressCompanyName) || Arguments.isNull(shipmentSerialNo)) {
                    log.error("fail to send sms, because expressCompanyName or shipmentSerialNo is null");
                    return;
                }
                params.put("expressCompany",expressCompanyName);
                params.put("shipmentSerialNo",shipmentSerialNo);
            }

            String mobile = makeMessage(nodeEnum,buyer,shopUser,params);

            if(!Strings.isNullOrEmpty(mobile)){
                smsNodeSwitchParser.doSendSms(mobile,nodeEnum.getName(),params);
            }

        }

    }


    private String makeMessage(TradeSmsNodeEnum nodeEnum,User buyer,User shopUser,Map<String,String> params){
       String mobile =null;
        //根据不用的节点封装短信参数
        switch (nodeEnum){
            case CREATE:
                mobile = shopUser.getMobile();
                break;
            case PAID:
                mobile = shopUser.getMobile();
                break;
            case BUYER_CANCEL:
                mobile = shopUser.getMobile();
                break;
            case SELLER_CANCEL:
                mobile = buyer.getMobile();
                break;
            case CHECKED:
                mobile = buyer.getMobile();
                break;
            case REJECT:
                mobile = buyer.getMobile();
                break;
            case DISPATCHER:
                //发给派单人
                mobile = shopUser.getMobile();
                break;
            case DISPATCHER_RECEIVE:
                //发给派单人
                mobile = shopUser.getMobile();
                break;
            case DISPATCHER_REJECT:
                //发给派单人
                mobile = shopUser.getMobile();
                break;
            case APPLY_REFUND:
                mobile = shopUser.getMobile();
                break;
            case AGREE_REFUND:
                mobile = buyer.getMobile();
                break;
            case REJECT_REFUND:
                mobile = buyer.getMobile();
                break;
            case APPLY_RETURN:
                mobile = shopUser.getMobile();
                break;
            case AGREE_RETURN:
                mobile = buyer.getMobile();
                break;
            case BUYER_RETURN:
                mobile = shopUser.getMobile();
                break;
            case SELLER_RECEIVE_RETURN:
                mobile = buyer.getMobile();
                break;
            case REJECT_RETURN:
                mobile = buyer.getMobile();
                break;
            case SHIPPED:
                mobile = buyer.getMobile();
                break;
            case CONFIRMED:
                mobile = shopUser.getMobile();
                break;
            default:
                mobile = shopUser.getMobile();
        }
        return mobile;
    }

    private Boolean nodeIsOpen(TradeSmsNodeEnum nodeEnum){

        Response<Boolean> isOpenRes = smsNodeSwitchParser.tradeIsOpen(nodeEnum);
        if(!isOpenRes.isSuccess()){
            log.error("fail to parse sms node:{} is open,error:{}",nodeEnum.toString(),isOpenRes.getError());
            return false;
        }
        return isOpenRes.getResult();
    }


}
