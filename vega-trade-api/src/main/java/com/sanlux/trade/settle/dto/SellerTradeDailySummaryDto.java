/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.dto;

import io.terminus.common.model.Paging;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
public class SellerTradeDailySummaryDto implements Serializable{

    private static final long serialVersionUID = -1240522842030290271L;

    private Paging<SellerTradeDailySummary> paging;

    private Integer totalOrderCount;

    private Integer totalRefundOrderCount;

    private Long totalOriginFee;

    private Long totalRefundFee;

    private Long totalShipFee;

    private Long totalActualPayFee;

    private Long totalPlatformCommission;

    private Long totalDealerCommission;

    private Long totalPriceDiff;

}
