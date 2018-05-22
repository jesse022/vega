/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.shop;

import com.google.common.eventbus.EventBus;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.web.front.core.events.VegaOrderStorageSyncEvent;
import com.sanlux.web.front.core.store.VegaOrderStorageSync;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/sync")
public class Storages {

    @Autowired
    private VegaOrderStorageSync vegaOrderStorageSync;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private EventBus eventBus;

    /**
     * 手动同步店铺库存
     * @return 结果
     */
    @RequestMapping(value = "/stock", method = RequestMethod.GET)
    public Boolean syncTargetShopStock() {
        ParanaUser currentUser = UserUtil.getCurrentUser();
        Long shopId = currentUser.getShopId();
        Shop shop = shopCacher.findShopById(shopId);
        if (!ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            log.warn("failed to sync stock, shop = {}, cause this shop is not first dealer.");
            throw new JsonResponseException("shop.not.first.dealer");
        }
        //改用事件方式,解决大数据同步时前端等待超时问题
        eventBus.post(VegaOrderStorageSyncEvent.from(shopId,shop.getUserId()));
        return Boolean.TRUE;
    }

}
