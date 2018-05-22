/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle.config;

import com.sanlux.web.admin.settle.job.VegaSummarySellerTradeDailyJob;
import io.terminus.parana.web.admin.jobs.settle.component.SummaryPayChannelDailyJob;
import io.terminus.parana.web.admin.jobs.settle.component.SummaryPlatformTradeDailyJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author : panxin
 */
@Configuration
public class VegaSettleJobConfig {

    @ConditionalOnProperty(
            name = {"vega.settle.job.summary.enable"},
            havingValue = "true",
            matchIfMissing = true
    )
    @Configuration
    public static class DailySummaryConfig {
        public DailySummaryConfig() {
        }

        @Bean
        public SummaryPayChannelDailyJob summaryPayChannelDailyJob() {
            return new SummaryPayChannelDailyJob();
        }

//        @Bean
//        public VegaSummaryPlatformTradeDailyJob vegaSummaryPlatformTradeDailyJob() {
//            return new VegaSummaryPlatformTradeDailyJob();
//        }

        @Bean
        public SummaryPlatformTradeDailyJob summaryPlatformTradeDailyJob() {
            return new SummaryPlatformTradeDailyJob();
        }

        @Bean
        public VegaSummarySellerTradeDailyJob vegaSummarySellerTradeDailyJob() {
            return new VegaSummarySellerTradeDailyJob();
        }

        @Bean
        @Profile({"test", "dev"})
        public VegaTestSummaryJob vegaTestSummaryJob(SummaryPayChannelDailyJob summaryPayChannelDailyJob,
                                                     SummaryPlatformTradeDailyJob summaryPlatformTradeDailyJob,
                                                     VegaSummarySellerTradeDailyJob vegaSummarySellerTradeDailyJob) {
            return new VegaTestSummaryJob(summaryPayChannelDailyJob,
                    summaryPlatformTradeDailyJob, vegaSummarySellerTradeDailyJob);
        }

        @Bean
        public VegaProductSummaryJob vegaProductSummaryJob(SummaryPayChannelDailyJob summaryPayChannelDailyJob,
                                                       SummaryPlatformTradeDailyJob summaryPlatformTradeDailyJob,
                                                       VegaSummarySellerTradeDailyJob vegaSummarySellerTradeDailyJob) {
            return new VegaProductSummaryJob(summaryPayChannelDailyJob,
                    summaryPlatformTradeDailyJob, vegaSummarySellerTradeDailyJob);
        }
    }

}
