/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle.config;

import com.sanlux.web.admin.settle.job.VegaSummarySellerTradeDailyJob;
import io.terminus.parana.web.admin.jobs.settle.component.SummaryPayChannelDailyJob;
import io.terminus.parana.web.admin.jobs.settle.component.SummaryPlatformTradeDailyJob;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

/**
 * @author : panxin
 */
public class VegaProductSummaryJob {

    private final SummaryPayChannelDailyJob summaryPayChannelDailyJob;
    private final SummaryPlatformTradeDailyJob summaryPlatformTradeDailyJob;
    private final VegaSummarySellerTradeDailyJob vegaSummarySellerTradeDailyJob;

    @Autowired
    public VegaProductSummaryJob(SummaryPayChannelDailyJob summaryPayChannelDailyJob,
                                 SummaryPlatformTradeDailyJob summaryPlatformTradeDailyJob,
                                 VegaSummarySellerTradeDailyJob vegaSummarySellerTradeDailyJob) {
        this.summaryPayChannelDailyJob = summaryPayChannelDailyJob;
        this.summaryPlatformTradeDailyJob = summaryPlatformTradeDailyJob;
        this.vegaSummarySellerTradeDailyJob = vegaSummarySellerTradeDailyJob;
    }

    @Scheduled(
            cron = "${vega.settle.cron.prod.summary.channel: 0 3 11  * * * }"
    )
    public void payChannelDailySummary() {
        Date sumAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        summaryPayChannelDailyJob.payChannelDailySummary(sumAt);
    }

    @Scheduled(
            cron = "${vega.settle.cron.prod.summary.platform: 0 6 11 * * * }"
    )
    public void vegaSummaryPlatformTradeDailyJob() {
        Date sumAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        summaryPlatformTradeDailyJob.platformTradeDailyJobSummary(sumAt);
    }

    @Scheduled(
            cron = "${vega.settle.cron.prod.summary.seller: 0 9 11 * * * }"
    )
    public void sellerTradeDailySummary() {
        Date sumAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        vegaSummarySellerTradeDailyJob.sellerTradeDailySummary(sumAt);
    }

}
