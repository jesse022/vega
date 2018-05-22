/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.settle.enums.AbnormalType;
import io.terminus.parana.web.core.events.settle.RefundSettleEvent;
import io.terminus.parana.web.core.events.settle.SettleAbnormalEvent;
import io.terminus.parana.web.core.settle.SettleCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * @author : panxin
 */
@Slf4j
public class VegaRefundPayChannelDetailListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private SettleCreateService settleCreateService;
    @RpcConsumer
    private RefundReadService refundReadService;

    @PostConstruct
    public void init() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onRefund(RefundSettleEvent event) {
        try {
            log.info("handle refund settle event: {}", event);
            Response<Refund> resp = refundReadService.findById(event.getRefundId());
            if (!resp.isSuccess()) {
                log.error("failed to find refund by id = {}, cause : {}", event.getRefundId(), resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Refund refund = resp.getResult();
            settleCreateService.createPayChannelDetail(refund);
        }catch (Exception e) {
            log.error("failed to create pay channel detail by refundId = {}, cause : {}",
                    event.getRefundId(), Throwables.getStackTraceAsString(e));
            eventBus.post(new SettleAbnormalEvent(String.valueOf(event.getRefundId()),
                    "refund pay channel detail create failed, cause :" + e.getMessage(),
                    AbnormalType.REFUND_TO_SETTLEMENT));
        }
    }

}
