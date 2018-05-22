package com.sanlux.user.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sanlux.user.model.ServiceManagerUser;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.terminus.common.utils.Arguments.isEmpty;

/**
 * 业务经理会员表Dao类
 *
 * Created by lujm on 2017/5/24.
 */
@Repository
public class ServiceManagerUserDao extends MyBatisDao<ServiceManagerUser> {

    /**
     * 通过业务经理ID获得业务经理会员信息
     * @param serviceManagerId 业务经理ID
     * @return ServiceManagerUser
     */
    public List<ServiceManagerUser> findByServiceManagerId(Long serviceManagerId) {
        return this.sqlSession.selectList(sqlId("findByServiceManagerId"), ImmutableMap.of("serviceManagerId", serviceManagerId));
    }


    /**
     * 通过业务经理Ids获得业务经理会员信息
     * @param serviceManagerIds 业务经理Ids
     * @return List<ServiceManagerUser>
     */
    public List<ServiceManagerUser> findByServiceManagerIds(List<Long> serviceManagerIds){
        if (isEmpty(serviceManagerIds)) {
            return Collections.emptyList();
        }
        return sqlSession.selectList(sqlId("findByServiceManagerIds"), serviceManagerIds);
    }


    /**
     * 通过用户ID获得业务经理会员信息
     * @param userId 用户ID
     * @return ServiceManagerUser
     */
    public List<ServiceManagerUser> findByUserId(Long userId) {
        return getSqlSession().selectList(sqlId("findByUserId"), userId);
    }

    /**
     * 通过手机号获取业务经理会员信息
     * @param mobile 手机号
     * @return 经销商用户
     */
    public ServiceManagerUser findByMobile(String mobile) {
        return getSqlSession().selectOne(sqlId("findByMobile"), mobile);
    }

    /**
     * 通过手机号获取业务经理会员信息
     * @param mobile 手机号
     * @return 经销商用户列表
     */
    public List<ServiceManagerUser> findListByMobile(String mobile){
        return sqlSession.selectList(sqlId("findListByMobile"), mobile);
    }


    /**
     * 根据业务经理ID删除会员
     * @param serviceManagerId 业务经理ID
     * @return 删除是否成功
     */

    public Boolean deleteByServiceManagerId(Long serviceManagerId) {
        return getSqlSession().selectOne(sqlId("deleteByServiceManagerId"), serviceManagerId);

    }

    /**
     * 根据用户ID删除会员
     * @param userId 用户ID
     * @return 删除是否成功
     */

    public Boolean deleteByUserId(Long userId) {
        return getSqlSession().selectOne(sqlId("deleteByUserId"), userId);

    }

    /**
     * 刷新会员信息
     * @param userId 用户ID
     * @param mobile 手机号码
     * @param userName 用户名
     * @return 是否成功
     */
    public Boolean refreshServiceManagerUserByUserId(Long userId,String mobile,String userName){
        Map<String, Object> map = Maps.newHashMap();
        map.put("userId", userId);
        map.put("mobile", mobile);
        map.put("userName", userName);
        return (this.sqlSession.update(sqlId("refreshServiceManagerUserByUserId"), map) > 0);
    }

}
