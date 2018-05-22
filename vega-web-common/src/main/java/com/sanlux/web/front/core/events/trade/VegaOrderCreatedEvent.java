/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade;

import lombok.Getter;

import java.io.Serializable;

/**
 * Author: songrenfei
 * Date: 2016-01-31
 */
public class VegaOrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 5290116056802912492L;
    @Getter
    private final Long orderId;

    @Getter
    private final String roleName;

    @Getter
    private final Long  purchaseOrderId;

    public VegaOrderCreatedEvent(Long orderId,String roleName,Long purchaseOrderId) {
        this.orderId = orderId;
        this.roleName = roleName;
        this.purchaseOrderId = purchaseOrderId;
    }
}
