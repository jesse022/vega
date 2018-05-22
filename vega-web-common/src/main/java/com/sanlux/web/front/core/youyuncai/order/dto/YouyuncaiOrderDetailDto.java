package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采订单sku详情Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderDetailDto implements Serializable {
    private static final long serialVersionUID = 7600063386674926098L;
    /**
     * 商品行号
     */
    private String lineNumber;

    /**
     * 商品sku编号
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
     * 货期(天)
     */
    private String leadTime;

    /**
     * 希望出货日期
     */
    private String requestShipDate;

    /**
     * 出货数量
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
     * 税价（单个商品）
     */
    private String taxPrice;

    /**
     * 无税金额
     */
    private String nakedAmoun;

    /**
     * 含税金额
     */
    private String amount;

    /**
     * 税额
     */
    private String taxAmount;

    /**
     * json格式，商品扩展字段，来自check out时的扩展字段
     */
    private String extendedInfo;

}
