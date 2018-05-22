package com.sanlux.web.front.core.settlement.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.parana.settle.service.SettleOrderDetailWriteService;
import io.terminus.parana.web.core.events.trade.OrderConfirmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * DATE: 16/8/10 上午12:26 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class VegaSettleOrderConfirmListener {

    @RpcConsumer
    private SettleOrderDetailWriteService settleOrderDetailWriteService;

    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onConfirm(OrderConfirmEvent event) {
        log.info("handle OrderConfirmEvent = {}, orderId = {}, orderType = {}",
                event, event.getOrderId(), event.getOrderType());

        Long orderId = event.getOrderId();
        Response<SettleOrderDetail> rSettleOrderDetail = settleOrderDetailReadService.findSettleOrderDetailByOrderId(orderId, event.getOrderType());
        if (!rSettleOrderDetail.isSuccess()) {
            log.error("findSettleOrderDetailByOrderId fail, event={}, cause={}", event, rSettleOrderDetail.getError());
            return;
        }

        SettleOrderDetail settleOrderDetail = rSettleOrderDetail.getResult();
        SettleOrderDetail toUpdate = new SettleOrderDetail();
        toUpdate.setId(settleOrderDetail.getId());
        toUpdate.setOrderFinishedAt(new Date());

        log.info("update SettleOrderDetail,original = [{}], toUpdate = [{}].", settleOrderDetail, toUpdate);

        Response<Boolean> rUpdate = settleOrderDetailWriteService.updateSettleOrderDetail(settleOrderDetail);
        if (!rUpdate.isSuccess()) {
            log.error("updateSettleOrderDetail fail, event={}, casue={}", event, rUpdate.getError());
        }
    }
}
