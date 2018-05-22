package com.sanlux.user.impl.dao;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.user.model.UserProfile;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Created by lujm on 2018/3/14.
 */
@Repository
public class UserProfileExtDao extends MyBatisDao<UserProfile> {

    /**
     * 根据extra_json字段模糊查询用户
     * @param jsonName 名称
     * @return 查询结果
     */
    public List<UserProfile> findByExtraJson(String jsonName){
        return sqlSession.selectList(sqlId("findByExtraJson"), jsonName);
    }
}
