/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store;

import com.google.common.eventbus.EventBus;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.web.front.core.events.VegaOrderStorageSyncEvent;
import com.sanlux.web.front.core.store.VegaOrderStorageSync;
import com.sanlux.web.front.core.store.VegaStorageLeaveSyncWriter;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
public class VegaAdminStocks {

    @Autowired
    private VegaStorageLeaveSyncWriter vegaStorageLeaveSyncWriter;
    @Autowired
    private VegaOrderStorageSync vegaOrderStorageSync;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private EventBus eventBus;

    /**
     * 手动同步店铺库存
     * @param shopId 店铺ID
     * @return 结果
     */
    @RequestMapping(value = "/stock/{shopId}", method = RequestMethod.GET)
    public Boolean syncTargetShopStock(@PathVariable Long shopId) {
        Shop shop = shopCacher.findShopById(shopId);
        if (!ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            log.warn("failed to sync stock, shop = {}, cause this shop is not first dealer.");
            throw new JsonResponseException("shop.not.first.dealer");
        }
        //改用事件方式,解决大数据同步时前端等待超时问题
        eventBus.post(VegaOrderStorageSyncEvent.from(shopId,shop.getUserId()));
        return Boolean.TRUE;
    }

    /**
     * 手动同步店铺库存
     * @return 结果
     */
    @RequestMapping(value = "/stocks", method = RequestMethod.GET)
    public Boolean syncAllDealer1thStock() {
        vegaOrderStorageSync.syncDealerShopSku();
        return Boolean.TRUE;
    }

    /**
     * 同步出库单
     * @param orderId 订单ID
     * @return 结果
     */
    @RequestMapping(value = "/leave-godown/{orderId}", method = RequestMethod.GET)
    public Boolean syncOrderLeaveGodown(@PathVariable Long orderId) {
        Response<Boolean> resp = vegaStorageLeaveSyncWriter.manualSync(orderId);
        if (!resp.isSuccess()) {
            log.error("failed to manual sync leaveGodown, orderId = {}, cause : {}",
                    orderId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

}
