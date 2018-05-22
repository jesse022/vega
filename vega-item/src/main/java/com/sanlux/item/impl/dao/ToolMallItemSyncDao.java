package com.sanlux.item.impl.dao;

import com.sanlux.item.model.ToolMallItemSync;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by lujm on 2018/4/19.
 */
@Repository
public class ToolMallItemSyncDao extends MyBatisDao<ToolMallItemSync> {

    public Boolean updateByType (ToolMallItemSync toolMallItemSync) {
        return getSqlSession().update(sqlId("updateByType"), toolMallItemSync) > 0;
    }
}
