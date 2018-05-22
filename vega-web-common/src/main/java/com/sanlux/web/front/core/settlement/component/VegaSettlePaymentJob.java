/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.component;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import io.terminus.parana.web.core.settle.SettleCreateService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaSettlePaymentJob {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private SettleCreateService settleCreateService;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public void settlePayments(Date startAt, Date endAt) {

        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("[CRON-JOB] settlePayments begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            settleCreateService.settlePayments(startAt, endAt);
        }catch (Exception e){
            log.error("settle payments daily fail, startAt={}, endAt={}, error:{}",startAt, endAt, Throwables.getStackTraceAsString(e));
        }

        stopwatch.stop();
        log.info("[CRON-JOB] settlePayments done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
