/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
public class CreditRepaymentNotification {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺短信接收者手机号
     */
    private String mobile;

    /**
     * 订单编号
     */
    private String tradeNo;

    /**
     * 应还款金额
     */
    private Long shouldRepaymentAmount;

    /**
     * 剩余未还款金额
     */
    private Long remainRepayment;

    /**
     * 罚息
     */
    private Long fineAmount;

    /**
     * 应还款日期
     */
    private String shouldRepaymentDate;

}
