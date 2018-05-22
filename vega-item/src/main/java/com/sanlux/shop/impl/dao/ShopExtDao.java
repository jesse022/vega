package com.sanlux.shop.impl.dao;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.shop.model.Shop;
import org.springframework.stereotype.Repository;

/**
 * Created by lujm on 2017/3/14.
 */
@Repository
public class ShopExtDao extends MyBatisDao<Shop> {
    /**
     * 更新对象
     * @param shop 店铺信息
     * @return 更新记录数
     */
    public Boolean updateShopStatus(Shop shop){
        return sqlSession.update(sqlId("updateShopStatus"), shop) == 1;
    }
}
