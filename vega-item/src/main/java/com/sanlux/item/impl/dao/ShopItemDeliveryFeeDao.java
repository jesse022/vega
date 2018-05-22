package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.item.model.ShopItemDeliveryFee;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:cp
 * Created on 8/16/16.
 */
@Repository
public class ShopItemDeliveryFeeDao extends MyBatisDao<ShopItemDeliveryFee> {

    public ShopItemDeliveryFee findByShopIdAndItemId(Long shopId, Long itemId) {
        return getSqlSession().selectOne(sqlId("findByShopIdAndItemId"),
                ImmutableMap.of("shopId", shopId, "itemId", itemId));
    }

    public boolean hasBoundTemplate(Long deliveryFeeTemplateId) {
        Long count = getSqlSession().selectOne(sqlId("countByTemplateId"), deliveryFeeTemplateId);
        return count > 0;
    }

    public List<ShopItemDeliveryFee> findByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        return getSqlSession().selectList(sqlId("findByShopIdAndItemIds"),
                ImmutableMap.of("shopId", shopId, "itemIds", itemIds));
    }
}
