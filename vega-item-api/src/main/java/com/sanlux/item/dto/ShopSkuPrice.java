package com.sanlux.item.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:cp
 * Created on 8/11/16.
 */
@Data
public class ShopSkuPrice implements Serializable {

    private static final long serialVersionUID = -128918058376114587L;

    private Long shopId;

    private Long itemId;

    private Long skuId;

    private Integer price;
}
