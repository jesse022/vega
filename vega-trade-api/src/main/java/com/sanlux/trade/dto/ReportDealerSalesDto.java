package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务商每日销售情况
 * Created by lujm on 2018/4/24.
 */
@Data
public class ReportDealerSalesDto implements Serializable {
    private static final long serialVersionUID = -840836254796215102L;

    /**
     * 主键(summaryDate + shopId)
     */
    private String key;

    /**
     * 时间
     */
    private String summaryDate;

    /**
     * 服务商Id
     */
    private Long shopId;

    /**
     * 服务商名称
     */
    private String shopName;

    /**
     * 成交订单数量
     */
    private Integer orderSum;

    /**
     * 成交金额
     */
    private Long salesVolumeSum;

}
