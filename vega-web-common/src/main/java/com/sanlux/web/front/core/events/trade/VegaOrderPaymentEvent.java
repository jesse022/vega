/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade;

import lombok.Getter;

/**
 * 支付单状态更新事件
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-13
 */
public class VegaOrderPaymentEvent {
    @Getter
    private final Long paymentId;

    public VegaOrderPaymentEvent(Long paymentId) {
        this.paymentId = paymentId;
    }
}
