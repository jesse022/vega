/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.model;

import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import lombok.Data;

/**
 * @author : panxin
 */
@Data
public class VegaSettleRefundOrderDetail extends SettleRefundOrderDetail {
    private static final long serialVersionUID = 4621184919347135338L;

    private String sellerType;

}
