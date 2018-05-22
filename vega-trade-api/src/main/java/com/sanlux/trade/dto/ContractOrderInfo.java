package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单生成合同订单信息Dto
 * Created by lujm on 2018/1/15.
 */
@Data
public class ContractOrderInfo implements Serializable {
    private static final long serialVersionUID = 6184220185491309596L;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 商品规格型号
     */
    private String itemModel;

    /**
     * 计价单位
     */
    private String itemUnit;

    /**
     * 数量
     */
    private String itemQuantity;

    /**
     * 单价
     */
    private String itemPrice;

    /**
     * 价格合计
     */
    private String itemSumPrice;

    /**
     * 价格合计合并标注
     */
    private String sumPriceMerge;

    /**
     * 备注
     */
    private String itemRemark;

    /**
     * 备注合并标记
     */
    private String remarkMerge;
}
