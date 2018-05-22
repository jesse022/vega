/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.credit;

import io.terminus.pay.api.TransCollector;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.PayTrans;
import io.terminus.pay.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class CreditPayTransCollector implements TransCollector<CreditPayTrans> {

    private final Long commissionRate;

    @Autowired
    public CreditPayTransCollector(@Value("${pay.credit-pay.commission.rate: 0}") Long commissionRate) {
        this.commissionRate = commissionRate;
    }

    @Override
    public List<PayTrans> collectCommissionAndRate(List<CreditPayTrans> transList) throws PayException {
        log.info("[credit-pay] pay trans collector, transList = {}");

        List<PayTrans> result = new ArrayList<>();
        for(CreditPayTrans trans : transList){
            PayTrans payTrans = new PayTrans();
            payTrans.setChannel(trans.getChannel());
            payTrans.setFee(trans.getFee());
            payTrans.setCommission(NumberUtil.splitByRate(trans.getFee(), commissionRate));
            payTrans.setRate(commissionRate);
            payTrans.setGatewayLogId(trans.getId().toString());
            payTrans.setAccountNo(trans.getAccount());
            payTrans.setTradeNo(trans.getTradeNo());
            payTrans.setRefundNo(trans.getRefundNo());

            result.add(payTrans);
        }
        return result;
    }
}
