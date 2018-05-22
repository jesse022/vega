package com.sanlux.trade.impl.manager;

import com.sanlux.trade.impl.dao.VegaDirectPayInfoDao;
import com.sanlux.trade.impl.settle.dao.VegaSellerTradeDailySummaryDao;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by liangfujie on 16/11/3
 */
@Slf4j
@Component
public class DirectPayInfoManager {

    @Autowired
    private VegaDirectPayInfoDao vegaDirectPayInfoDao;
    @Autowired
    private VegaSellerTradeDailySummaryDao sellerTradeDailySummaryDao;


    @Transactional
    public void createVegaDirectPayInfo(VegaDirectPayInfo vegaDirectPayInfo,
                                        VegaSellerTradeDailySummary sellerTradeDailySummary){
        vegaDirectPayInfoDao.create(vegaDirectPayInfo);
        sellerTradeDailySummaryDao.update(sellerTradeDailySummary);
    }

    @Transactional
    public void updateVegaDirectPayInfoStatus(VegaDirectPayInfo vegaDirectPayInfo,
                                              VegaSellerTradeDailySummary sellerTradeDailySummary){
        vegaDirectPayInfoDao.update(vegaDirectPayInfo);
        sellerTradeDailySummaryDao.update(sellerTradeDailySummary);
    }





}
