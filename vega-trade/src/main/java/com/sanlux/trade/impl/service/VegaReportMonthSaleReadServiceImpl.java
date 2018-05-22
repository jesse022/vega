package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.VegaReportMonthSaleDao;
import com.sanlux.trade.model.VegaReportMonthSale;
import com.sanlux.trade.service.VegaReportMonthSaleReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lujm on 2018/1/5.
 */
@Service
@RpcProvider
@Slf4j
public class VegaReportMonthSaleReadServiceImpl implements VegaReportMonthSaleReadService {
    private final VegaReportMonthSaleDao vegaReportMonthSaleDao;

    @Autowired
    public VegaReportMonthSaleReadServiceImpl (VegaReportMonthSaleDao vegaReportMonthSaleDao) {
        this.vegaReportMonthSaleDao = vegaReportMonthSaleDao;
    }


    @Override
    public Response<VegaReportMonthSale> findById(Long id) {
        try {
            return Response.ok(vegaReportMonthSaleDao.findById(id));
        } catch (Exception e) {
            log.error("find month sale report by id failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("month.sale.report.find.fail");
        }
    }

    @Override
    public Response<List<VegaReportMonthSale>> findByIds(List<Long> ids) {
        try {
            return Response.ok(vegaReportMonthSaleDao.findByIds(ids));
        } catch (Exception e) {
            log.error("find month sale report by ids failed, ids:{}, cause:{}", ids, Throwables.getStackTraceAsString(e));
            return Response.fail("month.sale.report.find.fail");
        }
    }

    @Override
    public Response<List<VegaReportMonthSale>> findByYear(Integer year) {
        try {
            return Response.ok(vegaReportMonthSaleDao.findByYear(year));
        } catch (Exception e) {
            log.error("find month sale report by year failed, year:{}, cause:{}", year, Throwables.getStackTraceAsString(e));
            return Response.fail("month.sale.report.find.fail");
        }
    }

    @Override
    public Response<VegaReportMonthSale> findByYearAndMonth(Integer year, Integer month) {
        try {
            return Response.ok(vegaReportMonthSaleDao.findByYearAndMonth(year, month));
        } catch (Exception e) {
            log.error("find month sale report by year and month failed, year:{} month:{},  cause:{}", year, month, Throwables.getStackTraceAsString(e));
            return Response.fail("month.sale.report.find.fail");
        }
    }
}
