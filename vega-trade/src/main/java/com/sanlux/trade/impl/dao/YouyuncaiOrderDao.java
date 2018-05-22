package com.sanlux.trade.impl.dao;

import com.sanlux.trade.model.YouyuncaiOrder;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * 友云采订单Dao类
 * Created by lujm on 2018/3/9.
 */
@Repository
public class YouyuncaiOrderDao  extends MyBatisDao<YouyuncaiOrder> {

    /**
     * 根据集乘网订单Id获取友云采订单信息
     * @param orderId 集乘网订单Id
     * @return 友云采订单信息
     */
    public YouyuncaiOrder findByOrderId(Long orderId){
        return getSqlSession().selectOne(sqlId("findByOrderId"),orderId);
    }

}

