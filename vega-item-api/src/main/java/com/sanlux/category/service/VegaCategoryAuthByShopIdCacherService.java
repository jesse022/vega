package com.sanlux.category.service;

import com.google.common.base.Optional;
import com.sanlux.category.model.CategoryAuthe;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
public interface VegaCategoryAuthByShopIdCacherService {

    /**
     * 根据店铺ID从缓存中获取授权类目表
     * @param shopId 店铺ID
     * @return 授权类目表ID
     */
    Response<Optional<CategoryAuthe>> findByShopId(Long shopId);


    /**
     * modify by lujm on 2017/03/31
     * 修改内容:先匹配二级经销商,匹配不到在匹配一级经销商
     * 根据下单的itemIds和在配送范围内的经销商List确定一家一级经销商接单或者平台接单
     * @param shopIds 在配送范围内的经销商店铺List
     * @param itemIds itemIds
     * @return 一级经销商接单返回shopI'd,平台返回空
     */
    Response<Optional<Long>> findShopIdForReceiveOrder(List<Long> shopIds, List<Long> itemIds);

    /**
     * 根据商品ID和配送范围内的经销商店铺List确定一家一级的经销商或者平台
     * @param shopIds 在配送范围内的经销商店铺List
     * @param itemId 商品ID
     * @return 一级经销商店铺ID或者平台
     */
    Response<Optional<Long>> findShopIdForItem(List<Long> shopIds, Long itemId);

    /**
     * 根据ID批量从缓存中失效授权类目及折扣
     * @param shopIds shopIds
     * @return Boolean
     */
    Response<Boolean> invalidByShopIds(List<Long> shopIds);

}
