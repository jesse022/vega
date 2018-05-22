/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.sanlux.pay.credit.dto;

import lombok.Data;

/**
 * 支付宝退款单数据集
 *
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-12 11:18 AM  <br>
 * Author: xiao
 */
@Data
public class CreditPayRefundData {

    /**
     *  交易号
     */
    private String tradeNo;
    /**
     * 退款金额
     */
    private Integer refundAmount;
    /**
     * 退款理由
     */
    private String reason;

    /**
     * 构造函数
     *
     * @param tradeNo   交易流水
     * @param refundAmount   退款金额(单位：分）
     * @param reason    退款理由
     */
    public CreditPayRefundData(String tradeNo, Integer refundAmount, String reason) {
        this.tradeNo = tradeNo;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

}
