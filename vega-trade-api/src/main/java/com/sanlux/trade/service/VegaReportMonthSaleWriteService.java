package com.sanlux.trade.service;

import com.sanlux.trade.model.VegaReportMonthSale;
import io.terminus.common.model.Response;

/**
 * 运营月销售报表写服务接口
 * Created by lujm on 2018/1/5
 */
public interface VegaReportMonthSaleWriteService {

    /**
     * 创建
     *
     * @param vegaReportMonthSale 月度销售报表
     * @return 主键id
     */
    Response<Boolean> create(VegaReportMonthSale vegaReportMonthSale);

    /**
     * 根据年度和月度更新
     *
     * @param vegaReportMonthSale 月度销售报表
     * @return 是否成功
     */
    Response<Boolean> updateByYearAndMonth(VegaReportMonthSale vegaReportMonthSale);
}
