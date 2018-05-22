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
public class VegaTestSettleTradeJob {

    // private final VegaSettlePaymentJob vegaSettlePaymentJob;
    private final VegaSettleRefundJob vegaSettleRefundJob;

    @Autowired
    public VegaTestSettleTradeJob(VegaSettleRefundJob vegaSettleRefundJob) {
        this.vegaSettleRefundJob = vegaSettleRefundJob;
    }

    @Scheduled(cron = "${vega.settle.cron.test.refund: 3 */3 * * * * }")
    public void settleRefunds() {
        Date startAt = DateTime.now().withTimeAtStartOfDay().toDate();
        Date endAt = DateTime.now().toDate();
        vegaSettleRefundJob.settleRefunds(startAt, endAt);
    }
}
