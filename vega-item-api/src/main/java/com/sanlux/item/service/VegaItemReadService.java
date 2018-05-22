package com.sanlux.item.service;

import com.google.common.base.Optional;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.item.dto.ItemWithSkus;
import io.terminus.parana.item.model.Item;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/31
 */
public interface VegaItemReadService {

    /**
     * item paging service findBy
     * @param itemCode itemCode
     * @param itemName itemName
     * @param status status
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return Paging<Item></>
     */
    Response<Paging<Item>> findBy(String itemCode, String itemName, Integer status,  Integer pageNo, Integer pageSize);

    /**
     * 查找类目下面的制定状态商品ID
     * @param categoryId 类目ID
     * @param shopId 店铺ID
     * @param statuses 状态
     * @return List
     */
    Response<Optional<List<Long>>> findItemIdsByCategoryIdAndShopId(Long categoryId, Long shopId, List<Integer> statuses);

    /**
     * paging item with skus where item status = 0
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return Paging<ItemWithSkus>
     */
    Response<Paging<ItemWithSkus>> findItemWithSkusWaitCheck (Integer pageNo, Integer pageSize);

    /**
     * paging item with skus where item status = 0
     * @return Long
     */
    Response<Long> countItemCheck ();

    /**
     * findItemsByCategoryIds
     * @param categoryIds 叶子类目categoryIds
     * @return List<Item>
     */
    Response<List<Item>> findItemsByCategoryIds (List<Long> categoryIds);

    /**
     * 根据类目ID随机获取几条商品信息
     * @param categoryId 类目ID
     * @param limit 获取条数
     * @return 商品信息
     */
    Response<List<Item>> randFindItemsByCategoryId (Long categoryId, Integer limit);
}
