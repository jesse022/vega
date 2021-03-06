package com.sanlux.trade.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.trade.model.OrderDispatchRelation;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 订单派送关联表Dao类
 * Date: 2016-08-16
 */
@Repository
public class OrderDispatchRelationDao extends MyBatisDao<OrderDispatchRelation> {

    /**
     * 根据订单id和订单被派给某个店铺的店铺id
     * @param orderId 订单id
     * @param dispatchShopId 订单被派给某个店铺的店铺id
     * @return 订单派送关联信息
     */
    public OrderDispatchRelation findByOrderIdAndDispatchShopId(Long orderId,Long dispatchShopId){
        return getSqlSession().selectOne(sqlId("findByOrderIdAndDispatchShopId"), ImmutableMap.of("orderId",orderId,
                "dispatchShopId",dispatchShopId));

    }

}
