package com.sanlux.item.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 经销商导入库存Dto
 * Created by cuiwentao
 * on 16/11/11
 */
@Data
public class VegaShopItemSkuDto implements Serializable {


    private static final long serialVersionUID = 8378173935836043108L;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商品ID
     */
    private Long itemId;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * SKUID
     */
    private Long skuId;

    /**
     * SKu库存
     */
    private Integer stockQuantity;

    /**
     * 运费模板
     */
    private Long deliveryFeeTemplateId;
}
