package com.sanlux.trade.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.trade.model.VegaRateDefs;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 三力士相关费率定义Dao
 *
 * Created by lujm on 2017/11/16.
 */
@Repository
public class VegaRateDefsDao extends MyBatisDao<VegaRateDefs> {
    /**
     * 获得指定类型的费率定义信息
     *
     * @param type 类型
     * @return 费率定义信息
     */
    public List<VegaRateDefs> findByType(Integer type) {

        return getSqlSession().selectList(sqlId("findByType"), type);

    }

    /**
     * 获得指定类型的费率定义信息
     *
     * @param type 类型
     * @return 费率定义信息
     */
    public VegaRateDefs findByTypeAndName(Integer type, String name) {

        return getSqlSession().selectOne(sqlId("findByTypeAndName"), ImmutableMap.of("type", type, "name", name));

    }
}
