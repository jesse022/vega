package com.sanlux.item.dto;

import com.sanlux.item.model.ShopSku;
import io.terminus.parana.item.model.Sku;
import lombok.Data;

import java.io.Serializable;

/**
 * Author:cp
 * Created on 8/10/16.
 */
@Data
public class RichShopSku implements Serializable {

    private static final long serialVersionUID = 4586923546812801952L;

    private ShopSku shopSku;

    private Sku sku;

}
