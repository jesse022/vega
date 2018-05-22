package com.sanlux.youyuncai.service;

import com.sanlux.youyuncai.model.VegaItemSync;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 集乘网商品对外同步表写服务接口
 * Created by lujm on 2018/1/31.
 */
public interface VegaItemSyncWriteService {

    /**
     * 新增
     * @param vegaItemSync 同步信息
     * @return 新增Id
     */
    Response<Boolean> create(VegaItemSync vegaItemSync);


    /**
     * 批量新增
     * @param vegaItemSyncs 同步信息
     * @return 新增Id
     */
    Response<Integer> creates(List<VegaItemSync> vegaItemSyncs);


    /**
     * 更新
     * @param vegaItemSync 同步信息
     * @return 更新结果
     */
    Response<Boolean> update(VegaItemSync vegaItemSync);

    /**
     * 根据渠道,类型,主键更新
     * @param vegaItemSync 同步信息
     * @return 更新结果
     */
    Response<Boolean> updateByChannelAndTypeAndSyncId(VegaItemSync vegaItemSync);
}
