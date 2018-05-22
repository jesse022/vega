/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.event;

import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 确认接单事件
 *
 * @author : panxin
 */
@Data
public class PaymentPayChannelDetailEvent implements Serializable {

    private static final long serialVersionUID = 2942974618922150137L;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付单号
     */
    private Long paymentId;

    /**
     * 订单号
     */
    private String tradeNo;

    /**
     * 流水号
     */
    private String paymentCode;

    /**
     * 支付时间
     */
    private Date paidAt;

    /**
     * 节点
     */
    private VegaOrderEvent vegaOrderEvent;

    public PaymentPayChannelDetailEvent(String channel,
                                        Long paymentId,
                                        String tradeNo,
                                        String paymentCode,
                                        Date paidAt,
                                        VegaOrderEvent vegaOrderEvent) {
        this.channel = channel;
        this.paymentId = paymentId;
        this.tradeNo = tradeNo;
        this.paymentCode = paymentCode;
        this.paidAt = paidAt;
        this.vegaOrderEvent = vegaOrderEvent;
    }

}
