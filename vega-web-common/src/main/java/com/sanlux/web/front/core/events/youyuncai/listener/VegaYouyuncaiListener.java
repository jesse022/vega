package com.sanlux.web.front.core.events.youyuncai.listener;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiEvent;
import com.sanlux.web.front.core.youyuncai.request.VegaYouyuncaiComponent;
import com.sanlux.youyuncai.enums.YouyuncaiApiType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.web.core.events.item.ItemUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by lujm on 2018/2/6.
 */
@Component
@Slf4j
public class VegaYouyuncaiListener {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private VegaYouyuncaiComponent vegaYouyuncaiComponent;

    @RpcConsumer
    private ItemReadService itemReadService;


    @PostConstruct
    public void register() {
        eventBus.register(this);
    }


    @Subscribe
    public void onVegaYouyuncaiEvent(VegaYouyuncaiEvent vegaYouyuncaiEvent) {
        Integer apiType = vegaYouyuncaiEvent.getApiType();

        switch (YouyuncaiApiType.from(apiType)) {
            case ITEM_ALL:
                vegaYouyuncaiComponent.skuSyncByItems(vegaYouyuncaiEvent.getItemIds());
                return;
            case ITEM_ADD:
            case ITEM_UPDATE:
            case ITEM_DELETE:
                vegaYouyuncaiComponent.skuSyncByItem(vegaYouyuncaiEvent.getItemId(), apiType);
                break;
        }
    }

    @Subscribe
    public void onItemUpdateEvent(ItemUpdateEvent itemUpdateEvent) {
        List<Integer> statuses = Lists.newArrayList(
                DefaultItemStatus.ITEM_DELETE,
                DefaultItemStatus.ITEM_FREEZE,
                DefaultItemStatus.ITEM_REFUSE
        );
        Item item = findItems(itemUpdateEvent.getItemId());
        if (Arguments.notNull(item) && statuses.contains(item.getStatus())) {
            // 商品状态修改时触发
            vegaYouyuncaiComponent.skuSyncByItems(ImmutableList.of(
                    itemUpdateEvent.getItemId()
            ));
        }
    }

    private Item findItems(Long itemId) {
        Response<Item> findItem = itemReadService.findById(itemId);
        if (!findItem.isSuccess()) {
            log.error("fail to find item by itemId={},cause:{}",
                    itemId, findItem.getError());
            return null;
        }
        return findItem.getResult();
    }
}
