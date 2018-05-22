package com.sanlux.item.service;

import io.terminus.common.model.Response;
import io.terminus.parana.item.dto.FullItem;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/10/24
 */
public interface VegaItemWriteService {

    /**
     * 根据categoryId更新商品图片
     * @param categoryId 类目ID
     * @param shopId 店铺ID
     * @param mainImage 商品主图URL
     * @return Integer
     */
    Response<Integer> updateImageByCategoryIdAndShopId(Long categoryId, Long shopId, String mainImage);

    /**
     * 批量更新商品详情
     * @param itemIds 商品IDs
     * @param richText 商品详情
     * @return 成功与否
     */
    Response<Boolean> batchUpdateRichText (List<Long> itemIds, String richText);


    /**
     * 批量删除店铺商品
     * add by lujm on 2017/3/21
     * @param  shopId 店铺ID
     * @param itemIds 商品IDs
     * @return 成功与否
     */
    Response<Boolean> batchDeleteItemsByShopId(Long shopId, List<Long> itemIds);


    /**
     * 更新商品库存
     *
     * @param skuId 商品id
     * @param delta  变化的销量,正数减库存,负则表示加库存
     */
    Response<Boolean> updateStockQuantity(Long skuId,Integer delta);

    /**
     * 更新商品销量
     *
     * @param skuId 商品id
     * @param delta  变化的销量,正数表示加销量,负则表示减销量
     */
    Response<Boolean> updateSaleQuantity(Long skuId,Integer delta);


    /**
     * 修改冻结状态商品的供货价格,并把商品状态设置成指定状态
     *
     * @param fullItems 商品信息
     */
    Response<Boolean> batchUpdateSellerPrice(List<FullItem> fullItems);



}
