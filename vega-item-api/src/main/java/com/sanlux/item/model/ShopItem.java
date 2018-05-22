package com.sanlux.item.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺商品
 * Author:cp
 * Created on 8/9/16
 */
@Data
public class ShopItem implements Serializable {

    private static final long serialVersionUID = 1749984236824714975L;

    /**
     * ID
     */
    private Long id;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 商品id
     */
    private Long itemId;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
