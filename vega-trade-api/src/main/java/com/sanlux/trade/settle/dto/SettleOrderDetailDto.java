/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.dto;

import io.terminus.parana.settle.model.SettleOrderDetail;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
public class SettleOrderDetailDto implements Serializable {

    private static final long serialVersionUID = -787148082449532378L;

    private SettleOrderDetail orderDetail;

    private String shopName;

    private Integer shopType;

    public SettleOrderDetailDto(SettleOrderDetail detail, String shopName, Integer shopType) {
        this.orderDetail = detail;
        this.shopName = shopName;
        this.shopType = shopType;
    }

}
