/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.manager;

import com.sanlux.trade.impl.settle.dao.VegaSellerTradeDailySummaryDao;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.SummaryType;
import io.terminus.parana.settle.impl.dao.PayChannelDailySummaryDao;
import io.terminus.parana.settle.impl.dao.PayChannelDetailDao;
import io.terminus.parana.settle.impl.dao.PlatformTradeDailySummaryDao;
import io.terminus.parana.settle.impl.dao.SettleOrderDetailDao;
import io.terminus.parana.settle.impl.dao.SettleRefundOrderDetailDao;
import io.terminus.parana.settle.impl.dao.SettlementDao;
import io.terminus.parana.settle.impl.manager.SettleManager;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author : panxin
 */
@Primary
@Component
public class VegaSettleManager extends SettleManager{

    @Autowired
    private SettlementDao settlementDao;
    @Autowired
    private PayChannelDetailDao payChannelDetailDao;
    @Autowired
    private VegaSellerTradeDailySummaryDao sellerTradeDailySummaryDao;
    @Autowired
    private PlatformTradeDailySummaryDao platformTradeDailySummaryDao;
    @Autowired
    private PayChannelDailySummaryDao payChannelDailySummaryDao;
    @Autowired
    private SettleOrderDetailDao settleOrderDetailDao;
    @Autowired
    private SettleRefundOrderDetailDao settleRefundOrderDetailDao;

    @Transactional
    public void batchCreateVegaSellerTradeDailySummary(List<VegaSellerTradeDailySummary> summaryList) {
        for (VegaSellerTradeDailySummary summary : summaryList) {
            sellerTradeDailySummaryDao.createOrderUpdate(summary);
        }
    }

    @Transactional
    public void batchCreateVegaSellerDaily(List<VegaSellerTradeDailySummary> forwardSummarys,
                                           List<VegaSellerTradeDailySummary> reverseSummarys,
                                           List<VegaSellerTradeDailySummary> mergeSummarys) {
        for (VegaSellerTradeDailySummary summary : forwardSummarys) {
            summary.setSummaryType(SummaryType.FORWARD.value());
            sellerTradeDailySummaryDao.createOrderUpdate(summary);
        }
        for (VegaSellerTradeDailySummary summary : reverseSummarys) {
            summary.setSummaryType(SummaryType.BACKWARD.value());
            sellerTradeDailySummaryDao.createOrderUpdate(summary);
        }
        for (VegaSellerTradeDailySummary summary : mergeSummarys) {
            summary.setSummaryType(SummaryType.ALL.value());
            sellerTradeDailySummaryDao.createOrderUpdate(summary);
        }
    }

    @Transactional
    public void createOrUpdateOrderDetail(SettleOrderDetail settleOrderDetail) {
        SettleOrderDetail exists = settleOrderDetailDao.findByShopOrderId(settleOrderDetail.getOrderId());
        if (exists == null) {
            settleOrderDetailDao.create(settleOrderDetail);
        } else {
            settleOrderDetail.setId(exists.getId());
            if (exists.getCheckStatus().equals(CheckStatus.CHECK_SUCCESS.value())) {
                settleOrderDetail.setCheckStatus(exists.getCheckStatus());
                settleOrderDetail.setCheckAt(exists.getCheckAt());
                settleOrderDetail.setGatewayCommission(exists.getGatewayCommission());
                settleOrderDetail.setChannelAccount(exists.getChannelAccount());
                // 设置商家应收
                // settleOrderDetail.setSellerReceivableFee(settleOrderDetail.getSellerReceivableFee() - exists.getGatewayCommission());
                settleOrderDetail.setSellerReceivableFee(settleOrderDetail.getSellerReceivableFee());
            }
            settleOrderDetailDao.update(settleOrderDetail);
        }
    }

    @Transactional
    public void createOrUpdateRefundDetail(SettleRefundOrderDetail settleRefundOrderDetail) {
        SettleRefundOrderDetail exists = settleRefundOrderDetailDao.findByRefundId(settleRefundOrderDetail.getRefundId());
        if (exists == null) {
            settleRefundOrderDetailDao.create(settleRefundOrderDetail);
        } else {
            settleRefundOrderDetail.setId(exists.getId());
            if (exists.getCheckStatus().equals(CheckStatus.CHECK_SUCCESS.value())) {
                settleRefundOrderDetail.setCheckStatus(exists.getCheckStatus());
                settleRefundOrderDetail.setCheckAt(exists.getCheckAt());
                settleRefundOrderDetail.setGatewayCommission(exists.getGatewayCommission());
                settleRefundOrderDetail.setChannelAccount(exists.getChannelAccount());
                // 设置商家应收
                // settleRefundOrderDetail.setSellerDeductFee(settleRefundOrderDetail.getSellerDeductFee() - exists.getGatewayCommission());
                settleRefundOrderDetail.setSellerDeductFee(settleRefundOrderDetail.getSellerDeductFee());
            }
            settleRefundOrderDetailDao.update(settleRefundOrderDetail);
        }
    }
}
