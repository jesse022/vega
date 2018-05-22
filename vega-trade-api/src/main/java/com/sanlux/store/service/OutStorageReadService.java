package com.sanlux.store.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.store.model.SonLeaveGodown;

/**
 * Created by lujm on 2017/3/1.
 */
public interface OutStorageReadService {
    /**
     * 根据出库单号分页查询入库子单明细
     * @param leaveGodownId 出库单号
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return 出库子单信息
     */
    Response<Paging<SonLeaveGodown>> pagingByLeaveGodownId(Long leaveGodownId, Integer pageNo, Integer pageSize);
    /**
     * 根据出库单号分页查询入库子单明细
     * @param sonLeaveGodown 子单信息
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return 出库子单信息
     */
    Response<Paging<SonLeaveGodown>> pagingBySonLeaveGodown(SonLeaveGodown sonLeaveGodown, Integer pageNo, Integer pageSize);
}
