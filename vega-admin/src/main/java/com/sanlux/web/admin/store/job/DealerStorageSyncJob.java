/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.job;

import com.sanlux.web.front.core.store.VegaOrderStorageSync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 经销商库存同步
 *
 * @author : panxin
 */
@Slf4j
public class DealerStorageSyncJob {

    @Autowired
    private VegaOrderStorageSync vegaOrderStorageSync;

    public void onSync() {
        log.info("同步一级经销商库存");
        vegaOrderStorageSync.syncDealerShopSku();
    }

}
