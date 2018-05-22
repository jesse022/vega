package com.sanlux.web.front.core.events.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.user.service.UserRankWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.web.core.events.user.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by liangfujie on 16/8/24
 */
@Slf4j
@Component
public class VegaUserRegisterListener {
    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private UserRankWriteService userRankWriteService;

    public VegaUserRegisterListener(){
        super();
    }

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void initUserRank(UserRegisteredEvent userRegisteredEvent) {
        Response<Boolean> response = userRankWriteService.initUserRank(userRegisteredEvent.getUserId());
        if (!response.isSuccess()) {
            log.error("user init fail , userId {} , case {}", userRegisteredEvent.getUserId(), response.getError());
        }
    }

}
