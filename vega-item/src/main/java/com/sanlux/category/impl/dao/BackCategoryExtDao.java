package com.sanlux.category.impl.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import io.terminus.parana.category.model.BackCategory;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by lujm on 2018/1/30.
 */
@Repository
public class BackCategoryExtDao extends MyBatisDao<BackCategory> {

    /**
     * 查询后台类目信息(剔除已经同步过的记录)
     * @param criteria 查询条件
     * @return backCategoryList
     */
    public Paging<BackCategory> pagingByNotSync(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        Long total = getSqlSession().selectOne(sqlId("countByNotSync"), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<BackCategory>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        List<BackCategory> datas = getSqlSession().selectList(sqlId("pagingByNotSync"), criteria);
        return new Paging<>(total, datas);
    }
}
