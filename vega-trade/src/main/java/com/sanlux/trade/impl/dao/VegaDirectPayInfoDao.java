package com.sanlux.trade.impl.dao;

import com.sanlux.trade.model.VegaDirectPayInfo;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/10/28
 */
@Repository
public class VegaDirectPayInfoDao extends MyBatisDao<VegaDirectPayInfo> {
    /**
     * 获得指定状态的银联订单
     *
     * @param status 订单状态
     * @return 银联订单
     */
    public List<VegaDirectPayInfo> findManyByStatus(Integer status) {

        return getSqlSession().selectList(sqlId("findManyByStaus"), status);

    }

    /**
     * 根据业务流程号查询银联订单
     * @param businessId 业务流程号
     * @return 银联订单
     */

    public VegaDirectPayInfo findByBusinessId(String businessId) {
        Map<String,Object> maps = new HashMap<String,Object>();
        maps.put("businessId",businessId);
        return getSqlSession().selectOne(sqlId("findByBusinessId"), maps);
    }

    /**
     * 根据业务流程号更新银联订单状态
     * @param businessId 业务流程号
     * @param newStatus 订单新状态
     * @return 是否更新成功
     */

    public Boolean updateStatusByBusinessId(String businessId, Integer newStatus) {
        Map<String,Object> maps = new HashMap<String,Object>();
        maps.put("businessId",businessId);
        maps.put("newStatus",newStatus);
        return getSqlSession().update(sqlId("updateStatusByBusinessId"), maps) > 0;
    }



    /**
     * 根据ID查询银联订单
     * @param orderId ID
     * @return 银联订单
     */

    public VegaDirectPayInfo findByOrderId(Long orderId) {
        Map<String,Object> maps = new HashMap<String,Object>();
        maps.put("orderId",orderId);
        return getSqlSession().selectOne(sqlId("findByOrderId"), maps);
    }

}
