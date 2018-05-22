package com.sanlux.item.dto.api;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/16.
 */
@Data
public class SkuCreateApiDto implements Serializable {

    private static final long serialVersionUID = -1097492770054646998L;
    /**
     * sku外部Id,唯一
     */
    @NotEmpty(message = "sku外部Id不能为空")
    private String skuOutId;

    /**
     * 商品供货价
     */
    @NotEmpty(message = "商品价格不能为空")
    @Pattern(regexp = "^\\+?[1-9][0-9]*$", message = "商品价格必须为数字")
    private String itemPrice;

    /**
     * 商品库存
     */
    @NotEmpty(message = "商品库存不能为空")
    @Pattern(regexp = "^\\+?[1-9][0-9]*$", message = "商品库存必须为数字")
    private String stockQuantity;

    /**
     * 计量单位数量
     */
    @Pattern(regexp = "^\\+?[1-9][0-9]*$", message = "商品计量单位数量必须为数字")
    private String unitAmount = "1";

    /**
     * 销售属性
     */
    private List<ItemAttributesDto> sellAttrs;

    /**
     * 土猫网商品上下架标志
     */
    private Integer toolMallIsMarketable;

}
