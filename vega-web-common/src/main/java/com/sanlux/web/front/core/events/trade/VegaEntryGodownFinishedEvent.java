/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade;

import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 已出库完成事件
 *
 * @author : panxin
 */
@Data
public class VegaEntryGodownFinishedEvent implements Serializable {

    private List<ShopOrder> alreadyEntryOrders;

    public VegaEntryGodownFinishedEvent(List<ShopOrder> alreadyEntryOrders) {
        this.alreadyEntryOrders = alreadyEntryOrders;
    }

}
