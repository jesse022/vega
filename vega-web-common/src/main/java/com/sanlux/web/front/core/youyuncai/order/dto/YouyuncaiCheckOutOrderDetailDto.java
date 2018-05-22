package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采checkOut orderDetail接口Dto
 * Created by lujm on 2018/3/5.
 */
@Data
public class YouyuncaiCheckOutOrderDetailDto implements Serializable {

    private static final long serialVersionUID = -153194661147204809L;

    /**
     * 商品行号
     */
    private String lineNumber;

    /**
     * 商品sku编号
     */
    private String skuCode;

    /**
     * 商品名称(商品名称+SKU规格)
     */
    private String  productName;

    /**
     * 商品分类(编号)
     */
    private String classification;

    /**
     * 厂商型号
     */
    private String manufacturerPartID;

    /**
     * 厂商名称
     */
    private String manufacturerName;

    /**
     * 品牌(品牌中文名称)
     */
    private String brand;

    /**
     * 货期（天）
     */
    private String leadTime;


    /**
     * 单位
     */
    private String UnitOfMeasure;

    /**
     * 数量
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
    private String nakedAmount;

    /**
     * 含税金额
     */
    private String amount;

    /**
     * 税额
     */
    private String taxAmount;

    /**
     * 商品详情URL
     */
    private String productDetailURL;

    /**
     * 商品图片URL
     */
    private String imgUrl;

    /**
     * json格式，商品扩展字段
     */
    private String extendedInfo;

}
