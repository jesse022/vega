package com.sanlux.item.dto;

import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:cp
 * Created on 8/11/16.
 */
@Data
public class RichSkus implements Serializable {

    private static final long serialVersionUID = 173149234006283688L;

    private Item item;

    private List<SkuWithShopSkuPrice> skuWithShopSkuPrices;

    @Data
    public static class SkuWithShopSkuPrice implements Serializable{

        private static final long serialVersionUID = -2728203533026037661L;

        private Sku sku;

        private Integer shopSkuPrice;
    }
}
