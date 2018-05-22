package com.sanlux.item.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/10/17
 */
@Data
public class ItemsToCreate implements Serializable {


    private static final long serialVersionUID = 8287464536771612170L;

    /**
     * 类目ID
     */
    private Long categoryId;

    /**
     * 商品名
     */
    private String name;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 商品代码(货号)
     */
    private String itemCode;

    /**
     * 外部商品ID
     */
    private String itemOuterId;

    /**
     * 外部SKUID
     */
    private String skuOuterId;

    /**
     * 商标代码
     */
    private String brandCode;

    /**
     * 对方货号
     */
    private String otherNo;

    /**
     * 商品图片
     */
    private String itemImage;

    /**
     * 副单位
     */
    private String assistantUnit;

    /**
     * 商标名称
     */
    private String trademarkName;

    /**
     * 运费模板ID
     */
    private Long deliveryFeeTemplateId;

    /**
     * 供货价
     */
    private Integer itemPrice;

    /**
     * 库存
     */
    private Integer stockQuantity;

    /**
     * 计量单位
     */
    private String unitMeasure;

    /**
     * 计量值
     */
    private Integer unitAmount;

    /**
     * 要创建的SKU
     */
    private List<ItemsToCreate> children;

    /**
     * 销售属性Map
     */
    private Map<String, String> sellAttrs;

    /**
     * 非销售属性Map
     */
    private Map<String, String> normalAttrs;

    /**
     * 商品外部系统连接
     */
    private String selfPlatformLink;

    /**
     * 商品详情
     */
    private String itemDetail;

    /**
     * 图片详情JSON
     */
    private String imagesJson;

    /**
     * 土猫网商品上下架标志
     */
    private Integer toolMallIsMarketable;


}
