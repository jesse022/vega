package com.sanlux.item.dto;

import com.sanlux.item.model.ShopItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liangfujie on 16/10/25
 */
@Data
public class VegaItemStockDto implements Serializable {
    private static final long serialVersionUID = -2364318617555404815L;
    private ShopItem shopItem;
    private Integer itemStockQuantity;

}
