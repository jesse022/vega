/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.settle.service.VegaPlatformTradeDailySummaryWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.impl.manager.SettleManager;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@Service
@RpcProvider
public class VegaPlatformTradeDailySummaryWriteServiceImpl implements VegaPlatformTradeDailySummaryWriteService {

    @Autowired
    private SettleManager settleManager;

    @Override
    public Response<Boolean> generatePlatformTradeDailySummary(List<PlatformTradeDailySummary> summaryList) {
        try{
            settleManager.batchCreateOrUpdatePlatformTradeDailySummary(summaryList);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("generatePlatformTradeDailySummary fail, summaryList={}, cause={}", summaryList , Throwables.getStackTraceAsString(e));
            return Response.fail("generate.platform.trade.daily.summary.fail");
        }
    }


}
