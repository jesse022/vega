package com.sanlux.item.service;

import com.google.common.base.Optional;
import com.sanlux.item.dto.RichShopSku;
import com.sanlux.item.dto.ShopSkuCriteria;
import com.sanlux.item.model.ShopSku;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:cp
 * Created on 8/4/16.
 */
public interface ShopSkuReadService {

    /**
     * 根据店铺id和sku id查询店铺sku信息
     *
     * @param shopId 店铺id
     * @param skuId  sku id
     * @return 店铺sku信息
     */
    Response<Optional<ShopSku>> findByShopIdAndSkuId(Long shopId, Long skuId);

    /**
     * 根据店铺id查询商品的所有店铺sku的详细信息
     *
     * @param shopId 店铺id
     * @param itemId 商品 id
     * @return 店铺sku详细信息
     */
    Response<List<RichShopSku>> findShopSkuDetail(Long shopId, Long itemId);

    /**
     * 根据店铺id和item id查询店铺sku信息
     * @param shopId shopId
     * @param itemId itemId
     * @return List<ShopSku>
     */
    Response<Optional<List<ShopSku>>> findByShopIdAndItemId(Long shopId, Long itemId);

    /**
     * 根据店铺id和sku id查询店铺 sku信息
     * @param shopId shopI'd
     * @param skuIds skuId List
     * @return List<ShopSku></>
     */
    Response<List<ShopSku>> findByShopIdAndSkuIds(Long shopId, List<Long> skuIds);

    /**
     * 根据
     * @param shopId 店铺ID
     * @return skuList
     */
    Response<Paging<ShopSku>> pagingByShopId(Integer pageNo, Integer pageSize, Long shopId);

    /**
     * 根据店铺id和item ids查询店铺sku信息
     * @param shopId shopId
     * @param itemIds itemIds
     * @return List<ShopSku>
     */
    Response<Optional<List<ShopSku>>> findByShopIdAndItemIds(Long shopId, List<Long> itemIds);

    /**
     * pagingShopSku
     * @param shopSkuCriteria  shopSkuCriteria
     * @return Paging<ShopSku>
     */
    Response<Paging<ShopSku>> shopSkuPaging (ShopSkuCriteria shopSkuCriteria);
}
