package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 业务经理提成统计Dto
 *
 * Created by lujm on 2017/11/13.
 */
@Data
public class ServiceManagerOrderDto implements Serializable {
    private static final long serialVersionUID = -5306224653644742810L;

    /**
     * 业务经理ID
     */
    private Long serviceManagerId;

    /**
     * 业务经理姓名
     */
    private String serviceManagerName;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 实付金额
     */
    private Long fee;

    /**
     * 新会员提成
     */
    private Long newMemberCommission;

    /**
     * 老会员提成
     */
    private Long oldMemberCommission;

    /**
     * 总提成
     */
    private Long totalMemberCommission;

}
