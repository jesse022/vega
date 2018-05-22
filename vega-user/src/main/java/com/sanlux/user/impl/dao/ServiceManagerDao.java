package com.sanlux.user.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.user.model.ServiceManager;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;


/**
 * 业务经理信息表(用户表)Dao类
 *
 * Created by lujm on 2017/5/23.
 */
@Repository
public class ServiceManagerDao extends MyBatisDao<ServiceManager> {


    /**
     * 业务经理表用户状态修改
     *
     * @param id id
     * @param status 状态
     * @return 是否成功
     */
    public Boolean updateStatus(Long id,Integer status){
        return (this.sqlSession.update(sqlId("updateStatus"), ImmutableMap.of("id", id, "status", status)) > 0);
    }

    /**
     * 通过用户ID获得业务经理信息
     * @param userId 用户ID
     * @return ServiceManager
     */
    public ServiceManager findByUserId(Long userId) {
        return getSqlSession().selectOne(sqlId("findByUserId"), userId);
    }

}
