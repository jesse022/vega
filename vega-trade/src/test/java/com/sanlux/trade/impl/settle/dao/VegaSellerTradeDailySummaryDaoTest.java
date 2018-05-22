package com.sanlux.trade.impl.settle.dao;

import com.sanlux.trade.impl.dao.BaseDaoTest;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by jesselu on 2017/2/6.
 */
public class VegaSellerTradeDailySummaryDaoTest extends BaseDaoTest {
    @Autowired
    private VegaSellerTradeDailySummaryDao vegaSellerTradeDailySummaryDao;

    private VegaSellerTradeDailySummary vegaSellerTradeDailySummary;

    @Before
    public void init() {
        vegaSellerTradeDailySummary = make();
        vegaSellerTradeDailySummaryDao.create(vegaSellerTradeDailySummary);
        assertNotNull(vegaSellerTradeDailySummary.getId());
    }


    @Test
    public void count() {
        Integer summaryType = 3;  //3:二级经销商 2:一级
        Integer transStatus = 0;
        long count=vegaSellerTradeDailySummaryDao.countDealerPayment(summaryType,transStatus);
        System.out.println("count====="+count);
    }



    private VegaSellerTradeDailySummary make() {
        VegaSellerTradeDailySummary vegaSellerTradeDailySummary = new VegaSellerTradeDailySummary();


        vegaSellerTradeDailySummary.setSellerId(28L);
        vegaSellerTradeDailySummary.setSellerType(null);
        vegaSellerTradeDailySummary.setSellerName("supplier1");
        vegaSellerTradeDailySummary.setOrderCount(2);
        vegaSellerTradeDailySummary.setRefundOrderCount(null);
        vegaSellerTradeDailySummary.setOriginFee(40000L);
        vegaSellerTradeDailySummary.setRefundFee(0L);
        vegaSellerTradeDailySummary.setSellerDiscount(0L);
        vegaSellerTradeDailySummary.setPlatformDiscount(0L);
        vegaSellerTradeDailySummary.setShipFee(6000L);
        vegaSellerTradeDailySummary.setShipFeeDiscount(0L);
        vegaSellerTradeDailySummary.setActualPayFee(46000L);
        vegaSellerTradeDailySummary.setGatewayCommission(23L);
        vegaSellerTradeDailySummary.setPlatformCommission(20000L);
        vegaSellerTradeDailySummary.setSellerReceivableFee(26000L);
        vegaSellerTradeDailySummary.setSummaryType(3);
        vegaSellerTradeDailySummary.setSumAt(new Date());
        vegaSellerTradeDailySummary.setExtra(null);
        vegaSellerTradeDailySummary.setDiffFee(null);
        vegaSellerTradeDailySummary.setCommission1(null);
        vegaSellerTradeDailySummary.setCommission2(null);
        vegaSellerTradeDailySummary.setCommission3(null);
        vegaSellerTradeDailySummary.setCommission4(null);
        vegaSellerTradeDailySummary.setCommission5(null);
        vegaSellerTradeDailySummary.setTransStatus(0);
        vegaSellerTradeDailySummary.setCreatedAt(new Date());
        vegaSellerTradeDailySummary.setUpdatedAt(new Date());


        return vegaSellerTradeDailySummary;
    }
}
