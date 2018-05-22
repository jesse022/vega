/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle.job;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.sanlux.web.admin.settle.job.component.SettleSellerSummaryGenerateService;
import io.terminus.common.model.Response;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author : panxin
 */
@Slf4j
public class VegaSummarySellerTradeDailyJob {

    @Autowired
    private SettleSellerSummaryGenerateService settleSellerSummaryGenerateService;
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private EventBus eventBus;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public VegaSummarySellerTradeDailyJob() {
    }

    public void sellerTradeDailySummary(Date sumAt) {
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
        } else {
            log.info("[CRON-JOB] generateSellerTradeDailySummary begin {}", DFT.print(DateTime.now()));
            Stopwatch stopwatch = Stopwatch.createStarted();
            Response response = settleSellerSummaryGenerateService.generateSellerTradeDailySummary(sumAt);
            if(!response.isSuccess()) {
                log.error("handle seller daily summary fail,error:{}", response.getError());
            }

            stopwatch.stop();
            log.info("[CRON-JOB] generateSellerTradeDailySummary done at {} cost {} ms",
                    DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
