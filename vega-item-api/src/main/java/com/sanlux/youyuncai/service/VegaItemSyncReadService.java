package com.sanlux.youyuncai.service;

import com.sanlux.youyuncai.model.VegaItemSync;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 集乘网商品对外同步表读服务接口
 * Created by lujm on 2018/1/31.
 */
public interface VegaItemSyncReadService {
    /**
     * 通过渠道,类型,主键查找
     * @param channel 同步渠道
     * @param type    类型
     * @param syncId  同步信息主键Id
     * @return 同步信息
     */
    Response<VegaItemSync> findByChannelAndTypeAndSyncId(Integer channel, Integer type, Long syncId);


    /**
     * 通过渠道,类型,主键查找
     * @param channel  同步渠道
     * @param type     类型
     * @param syncIds  同步信息主键Ids
     * @return 同步信息
     */
    Response<List<VegaItemSync>> findByChannelAndTypeAndSyncIds(Integer channel, Integer type, List<Long> syncIds);
}
