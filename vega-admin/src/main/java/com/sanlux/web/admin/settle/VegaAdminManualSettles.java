/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle;

import com.sanlux.web.admin.settle.job.CreditPayTransJob;
import com.sanlux.web.admin.settle.job.component.SettleSellerSummaryGenerateService;
import com.sanlux.web.front.core.settlement.component.VegaSettleRefundJob;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.service.PlatformTradeDailySummaryWriteService;
import io.terminus.pay.model.PayTransCriteria;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/settle/manual")
public class VegaAdminManualSettles {

    @Autowired
    SettleSellerSummaryGenerateService settleSellerSummaryGenerateService;
    @Autowired
    private VegaSettleRefundJob vegaSettleRefundJob;
    @Autowired
    private CreditPayTransJob creditPayTransJob;
    @RpcConsumer
    private PlatformTradeDailySummaryWriteService platformTradeDailySummaryWriteService;

    @RequestMapping(value = "/seller-daily-summary", method = RequestMethod.GET)
    public Boolean sellerDailySummary(@RequestParam String date) {
        Date sumAt = DateTime.parse(date).withTimeAtStartOfDay().toDate();
        Response<Boolean> resp = settleSellerSummaryGenerateService.generateSellerTradeDailySummary(sumAt);
        if (!resp.isSuccess()) {
            log.error("failed to generate seller trade daily summary, summary at [{}], cause : {}",
                    sumAt, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/platform-daily-summary", method = RequestMethod.GET)
    public Boolean platformDailySummary(@RequestParam String date) {
        Date sumAt = DateTime.parse(date).withTimeAtStartOfDay().toDate();
        Response<Boolean> resp = platformTradeDailySummaryWriteService.generatePlatformTradeDailySummary(sumAt);
        if (!resp.isSuccess()) {
            log.error("failed to generate platform trade daily summary, summary at [{}], cause : {}",
                    sumAt, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/refund-order-detail", method = RequestMethod.GET)
    public Boolean refundOrderDetail(@RequestParam String date) {
        Date startAt = DateTime.parse(date).withTimeAtStartOfDay().toDate();
        Date endAt = DateTime.now().toDate();
        vegaSettleRefundJob.settleRefunds(startAt, endAt);
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/credit-trans", method = RequestMethod.GET)
    public void creditPayTran(PayTransCriteria criteria) {
        log.info("credit trans criteria = {}", criteria);
        creditPayTransJob.syncCreditPayTrans(criteria);
    }


}
