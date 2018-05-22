/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.settle.dao.VegaSellerTradeDailySummaryDao;
import com.sanlux.trade.impl.settle.manager.VegaSettleManager;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.api.SummaryRule;
import io.terminus.parana.settle.impl.dao.SettleOrderDetailDao;
import io.terminus.parana.settle.impl.dao.SettleRefundOrderDetailDao;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@Service
@RpcProvider
public class VegaSellerTradeDailySummaryWriteServiceImpl implements VegaSellerTradeDailySummaryWriteService {

    @Autowired
    private VegaSellerTradeDailySummaryDao sellerTradeDailySummaryDao;

    @Autowired
    private SettleOrderDetailDao settleOrderDetailDao;

    @Autowired
    private SettleRefundOrderDetailDao settleRefundOrderDetailDao;

    @Autowired
    private SummaryRule summaryRule;

    @Autowired
    private VegaSettleManager settleManager;

    @Override
    public Response<Boolean> generateSellerTradeDailySummary(List<VegaSellerTradeDailySummary> allSummaryList) {
        try{
            settleManager.batchCreateVegaSellerTradeDailySummary(allSummaryList);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("generateSellerTradeDailySummary fail, allSummaryList={}, cause={}", allSummaryList , Throwables.getStackTraceAsString(e));
            return Response.fail("generate.seller.trade.daily.summary.fail");
        }
    }

    @Override
    public Response<Long> createSellerTradeDailySummary(VegaSellerTradeDailySummary sellerTradeDailySummary) {
        try {
            sellerTradeDailySummaryDao.create(sellerTradeDailySummary);
            return Response.ok(sellerTradeDailySummary.getId());
        } catch (Exception e) {
            log.error("create sellerTradeDailySummary failed, sellerTradeDailySummary:{}, cause:{}", sellerTradeDailySummary, Throwables.getStackTraceAsString(e));
            return Response.fail("seller.trade.daily.summary.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateSellerTradeDailySummary(VegaSellerTradeDailySummary sellerTradeDailySummary) {
        try {
            return Response.ok(sellerTradeDailySummaryDao.update(sellerTradeDailySummary));
        } catch (Exception e) {
            log.error("update sellerTradeDailySummary failed, sellerTradeDailySummary:{}, cause:{}", sellerTradeDailySummary, Throwables.getStackTraceAsString(e));
            return Response.fail("seller.trade.daily.summary.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteSellerTradeDailySummaryById(Long sellerTradeDailySummaryId) {
        try {
            return Response.ok(sellerTradeDailySummaryDao.delete(sellerTradeDailySummaryId));
        } catch (Exception e) {
            log.error("delete sellerTradeDailySummary failed, sellerTradeDailySummaryId:{}, cause:{}", sellerTradeDailySummaryId, Throwables.getStackTraceAsString(e));
            return Response.fail("seller.trade.daily.summary.delete.fail");
        }
    }

    @Override
    public Response<Boolean> batchCreate(List<VegaSellerTradeDailySummary> forwardSummarys,
                                         List<VegaSellerTradeDailySummary> reverseSummarys,
                                         List<VegaSellerTradeDailySummary> mergeSummarys) {
        Response<Boolean> result = new Response<Boolean>();
        try {
            settleManager.batchCreateVegaSellerDaily(forwardSummarys, reverseSummarys, mergeSummarys);
            result.setResult(Boolean.TRUE);
        }catch (Exception e){
            log.error("batch create seller trade daily summary fail,cause: {}",Throwables.getStackTraceAsString(e));
            result.setError("seller.trade.daily.summary.create.fail");

        }
        return result;
    }

    @Override
    public Response<Boolean> generateSellerTradeDailySummary(Date sumAt) {
        try{
            Date endAt = new DateTime(sumAt.getTime()).plusDays(1).toDate();

            List<SellerTradeDailySummary> forwardList = settleOrderDetailDao.generateSellerTradeDailySummary(sumAt, endAt);
            List<SellerTradeDailySummary> backwardList = settleRefundOrderDetailDao.generateSellerTradeDailySummary(sumAt, endAt);

            List<SellerTradeDailySummary> allSummaryList = summaryRule.sellerDaily(forwardList, backwardList);

            for(SellerTradeDailySummary summary : allSummaryList){
                summary.setSumAt(sumAt);
            }
            settleManager.batchCreateSellerTradeDailySummary(allSummaryList);

            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("generateSellerTradeDailySummary fail, sumAt={}, cause={}", sumAt , Throwables.getStackTraceAsString(e));
            return Response.fail("generate.seller.trade.daily.summary.fail");
        }
    }

}
