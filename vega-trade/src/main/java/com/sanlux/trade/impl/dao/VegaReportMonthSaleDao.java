package com.sanlux.trade.impl.dao;

import com.sanlux.trade.model.VegaReportMonthSale;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Arguments;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 运营月销售报表Dao
 * Created by lujm on 2018/1/5.
 */
@Repository
public class VegaReportMonthSaleDao extends MyBatisDao<VegaReportMonthSale> {

    /**
     * 根据年度获取月销售报表
     * @param year 年度
     * @return 月销售报表
     */

    public List<VegaReportMonthSale> findByYear(Integer year) {
        return getSqlSession().selectList(sqlId("findByYear"), year);
    }

    /**
     * 根据年度和月度获取月销售报表
     * @param year 年度
     * @param month 月度
     * @return 月销售报表
     */

    public VegaReportMonthSale findByYearAndMonth(Integer year, Integer month) {
        VegaReportMonthSale vegaReportMonthSale = new VegaReportMonthSale();
        vegaReportMonthSale.setYear(year);
        vegaReportMonthSale.setMonth(month);
        return getSqlSession().selectOne(sqlId("findByYearAndMonth"), vegaReportMonthSale);
    }

    /**
     * 根据年度和月度更新月销售报表
     * @param vegaReportMonthSale 销售报表信息
     * @return 是否更新成功
     */

    public Boolean updateByYearAndMonth(VegaReportMonthSale vegaReportMonthSale) {
        if (Arguments.isNull(vegaReportMonthSale.getYear())) {
            return Boolean.FALSE;
        }
        if (Arguments.isNull(vegaReportMonthSale.getMonth())) {
            return Boolean.FALSE;
        }
        return getSqlSession().update(sqlId("updateByYearAndMonth"), vegaReportMonthSale) == 1;
    }

}
