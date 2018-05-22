/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.web.core.events.trade.OrderRefundEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 订单退款相关listener
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
@Slf4j
@Component
public class VegaOrderRefundListener {

    @RpcConsumer
    private RefundReadService refundReadService;

    @RpcConsumer
    private RefundWriteService refundWriteService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private VegaOrderStatusUpdater orderStatusUpdater;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init(){
        eventBus.register(this);
    }

    @Subscribe
    public void onRefund(OrderRefundEvent orderRefundEvent){
        Long refundId = orderRefundEvent.getRefundId();

        Response<List<OrderRefund>> rOrderRefunds = refundReadService.findOrderIdsByRefundId(refundId);
        if (!rOrderRefunds.isSuccess()) {
            log.error("failed to find orderIds for refund(id={}), error code:{}", refundId, rOrderRefunds.getError());
            return;
        }
        List<OrderRefund> orderRefunds = rOrderRefunds.getResult();

        if (CollectionUtils.isEmpty(orderRefunds)) {
            return;
        }

        Response<Refund> rRefund = refundReadService.findById(refundId);
        if(!rRefund.isSuccess()){
            log.error("failed to find Refund(id={}), error code:{}", refundId, rRefund.getError());
            return;
        }

        isApplyShiped(rRefund.getResult());
        isApplyNotShiped(rRefund.getResult());

        //更新订单信息
        orderStatusUpdater.update(orderRefunds, new OrderOperation(orderRefundEvent.getEventType()));

    }

    /**
     * 已发货 申请退款
     * @param refund 退款单
     */
    private void isApplyShiped(Refund refund){
        if(refund.getStatus().equals(VegaOrderStatus.RETURN_APPLY.getValue())) {
            Map<String,String> map = Maps.newHashMap();
            map.put(SystemConstant.IS_SHIPPED, SystemConstant.SHIPPED);
            Refund update = new Refund();
            update.setId(refund.getId());
            update.setTags(map);


            Response<Boolean> updateRes = refundWriteService.update(update);
            if(!updateRes.isSuccess()){
                log.error("update refund :{} fail,error:{}",update,updateRes.getError());
            }
        }
    }

    /**
     * 未发货 申请退款
     * @param refund
     * @return
     */
    private void isApplyNotShiped(Refund refund) {
        List<Integer> status = Lists.newArrayList();
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_PLATFORM_CHECK_REJECT.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_CHECK_REJECT.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SUPPLIER_SHIPP.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK_REJECT.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_SHIPP_RECEIVE.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_CHECK_REJECT.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_PLATFORM.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_SECOND_DEALER_SHIPP_RECEIVE_FIRST_DEALER.getValue());
        status.add(VegaOrderStatus.REFUND_APPLY_WAIT_FIRST_DEALER_CHECK.getValue());

        if (status.contains(refund.getStatus())) {
            Map<String, String> map = Maps.newHashMap();
            map.put(SystemConstant.IS_SHIPPED, SystemConstant.NOT_SHIPPED);
            Refund update = new Refund();
            update.setId(refund.getId());
            update.setTags(map);


            Response<Boolean> updateRes = refundWriteService.update(update);
            if (!updateRes.isSuccess()) {
                log.error("update refund :{} fail,error:{}", update, updateRes.getError());
            }
        }
    }
}
