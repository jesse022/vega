package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.Item;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/10/21
 */
@Repository
public class ItemExtDao extends MyBatisDao<Item> {

    public Integer updateImageByCategoryIdAndShopId (Long categoryId, Long shopId, String mainImage) {
        return getSqlSession().update(sqlId("updateImageByCategoryIdAndShopId"),
                ImmutableMap.of("categoryId", categoryId, "mainImage", mainImage, "shopId", shopId));
    }

    public List<Long> findItemIdsByCategoryIdAndShopId (Long categoryId, Long shopId, List<Integer> statuses) {
        return getSqlSession().selectList(sqlId("findItemIdsByCategoryIdAndShopId"),
                ImmutableMap.of("categoryId", categoryId, "shopId", shopId, "statuses", statuses));
    }

    public Integer batchUpdateItemInfoMd5(List<Item> items) {
        return getSqlSession().update(sqlId("batchUpdateItemInfoMd5"), items);
    }

    public Integer batchUpdateStockByShopIdAndId (List<Item> items) {
        return getSqlSession().update(sqlId("batchUpdateStockByShopIdAndId"), items);
    }

    public List<Item> findItemsByCategoryIds (List<Long> categoryIds) {
        return getSqlSession().selectList(sqlId("findItemsByCategoryIds"),
                ImmutableMap.of("categoryIds", categoryIds));
    }
    public Long countItemWaitCheck (Integer status) {
        return getSqlSession().selectOne(sqlId("countItemWaitCheck"), ImmutableMap.of("status",status));
    }

    /**
     * 修改冻结状态商品的供货价格,并把商品状态设置成指定状态
     *
     * @param item item
     * @return 是否成功
     */
    public Boolean updateSellerPriceById (Item item) {
        return getSqlSession().update(sqlId("updateSellerPriceById"), item) > 0;
    }

    public List<Item> randFindItemsByCategoryId (Long categoryId, Integer limit) {
        return getSqlSession().selectList(sqlId("randFindItemsByCategoryId"),
                ImmutableMap.of("categoryId", categoryId, "limit", limit));
    }
}
