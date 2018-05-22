/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle.job;

import com.google.common.eventbus.EventBus;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.pay.credit.request.CreditPayToken;
import com.sanlux.web.front.core.settlement.credit.CreditPayTrans;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.api.TransCollector;
import io.terminus.pay.api.TransLoader;
import io.terminus.pay.component.TransJobTemplate;
import io.terminus.pay.model.PayTransCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : panxin
 */
@Component
@ConditionalOnProperty(
        name = "pay.job.trans.credit-pay.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class CreditPayTransJob extends TransJobTemplate<CreditPayToken, CreditPayTrans> {

    @Autowired
    public CreditPayTransJob(TokenProvider<CreditPayToken> tokenProvider,
                             EventBus eventBus,
                             TransLoader<CreditPayTrans, CreditPayToken> transLoader,
                             TransCollector<CreditPayTrans> transCollector) {
        super(tokenProvider, eventBus, transLoader, transCollector, CreditPayConstants.PAY_CHANNEL);
    }

    @Scheduled(
            cron = "${pay.cron.test.trans.credit-pay: 0 0 2 * * ?}"
    )
    public void syncCreditPayTrans() {
        PayTransCriteria criteria = new PayTransCriteria().startFromToday().endWithNow();
        super.syncTrans(criteria);
    }

    public void syncCreditPayTrans(PayTransCriteria criteria) {
        super.syncTrans(criteria);
    }

}
