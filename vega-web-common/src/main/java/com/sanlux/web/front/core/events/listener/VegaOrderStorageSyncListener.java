package com.sanlux.web.front.core.events.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.web.front.core.events.VegaOrderStorageSyncEvent;
import com.sanlux.web.front.core.store.VegaOrderStorageSync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;

/**
 * Created by lujm on 2017/5/16.
 */
@Component
@Slf4j
public class VegaOrderStorageSyncListener {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private VegaOrderStorageSync vegaOrderStorageSync;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onVegaOrderStorageSync (VegaOrderStorageSyncEvent event) {
        vegaOrderStorageSync.manualSync(Collections.singletonList(event.getShopId()), ImmutableMap.of(
                event.getShopId(), event.getUserId()
        ));
    }
}
