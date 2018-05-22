package com.sanlux.category.impl.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.category.model.FrontCategory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by jesselu on 2017/2/9.
 */
@Repository
public class FrontCategoryExtDao extends MyBatisDao<FrontCategory> {
    public Boolean updatePidAndLevel (Long pid, Integer level, Long id) {
        return getSqlSession().update(sqlId("updatePidAndLevel"),
                ImmutableMap.of("pid", pid, "level", level, "id", id))==1;
    }

    public List<FrontCategory> findCategoryByIdAndHasChildren (Long id) {
        return getSqlSession().selectList(sqlId("findCategoryByIdAndHasChildren"),
                ImmutableMap.of("id", id));
    }

    public Long checkCategoryByIdAndLevelAndHasChildren (Map<String, Object> criteria) {
        return getSqlSession().selectOne(sqlId("checkCategoryByIdAndLevelAndHasChildren"), criteria);
    }
}
