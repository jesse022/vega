package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 友云采checkOut接口Dto
 * Created by lujm on 2018/3/5.
 */
@Data
public class YouyuncaiCheckOutDto implements Serializable{

    private static final long serialVersionUID = -4620156953165095904L;

    /**
     * 友云采用户checkIn cookie
     */
    private String cookieId;

    /**
     * 集乘网用户ID
     */
    private String custUserCode;

    /**
     * 无税金额
     */
    private String nakedAmount;

    /**
     * 税额
     */
    private String taxAmount;

    /**
     * 总额,购物车待支付总额（price * quantity求和，不含运费）
     */
    private String amount;

    /**
     * 集乘网企业编码
     */
    private String customerCode;

    /**
     * 订单详情
     */
    private List<YouyuncaiCheckOutOrderDetailDto> orderDetail;


}
