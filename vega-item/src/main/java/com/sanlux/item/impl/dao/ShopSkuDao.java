package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.item.model.ShopSku;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:cp
 * Created on 8/2/16
 */
@Repository
public class ShopSkuDao extends MyBatisDao<ShopSku> {

    public List<ShopSku> findByShopIdAndItemId(Long shopId, Long itemId) {
        return getSqlSession().selectList(sqlId("findByShopIdAndItemId"),
                ImmutableMap.of("shopId", shopId, "itemId", itemId));
    }

    public ShopSku findByShopIdAndSkuId(Long shopId, Long skuId) {
        return getSqlSession().selectOne(sqlId("findByShopIdAndSkuId"),
                ImmutableMap.of("shopId", shopId, "skuId", skuId));
    }

    /**
     * 在原来的库存基础上增加 delta
     * @param id
     * @param delta
     * @return
     */
    public boolean updateStockQuantity(Long id, Integer delta) {
        return getSqlSession().update(sqlId("updateStockQuantity"),
                ImmutableMap.of("id", id, "delta", delta)) == 1;
    }

    /**
     * 更新库存,直接覆盖原来的库存
     * @param id
     * @param stockQuantity
     * @return
     */
    public boolean updateStockById(Long id , Integer stockQuantity) {
        return getSqlSession().update(sqlId("updateStockById"),
                ImmutableMap.of("id", id, "stockQuantity", stockQuantity)) == 1;
    }

    public boolean updateStatus(Long id, Integer status) {
        return getSqlSession().update(sqlId("updateStatus"),
                ImmutableMap.of("id", id, "status", status)) == 1;
    }

    public  List<ShopSku> findByShopIdAndSkuIds(Long shopId, List<Long> skuIds) {
        return getSqlSession().selectList(sqlId("findByShopIdAndSkuIds"),
                ImmutableMap.of("shopId", shopId, "skuIds", skuIds));
    }

    public Integer batchUpdateByShopIdAndSkuId(List<ShopSku> shopSkus) {
        return getSqlSession().update(sqlId("batchUpdateByShopIdAndSkuId"), shopSkus);
    }

    public List<ShopSku> findByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        return getSqlSession().selectList(sqlId("findByShopIdAndItemIds"),
                ImmutableMap.of("shopId", shopId, "itemIds", itemIds));
    }

}
