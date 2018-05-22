/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.service;

import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import io.terminus.common.model.Response;

import java.util.Date;
import java.util.List;

/**
 * @author : panxin
 */
public interface VegaSellerTradeDailySummaryWriteService {

    /**
     * 生成日汇总信息
     * @param allSummaryList 信息
     * @return 是否成功
     */
    Response<Boolean> generateSellerTradeDailySummary(List<VegaSellerTradeDailySummary> allSummaryList);

    /**
     * 创建SellerTradeDailySummary
     * @param sellerTradeDailySummary 信息
     * @return 主键id
     */
    Response<Long> createSellerTradeDailySummary(VegaSellerTradeDailySummary sellerTradeDailySummary);

    /**
     * 更新SellerTradeDailySummary
     * @param sellerTradeDailySummary 信息
     * @return 是否成功
     */
    Response<Boolean> updateSellerTradeDailySummary(VegaSellerTradeDailySummary sellerTradeDailySummary);

    /**
     * 根据主键id删除SellerTradeDailySummary
     * @param sellerTradeDailySummaryId ID
     * @return 是否成功
     */
    Response<Boolean> deleteSellerTradeDailySummaryById(Long sellerTradeDailySummaryId);
    /**
     * 批量创建 商家日汇总
     * @param forwardSummarys 正向汇总
     * @param reverseSummarys 逆向汇总
     * @param mergeSummarys 合并汇总
     * @return 是否创建成功
     */
    Response<Boolean> batchCreate(List<VegaSellerTradeDailySummary> forwardSummarys,
                                  List<VegaSellerTradeDailySummary> reverseSummarys,
                                  List<VegaSellerTradeDailySummary> mergeSummarys);

    /**
     * 生成指定汇总时间的商家日汇总记录, 如果已经存在则更新
     * @param sumAt 指定的汇总时间, 格式为yyyy-MM-dd
     * @return 是否成功
     */
    Response<Boolean> generateSellerTradeDailySummary(Date sumAt);
}
