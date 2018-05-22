/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.config;

import com.sanlux.web.admin.store.job.BuyerOrderStorageOutSyncJob;
import com.sanlux.web.admin.store.job.BuyerRefundStorageEntrySyncJob;
import com.sanlux.web.admin.store.job.DealerOrderStorageOutSyncJob;
import com.sanlux.web.admin.store.job.DealerRefundStorageEntrySyncJob;
import com.sanlux.web.admin.store.job.FirstDealerOrderEntrySyncJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author : panxin
 */
public class VegaStorageJobConfig {

    private final DealerOrderStorageOutSyncJob dealerOrderStorageOutSyncJob;
    private final BuyerOrderStorageOutSyncJob buyerOrderStorageOutSyncJob;
    private final DealerRefundStorageEntrySyncJob dealerRefundStorageEntrySyncJob;
    private final BuyerRefundStorageEntrySyncJob buyerRefundStorageEntrySyncJob;
    private final FirstDealerOrderEntrySyncJob firstDealerOrderEntrySyncJob;

    @Autowired
    public VegaStorageJobConfig(DealerOrderStorageOutSyncJob dealerOrderStorageOutSyncJob,
                                BuyerOrderStorageOutSyncJob buyerOrderStorageOutSyncJob,
                                DealerRefundStorageEntrySyncJob dealerRefundStorageEntrySyncJob,
                                BuyerRefundStorageEntrySyncJob buyerRefundStorageEntrySyncJob,
                                FirstDealerOrderEntrySyncJob firstDealerOrderEntrySyncJob) {
        this.dealerOrderStorageOutSyncJob = dealerOrderStorageOutSyncJob;
        this.buyerOrderStorageOutSyncJob = buyerOrderStorageOutSyncJob;
        this.dealerRefundStorageEntrySyncJob = dealerRefundStorageEntrySyncJob;
        this.buyerRefundStorageEntrySyncJob = buyerRefundStorageEntrySyncJob;
        this.firstDealerOrderEntrySyncJob = firstDealerOrderEntrySyncJob;
    }

    @Scheduled(
            cron = "${order.storage.dealer.sync.leave.cron: 0 */3 * * * * }"
    )
    public void dealerOrderStorageOutSyncJob() {
        dealerOrderStorageOutSyncJob.onSync();
    }

    @Scheduled(
            cron = "${order.storage.buyer.sync.leave.cron: 0 */3 * * * * }"
    )
    public void buyerOrderStorageOutSyncJob() {
        buyerOrderStorageOutSyncJob.onSync();
    }

    @Scheduled(
            cron = "${order.storage.dealer.sync.entry.cron: 0 */3 * * * * }"
    )
    public void dealerRefundStorageEntrySyncJob() {
        dealerRefundStorageEntrySyncJob.onSync();
    }

    @Scheduled(
            cron = "${order.storage.buyer.sync.entry.cron: 0 */3 * * * * }"
    )
    public void buyerRefundStorageEntrySyncJob() {
        buyerRefundStorageEntrySyncJob.onSync();
    }

    @Scheduled(
            cron = "${order.storage.first-dealer.sync.entry.cron: 0 */3 * * * * }"
    )
    public void firstDealerOrderEntrySyncJob() {
        firstDealerOrderEntrySyncJob.onSync();
    }

}
