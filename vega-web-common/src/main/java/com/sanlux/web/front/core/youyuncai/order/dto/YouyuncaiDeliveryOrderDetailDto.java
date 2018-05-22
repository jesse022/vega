package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采交期确认订单明细Dto
 * Created by lujm on 2018/3/12.
 */
@Data
public class YouyuncaiDeliveryOrderDetailDto implements Serializable {

    private static final long serialVersionUID = -2890952602044001805L;

    /**
     * 友云采明细号码
     */
    private String lineNumber;

    /**
     * 集乘网明细号码
     */
    private String supplierlineNumber;

    /**
     * 商品SKU编码
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 厂商型号
     */
    private String manufacturerPartID;

    /**
     * 厂商名称
     */
    private String manufacturerName;

    /**
     * 预计出货日
     */
    private String shipScheduleDate;

    /**
     * 商品数量
     */
    private String quantity;

    /**
     * 币种
     */
    private String currency;

    /**
     * 无税价
     */
    private String nakedPrice;

    /**
     * 含税价
     */
    private String price;

    /**
     * 税率
     */
    private String taxRate;

    /**
     * 税价
     */
    private String taxPrice;

    /**
     * 无税金额
     */
    private String nakedAmount;

    /**
     * 含税金额
     */
    private String amount;

    /**
     * 税额
     */
    private String taxAmount;


}
