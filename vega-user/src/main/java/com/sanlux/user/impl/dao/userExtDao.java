package com.sanlux.user.impl.dao;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.user.model.User;
import org.springframework.stereotype.Repository;

/**
 * Created by lujm on 2017/3/13.
 */
@Repository
public class userExtDao extends MyBatisDao<User> {
    /**
     * 更新对象
     * @param user 用户信息
     * @return 更新记录数
     */
    public Boolean updateUserRoles(User user){
        return sqlSession.update(sqlId("updateUserRoles"), user) == 1;
    }
}
