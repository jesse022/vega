package com.sanlux.trade.service;

import com.sanlux.trade.model.VegaReportMonthSale;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 运营月销售报表读服务接口
 * Created by lujm on 2018/1/5
 */
public interface VegaReportMonthSaleReadService {


    /**
     * 根据Id获取销售报表信息
     * @param Id id
     * @return
     */
    Response<VegaReportMonthSale> findById(Long Id);

    /**
     * 根据Ids获取销售报表信息
     * @param ids Ids
     * @return
     */
    Response<List<VegaReportMonthSale>> findByIds(List<Long> ids);

    /**
     * 根据年度获取销售报表信息
     * @param year 年度
     * @return
     */
    Response<List<VegaReportMonthSale>> findByYear(Integer year);

    /**
     * 根据年度和月度获取销售报表信息
     * @param year  年度
     * @param month 月度
     * @return 销售报表
     */
    Response<VegaReportMonthSale> findByYearAndMonth(Integer year, Integer month);
}
