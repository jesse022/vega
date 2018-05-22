package com.sanlux.item.service;

import com.sanlux.item.dto.ShopSkuPrice;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Sku;

import java.util.List;

/**
 * Author:cp
 * Created on 8/4/16.
 */
public interface ShopSkuWriteService {

    /**
     * 创建店铺sku,如果对应的店铺商品不存在,则同时创建之
     *
     * @param shopSku 待创建的店铺sku
     * @return 生成的shopSku id
     */
    Response<Long> create(ShopSku shopSku);

    /**
     * 更新店铺sku的库存
     *
     * @param shopId 店铺id
     * @param skuId  sku id
     * @param delta  变更的库存,正数表示增加,负数表示减少
     * @return 是否更新成功
     */
    Response<Boolean> updateStockQuantity(Long shopId, Long skuId, Integer delta);

    /**
     * 更新店铺sku状态
     *
     * @param shopId 店铺id
     * @param skuId  sku id
     * @param status 新状态
     * @return 是否更新成功
     */
    Response<Boolean> updateStatus(Long shopId, Long skuId, Integer status);

    /**
     * 批量设置店铺sku价格
     *
     * @param shopSkuPrices 店铺sku价格列表
     * @return 是否设置成功
     */
    Response<Boolean> batchSetPrice(List<ShopSkuPrice> shopSkuPrices);

    /**
     * 根据ID物理删除店铺SKU
     * @param id  ShopSkuId
     * @return Boolean
     */
    Response<Boolean> deleteById(Long id);

    /**
     * 批量更新店铺SKU库存
     * @param shopSkus sku
     * @return 更新结果
     */
    Response<Boolean> batchUpdateStockByShopIdAndSkuId(List<ShopSku> shopSkus);

    /**
     * 批量操作(同步库存使用)
     * @param toCreateShopItems 需要创建的item
     * @param toCreateShopSkus 需要创建sku
     * @param toUpdateShopSkus 需要更新的sku
     * @return 结果
     */
    Response<Boolean> batchCreateAndUpdate(List<ShopItem> toCreateShopItems,
                                           List<ShopSku> toCreateShopSkus,
                                           List<ShopSku> toUpdateShopSkus);

    /**
     * 根据excel数据,创建或更新经销商商品库存
     * @param shopId shopId
     * @param rawData rawData
     * @return Boolean
     */
    Response<Boolean> uploadToImportRaw (Long shopId, UploadRaw rawData);


    /**
     * 经销商类目授权变更后,自动创建或更新经销商商品信息
     * 主要规则:
     * 1)商品信息不做删除操作,如授权类目取消后,原先已经添加的商品信息不会删除
     * 2)商品信息更新时,库存和运费模板不变
     * 3)商品信息新增时,库存默认为0,运费模板为店铺默认运费模板,没有默认模板就赋值为null
     * @param shopId 店铺ID
     * @param skus 商品SKU信息
     * @return Boolean
     */
    Response<Boolean> batchUploadByCategoryAuth (Long shopId,List<Sku> skus);
}
