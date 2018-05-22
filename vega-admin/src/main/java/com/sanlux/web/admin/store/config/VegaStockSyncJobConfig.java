/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.config;

import com.sanlux.web.admin.store.job.DealerStorageSyncJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author : panxin
 */
public class VegaStockSyncJobConfig {

    private final DealerStorageSyncJob dealerStorageSyncJob;

    @Autowired
    public VegaStockSyncJobConfig(DealerStorageSyncJob dealerStorageSyncJob) {
        this.dealerStorageSyncJob = dealerStorageSyncJob;
    }

    @Scheduled(
            cron = "${first-dealer.stock.sync.cron: 0 */3 * * * * }"
    )
    public void syncDealerStockJob() {
        dealerStorageSyncJob.onSync();
    }

}
