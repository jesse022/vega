package com.sanlux.user.impl.dao;

import com.google.common.collect.Maps;
import com.sanlux.user.model.ShopUserExtras;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

/**
 * 经销商专属会员扩展信息表Dao类
 *
 * Created by lujm on 2017/9/1.
 */
@Repository
public class ShopUserExtrasDao extends MyBatisDao<ShopUserExtras> {
    /**
     * 根据会员用户ID获取经销商会员信息
     * @param userId 用户Id
     * @return 会员用户信息
     */
    public ShopUserExtras findByUserId(Long userId) {
        return getSqlSession().selectOne(sqlId("findByUserId"), userId);
    }

    /**
     * 通过手机号获取经销商会员信息
     * @param mobile 手机号
     * @return 经销商会员信息
     */
    public ShopUserExtras findByMobile(String mobile) {

        return getSqlSession().selectOne(sqlId("findByMobile"), mobile);
    }


    /**
     * 刷新经销商专属会员信息
     * @param userId 会员用户Id
     * @param mobile 会员手机号
     * @param userName 会员用户名
     * @param userType 会员用户类型
     * @param userStatus 会员用户状态
     * @return 是否成功
     */
    public Boolean refreshShopUserByUserId(Long userId,String mobile,String userName,
                                           Integer userType, Integer userStatus){
        HashMap<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);
        params.put("mobile", mobile);
        params.put("userName", userName);
        params.put("userType", userType);
        params.put("userStatus", userStatus);
        return (this.sqlSession.update(sqlId("refreshShopUserByUserId"), params) > 0);
    }
}
