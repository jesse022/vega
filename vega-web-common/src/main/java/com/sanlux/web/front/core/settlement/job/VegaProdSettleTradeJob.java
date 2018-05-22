/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.job;

import com.sanlux.web.front.core.settlement.component.VegaSettleRefundJob;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

/**
 * @author : panxin
 */
public class VegaProdSettleTradeJob {

    private final VegaSettleRefundJob vegaSettleRefundJob;

    @Autowired
    public VegaProdSettleTradeJob(VegaSettleRefundJob vegaSettleRefundJob) {
        this.vegaSettleRefundJob = vegaSettleRefundJob;
    }

    @Scheduled(
            cron = "${vega.settle.cron.prod.refund: 30 0 1  * * * }"
    )
    public void settleRefunds() {
        Date startAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        Date endAt = DateTime.now().withTimeAtStartOfDay().toDate();
        vegaSettleRefundJob.settleRefunds(startAt, endAt);
    }
}
