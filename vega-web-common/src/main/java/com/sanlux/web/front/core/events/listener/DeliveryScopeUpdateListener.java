package com.sanlux.web.front.core.events.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.user.service.VegaRegionIdsByShopIdCacherService;
import com.sanlux.web.front.core.events.DeliveryScopeUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by cuiwentao
 * on 16/8/22
 */
@Component
@Slf4j
public class DeliveryScopeUpdateListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private VegaRegionIdsByShopIdCacherService vegaRegionIdsByShopIdCacherService;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }


    @Subscribe
    public void onDeliveryScopeUpdate(DeliveryScopeUpdateEvent deliveryScopeUpdateEvent) {

        Response<Boolean> response = vegaRegionIdsByShopIdCacherService.invalidByShopId(deliveryScopeUpdateEvent.getShopId());
        if (!response.isSuccess()) {
            log.error("invalid categoryAuth by shopId: {} failed, cause:{}",
                    deliveryScopeUpdateEvent.getShopId(), response.getError());
        }
    }
}
