package com.sanlux.item.service;

import com.sanlux.item.dto.RichSkuWithItem;
import com.sanlux.item.dto.RichSkus;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Sku;

import java.util.List;

/**
 * Author:cp
 * Created on 8/11/16.
 */
public interface VegaSkuReadService {

    /**
     * 根据店铺id和商品id查询带店铺sku价格的sku信息
     *
     * @param shopId 店铺id
     * @param itemId 商品id
     * @return 带店铺sku价格的sku信息
     */
    Response<RichSkus> findSkusWithShopSkuPrice(Long shopId, Long itemId);

    Response<Paging<Sku>> paging (Integer pageNo, Integer pageSize);

    /**
     * 获取店铺所有已冻结的SKU信息
     * @param shopId 店铺Id
     * @return SKU信息
     */
    Response<List<Sku>> findFrozenItemsByShopId(Long shopId);

    /**
     * 获取特定状态下所有SKU信息
     * @param status 状态
     * @return SKU信息
     */
    Response<List<Sku>> findSpeciallyStatusSkus(Integer status);


    /**
     * 根据商品Ids查询所有SKU信息(包括已经删除的),友云采对接用到
     * @param itemIds 状态
     * @return SKU信息
     */
    Response<List<Sku>> findAllByItemIds(List<Long> itemIds);

    /**
     * 根据查询SKU同步信息
     * @param status     状态
     * @param channel    同步渠道
     * @param syncStatus 同步标志(空:查询所有; 非空:剔除已经同步过的记录)
     * @return skuList
     */
    Response<Paging<Sku>> pagingBySkuSync(Integer pageNo, Integer pageSize, Integer status, Integer channel, Integer syncStatus);

    /**
     * 根据skuIds查询带店铺sku价格及item的skus信息
     *
     * @param skuIds skuIds
     * @return 带店铺sku价格的sku信息
     */
    Response<List<RichSkuWithItem>> findSkusWithItemAndShopSkuPrice(List<Long> skuIds);

    /**
     * 查询所有上架商品SKU含散客价/供货价信息,用于运营日常价格导出
     * @return SKU信息
     */
    Response<List<Sku>> findAllSkuWithPrice();

}
