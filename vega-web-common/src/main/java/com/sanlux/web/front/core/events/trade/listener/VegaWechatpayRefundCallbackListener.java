package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.event.RefundCallbackEvent;
import io.terminus.pay.model.TradeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * DATE: 16/11/8 下午9:28 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
@Component
public class VegaWechatpayRefundCallbackListener {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private RefundReadService refundReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @Autowired
    private CommonRefundLogic refundLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init(){
        eventBus.register(this);
    }

    @Subscribe
    public void onRefundCallback(RefundCallbackEvent refundCallbackEvent){
        log.info("handle RefundCallbackEvent={}", refundCallbackEvent);

        TradeResult result = refundCallbackEvent.getTradeResult();
        if(! result.getChannel().contains("wechatpay")){
            log.info("not wechatpay refund callback, just return...");
            return;
        }
        if(!result.getStatus().equals(TradeStatus.SUCCESS.value())){
            log.info("refund status not success, just return...");
            return;
        }

        //根据outId查询退款单
        Response<Refund> refundR = refundReadService.findByOutId(result.getMerchantSerialNo());
        if (!refundR.isSuccess()) {
            log.error("fail to find refund by out id {}, error code:{}, return directly",
                    result.getMerchantSerialNo(), refundR.getError());
            return;
        }
        Refund refund = refundR.getResult();

        Response<Shop> shopRes = shopReadService.findById(refund.getShopId());
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", refund.getShopId(), shopRes.getError());
            return;
        }
        OrderOperation orderOperation;
        if(Objects.equal(shopRes.getResult().getType(), VegaShopType.PLATFORM.value())) {
            orderOperation = VegaOrderEvent.REFUND_ADMIN.toOrderOperation();
        }else {
            orderOperation = VegaOrderEvent.REFUND.toOrderOperation();

        }

        Date refundAt = result.getTradeAt() == null ? new Date() : result.getTradeAt();
        refundLogic.postRefund(result.getMerchantSerialNo(), refundAt, orderOperation);
    }
}
