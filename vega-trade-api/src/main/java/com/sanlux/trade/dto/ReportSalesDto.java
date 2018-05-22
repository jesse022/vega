package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 运营销售情况统计报表查询DTO
 * Created by lujm on 2017/9/21.
 */
@Data
public class ReportSalesDto implements Serializable {
    private static final long serialVersionUID = -3998219754462479977L;

    /*汇总时间*/
    private String summaryDate;

    /*年度*/
    private Integer year;

    /*月度*/
    private Integer month;

    /*状态*/
    private Boolean status;

    /*成交订单数*/
    private Integer orderSum;

    /*访客类型*/
    private List<ReportTerminalDto> reportTerminalDtos;

    /*类目销售金额*/
    private List<ReportCategoryDto> reportCategoryDtos;

    /*成交金额*/
    private Long salesVolumeSum;

    /*累计订单金额(销量),不存数据库*/
    private Long totalOrderFee;

    /*会员数量*/
    private Integer memberSum;
}
