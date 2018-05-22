package com.sanlux.item.dto;

import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:cp
 * Created on 8/10/16.
 */
@Data
public class RichShopItem implements Serializable {

    private static final long serialVersionUID = 6060710491972813814L;

    private ShopItem shopItem;

    private List<ShopSku> shopSkus;

}
