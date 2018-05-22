package com.sanlux.store.impl.dao;

import com.sanlux.store.model.StoreImports;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * 经销存批量导入日志表
 * Created by lujm on 2017/3/15.
 */
@Repository
public class StoreImportsDao extends MyBatisDao<StoreImports> {
    /**
     * 通过批次查询
     * @param key 导入批次
     * @return 对象
     */
    public StoreImports findByKey(String key){
        return sqlSession.selectOne(sqlId("findByKey"), key);
    }

    /**
     * 通过批次查询
     * @param key 导入批次
     * @return 对象
     */
    public List<StoreImports> findListByKey(String key){
        return sqlSession.selectList(sqlId("findByKey"), key);
    }
}
