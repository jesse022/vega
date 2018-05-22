package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.item.model.VegaBrandExtra;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * 品牌扩展信息Dao
 * Created by lujm on 2018/1/22.
 */
@Repository
public class VegaBrandExtraDao extends MyBatisDao<VegaBrandExtra> {

    /**
     * 根据品牌Id获取品牌扩展信息
     * @param brandId 品牌Id
     * @return 品牌扩展信息
     */

    public VegaBrandExtra findByBrandId(Long brandId) {
        return getSqlSession().selectOne(sqlId("findByBrandId"), brandId);
    }

    /**
     * 根据品牌Id修改品牌介绍详情
     * @param brandId 品牌Id
     * @return 是否成功
     */
    public Boolean updateByBrandId (Long brandId, String detail) {
        return getSqlSession().update(sqlId("updateByBrandId"),
                ImmutableMap.of("brandId", brandId, "detail", detail)) > 0;
    }

}
