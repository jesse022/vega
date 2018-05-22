package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.item.model.ShopItem;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:cp
 * Created on 8/9/16
 */
@Repository
public class ShopItemDao extends MyBatisDao<ShopItem> {

    public ShopItem findByShopIdAndItemId(Long shopId, Long itemId) {
        return getSqlSession().selectOne(sqlId("findByShopIdAndItemId"),
                ImmutableMap.of("shopId", shopId, "itemId", itemId));
    }

    public List<ShopItem> findByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        return getSqlSession().selectList(sqlId("findByShopIdAndItemIds"),
                ImmutableMap.of("shopId", shopId, "itemIds", itemIds));
    }

}
