package com.sanlux.item.dto.api;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/16.
 */
@Data
public class ItemCreateApiDto implements Serializable {

    private static final long serialVersionUID = -4171021609531253204L;

    /**
     * 类目Id
     */
    @NotEmpty(message = "类目ID不能为空")
    private String categoryId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 商品名称
     */
    @NotEmpty(message = "商品名称不能为空")
    private String name;

    /**
     * 商品外部Id
     */
    @NotEmpty(message = "商品外部ID不能为空")
    private String itemOutId;

    /**
     * 商标代号
     */
    private String brandCode = "无";

    /**
     * 商标名称
     */
    private String trademarkName = "无";

    /**
     * 计量单位
     */
    @NotEmpty(message = "计量单位不能为空")
    private String unitMeasure;

    /**
     * 计量副单位
     */
    private String assistantUnit = "无";

    /**
     * 商品详情
     */
    @NotEmpty(message = "商品详情不能为空")
    private String itemDetail;

    /**
     * 图片数组
     */
    @NotEmpty(message = "商品图片不能为空")
    private String[] imagesArray;

    /**
     * 商品外部链接
     */
    @NotEmpty(message = "商品外部链接不能为空")
    private String itemOutUrl;

    /**
     * 非销售属性
     */
    private List<ItemAttributesDto> normalAttrs;

    /**
     * 商品sku信息
     */
    @Valid
    @NotNull
    private List<SkuCreateApiDto> children;

}
