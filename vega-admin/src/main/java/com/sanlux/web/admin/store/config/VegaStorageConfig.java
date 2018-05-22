/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.config;

import com.sanlux.web.admin.store.job.BuyerOrderStorageOutSyncJob;
import com.sanlux.web.admin.store.job.BuyerRefundStorageEntrySyncJob;
import com.sanlux.web.admin.store.job.DealerOrderStorageOutSyncJob;
import com.sanlux.web.admin.store.job.DealerRefundStorageEntrySyncJob;
import com.sanlux.web.admin.store.job.DealerStorageSyncJob;
import com.sanlux.web.admin.store.job.FirstDealerOrderEntrySyncJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : panxin
 */
public class VegaStorageConfig {

    @ConditionalOnProperty(
            name = "${vega.store.corn.job.enable}",
            havingValue = "true",
            matchIfMissing = true
    )
    @Configuration
    public static class VegaStorageJobConfiguration {

        @Bean
        public DealerOrderStorageOutSyncJob dealerOrderStorageOutSyncJob() {
            return new DealerOrderStorageOutSyncJob();
        }

        @Bean
        public BuyerOrderStorageOutSyncJob buyerOrderStorageOutSyncJob() {
            return new BuyerOrderStorageOutSyncJob();
        }

        @Bean
        public DealerRefundStorageEntrySyncJob dealerRefundStorageEntrySyncJob() {
            return new DealerRefundStorageEntrySyncJob();
        }

        @Bean
        public BuyerRefundStorageEntrySyncJob buyerRefundStorageEntrySyncJob() {
            return new BuyerRefundStorageEntrySyncJob();
        }

        @Bean
        public FirstDealerOrderEntrySyncJob firstDealerOrderEntrySyncJob() {
            return new FirstDealerOrderEntrySyncJob();
        }

        @Bean
        public VegaStorageJobConfig vegaStorageJobConfig(DealerOrderStorageOutSyncJob dealerOrderStorageOutSyncJob,
                                                         BuyerOrderStorageOutSyncJob buyerOrderStorageOutSyncJob,
                                                         DealerRefundStorageEntrySyncJob dealerRefundStorageEntrySyncJob,
                                                         BuyerRefundStorageEntrySyncJob buyerRefundStorageEntrySyncJob,
                                                         FirstDealerOrderEntrySyncJob firstDealerOrderEntrySyncJob) {
            return new VegaStorageJobConfig(dealerOrderStorageOutSyncJob, buyerOrderStorageOutSyncJob,
                    dealerRefundStorageEntrySyncJob, buyerRefundStorageEntrySyncJob, firstDealerOrderEntrySyncJob);
        }
    }

    @ConditionalOnProperty(
            name = "${first-dealer.stock.sync.job.enable}",
            havingValue = "true",
            matchIfMissing = true
    )
    @Configuration
    public static class VegaStorageSyncJob {

        @Bean
        public DealerStorageSyncJob dealerStorageSyncJob() {
            return new DealerStorageSyncJob();
        }

        @Bean
        public VegaStockSyncJobConfig vegaStockSyncJobConfig(DealerStorageSyncJob dealerStorageSyncJob) {
            return new VegaStockSyncJobConfig(dealerStorageSyncJob);
        }
    }

}
