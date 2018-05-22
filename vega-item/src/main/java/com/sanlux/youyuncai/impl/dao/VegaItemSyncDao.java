package com.sanlux.youyuncai.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.sanlux.youyuncai.model.VegaItemSync;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 集乘网商品对外同步Dao
 * Created by lujm on 2018/1/31.
 */
@Repository
public class VegaItemSyncDao extends MyBatisDao<VegaItemSync> {

    /**
     * 根据渠道,类型,主键查找同步信息
     * @param channel   同步渠道
     * @param type      同步类型
     * @param syncId    同步主键Id
     * @return 同步信息
     */
    public VegaItemSync findByChannelAndTypeAndSyncId(Integer channel, Integer type, Long syncId) {
        return getSqlSession().selectOne(sqlId("findByChannelAndTypeAndSyncId"),
                ImmutableMap.of(
                        "channel", channel,
                        "type", type,
                        "syncId", syncId
                )
        );
    }

    /**
     * 根据渠道,类型,主键查找同步信息
     * @param channel   同步渠道
     * @param type      同步类型
     * @param syncIds    同步主键Ids
     * @return 同步信息
     */
    public List<VegaItemSync> findByChannelAndTypeAndSyncIds(Integer channel, Integer type, List<Long> syncIds) {
        return getSqlSession().selectList(sqlId("findByChannelAndTypeAndSyncIds"), ImmutableMap.of(
                "channel", channel,
                "type", type,
                "syncIds", syncIds
        ));
    }

    /**
     * 根据渠道,类型,主键修改
     * @param vegaItemSync 修改同步信息
     * @return 是否成功
     */
    public Boolean updateByChannelAndTypeAndSyncId(VegaItemSync vegaItemSync) {
        return getSqlSession().update(sqlId("updateByChannelAndTypeAndSyncId"), vegaItemSync) == 1;
    }
}
