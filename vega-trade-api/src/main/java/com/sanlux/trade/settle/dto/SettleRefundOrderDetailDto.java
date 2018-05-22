/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.dto;

import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
public class SettleRefundOrderDetailDto implements Serializable {

    private static final long serialVersionUID = -787148082449532378L;

    private SettleRefundOrderDetail refundOrderDetail;

    private String shopName;

    private Integer shopType;

    public SettleRefundOrderDetailDto(SettleRefundOrderDetail detail, String shopName, Integer shopType) {
        this.refundOrderDetail = detail;
        this.shopName = shopName;
        this.shopType = shopType;
    }

}
