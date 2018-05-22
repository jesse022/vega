package com.sanlux.item.impl.dao;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.ItemDetail;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/10/24
 */
@Repository
public class ItemDetailExtDao extends MyBatisDao<ItemDetail> {

    public Integer batchUpdateItemDetail (List<ItemDetail> itemDetails) {
        return getSqlSession().update(sqlId("batchUpdateItemDetail"), itemDetails);
    }

    public List<ItemDetail> findItemDetailsByItemIds (List<Long> itemIds) {
        return getSqlSession().selectList(sqlId("findItemDetailsByItemIds"), itemIds);
    }

}
