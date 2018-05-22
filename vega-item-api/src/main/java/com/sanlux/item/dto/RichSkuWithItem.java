package com.sanlux.item.dto;

import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/3/5.
 */
@Data
public class RichSkuWithItem implements Serializable {
    private static final long serialVersionUID = 8069344198249454530L;

    private Long skuId;

    private Sku sku;

    private Integer shopSkuPrice;

    private Item item;

}
