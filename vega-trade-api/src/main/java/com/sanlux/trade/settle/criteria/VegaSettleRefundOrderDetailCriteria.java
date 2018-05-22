/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.criteria;

import io.terminus.parana.settle.dto.paging.SettleRefundOrderDetailCriteria;
import lombok.Data;

/**
 * @author : panxin
 */
@Data
public class VegaSettleRefundOrderDetailCriteria extends SettleRefundOrderDetailCriteria {
    private static final long serialVersionUID = -364594393279634318L;

    private String sellerName;
}
