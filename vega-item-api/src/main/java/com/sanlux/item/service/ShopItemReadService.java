package com.sanlux.item.service;

import com.google.common.base.Optional;
import com.sanlux.item.model.ShopItem;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:cp
 * Created on 8/9/16.
 */
public interface ShopItemReadService {

    /**
     * 根据店铺id和商品id查询店铺商品
     *
     * @param shopId 店铺ID
     * @param itemId 商品id
     * @return 店铺商品
     */
    Response<Optional<ShopItem>> findByShopIdAndItemId(Long shopId, Long itemId);

    /**
     * 根据店铺id和商品id列表查询店铺商品列表
     *
     * @param shopId  店铺id
     * @param itemIds 商品id列表
     * @return 店铺商品列表
     */
    Response<List<ShopItem>> findByShopIdAndItemIds(Long shopId, List<Long> itemIds);

    /**
     * 分页查询店铺商品
     *
     * @param shopId   店铺ID
     * @param itemId   商品Id,若为空则查询所有商品
     * @param itemName 商品名称,若为空则查询所有商品
     * @param pageNo   页码
     * @param pageSize 每页大小
     * @return 店铺商品
     */
    Response<Paging<ShopItem>> findBy(Long shopId, Long itemId, String itemName, Integer pageNo, Integer pageSize);

}
