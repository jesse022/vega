/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.dto;

import io.terminus.common.model.Paging;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
public class PlatformTradeDailySummaryDto implements Serializable{

    private static final long serialVersionUID = -1285209924847011422L;

    private Paging<PlatformTradeDailySummary> paging;

    /**
     * 总订单数量
     */
    private Integer totalOrderCount;

    /**
     * 总退款单数量
     */
    private Integer totalRefundOrderCount;

    /**
     * 总应收金额
     */
    private Long totalOriginFee;

    /**
     * 总退款金额
     */
    private Long totalRefundFee;

    /**
     * 总运费
     */
    private Long totalShipFee;

    /**
     * 总实收金额
     */
    private Long totalActualPayFee;

    /**
     * 总平台优惠
     */
    private Long totalPlatformDiscount;

    /**
     * 总平台佣金
     */
    private Long totalPlatformCommission;

    /**
     * 总经销商佣金
     */
    private Long totalDealerCommission;

    /**
     * 总平台差价佣金
     */
    private Long totalPriceDiff;

}
