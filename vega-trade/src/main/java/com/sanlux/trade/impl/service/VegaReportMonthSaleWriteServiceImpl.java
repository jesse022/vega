package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.VegaReportMonthSaleDao;
import com.sanlux.trade.model.VegaReportMonthSale;
import com.sanlux.trade.service.VegaReportMonthSaleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/1/5.
 */
@Service
@RpcProvider
@Slf4j
public class VegaReportMonthSaleWriteServiceImpl implements VegaReportMonthSaleWriteService {
    private final VegaReportMonthSaleDao vegaReportMonthSaleDao;

    @Autowired
    public VegaReportMonthSaleWriteServiceImpl (VegaReportMonthSaleDao vegaReportMonthSaleDao) {
        this.vegaReportMonthSaleDao = vegaReportMonthSaleDao;
    }

    @Override
    public Response<Boolean> create(VegaReportMonthSale vegaReportMonthSale) {
        try {
            return Response.ok(vegaReportMonthSaleDao.create(vegaReportMonthSale));
        } catch (Exception e) {
            log.error("create month sale report failed, vegaReportMonthSale:{}, cause:{}", vegaReportMonthSale, Throwables.getStackTraceAsString(e));
            return Response.fail("create.month.sale.report.fail");
        }
    }

    @Override
    public Response<Boolean> updateByYearAndMonth(VegaReportMonthSale vegaReportMonthSale) {
        try {
            return Response.ok(vegaReportMonthSaleDao.updateByYearAndMonth(vegaReportMonthSale));
        } catch (Exception e) {
            log.error("update month sale report by year and month failed, vegaReportMonthSale:{}, cause:{}", vegaReportMonthSale, Throwables.getStackTraceAsString(e));
            return Response.fail("update.month.sale.report.fail");
        }
    }
}
