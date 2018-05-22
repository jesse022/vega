package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 友云采出货通知Dto
 * Created by lujm on 2018/3/12.
 */
@Data
public class YouyuncaiShipInfoDto implements Serializable {

    private static final long serialVersionUID = 8648681999459903537L;

    /**
     * 友云采订单号
     */
    private String  orderCode;

    /**
     * 集乘网订单号
     */
    private String supplierOrderID;

    /**
     * 订单详情
     */
    private List<YouyuncaiShipInfoOrderDetailDto> orderDetail;

}
