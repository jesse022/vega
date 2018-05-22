package com.sanlux.trade.dto;

import com.sanlux.trade.model.PurchaseSkuOrder;
import io.terminus.parana.item.model.Sku;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/9/16
 * Time: 9:10 PM
 */
@Data
public class RichPurchaseSkuOrder implements Serializable {


    private static final long serialVersionUID = 6404705484618767622L;

    private PurchaseSkuOrder purchaseSkuOrder;//采购商品信息
    private Sku sku;            // 商品详情
    private String itemName;    // 商品名
    private String itemImage;   // 商品主图
    private Integer itemStatus; // 商品状态
    private String shopName;    //店铺名称

}
