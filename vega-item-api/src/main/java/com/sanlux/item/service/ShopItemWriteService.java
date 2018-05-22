package com.sanlux.item.service;

import com.sanlux.item.dto.RichShopItem;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:cp
 * Created on 8/10/16.
 */
public interface ShopItemWriteService {

    /**
     * 创建店铺商品及其sku
     *
     * @param richShopItem 店铺商品及其sku
     * @return 店铺商品id
     */
    Response<Long> create(RichShopItem richShopItem);

    /**
     * 批量创建更新店铺商品及SKU
     * @param shopItems shopItems
     * @param shopSkus shopSkus
     * @return Boolean
     */
    Response<Boolean> batchCreateAndUpdate(List<ShopItem> shopItems, List<ShopSku> shopSkus);

    /**
     * 根据导入表格,运营后台批量修改待审核状态商品的销售价格
     *
     * @param rawData 导入数据
     * @return 更新的商品Ids
     */
    Response<List<Long>> batchUpdatePriceByExcel(UploadRaw rawData);

}
