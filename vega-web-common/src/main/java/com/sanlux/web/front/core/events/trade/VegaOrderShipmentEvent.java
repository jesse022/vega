/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade;

import lombok.Getter;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
public class VegaOrderShipmentEvent implements Serializable {

    private static final long serialVersionUID = 2969280492730054880L;

    @Getter
    private final Long shipmentId;

    public VegaOrderShipmentEvent(Long shipmentId) {
        this.shipmentId = shipmentId;
    }
}
