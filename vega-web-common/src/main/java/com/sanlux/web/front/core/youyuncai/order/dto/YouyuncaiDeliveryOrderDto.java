package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 友云采交期确认Dto
 * Created by lujm on 2018/3/12.
 */
@Data
public class YouyuncaiDeliveryOrderDto implements Serializable{
    private static final long serialVersionUID = 3710122255802447838L;

    /**
     * 订单状态，0：取消，1：确认
     */
    private String orderState;

    /**
     * 友云采订单号
     */
    private String orderCode;

    /**
     * 集乘网订单号
     */
    private String supplierOrderID;

    /**
     * 订单详情
     */
    private List<YouyuncaiDeliveryOrderDetailDto> orderDetail;


}
