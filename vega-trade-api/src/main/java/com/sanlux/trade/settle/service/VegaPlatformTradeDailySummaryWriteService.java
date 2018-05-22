/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.service;

import io.terminus.common.model.Response;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;

import java.util.Date;
import java.util.List;

/**
 * @author : panxin
 */
public interface VegaPlatformTradeDailySummaryWriteService {

    /**
     * 生成指平台日汇总记录
     * @return 是否成功
     */
    Response<Boolean> generatePlatformTradeDailySummary(List<PlatformTradeDailySummary> summaryList);

}
