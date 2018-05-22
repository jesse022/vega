package com.sanlux.user.impl.dao;

import com.sanlux.user.model.Nation;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by lujm on 2017/2/23.
 */
@Repository
public class NationDao extends MyBatisDao<Nation> {
    /**
     * 通过父code查询七鱼客服地区对照表信息
     * @param code
     * @return
     */
    public Nation findByCode(String code){
        return this.sqlSession.selectOne(sqlId("findByCode"),code);
    }
}
