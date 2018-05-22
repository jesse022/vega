package com.sanlux.item.component;

import com.google.common.base.MoreObjects;
import io.terminus.parana.item.api.InitialItemInfoFiller;
import io.terminus.parana.item.model.Item;

/**
 * Author:cp
 * Created on 8/8/16.
 */
public class VegaInitialItemInfoFiller implements InitialItemInfoFiller {

    @Override
    public void fill(Item item) {
        item.setStatus(0); //默认待审核
        item.setSaleQuantity(MoreObjects.firstNonNull(item.getSaleQuantity(), 0));  //默认销量为0
        item.setType(MoreObjects.firstNonNull(item.getType(), 1));   //默认为普通商品
        item.setReduceStockType(MoreObjects.firstNonNull(item.getReduceStockType(), 1)); //默认拍下减库存
        item.setStockType(MoreObjects.firstNonNull(item.getStockType(), 0));//默认不分仓存储
    }
}
