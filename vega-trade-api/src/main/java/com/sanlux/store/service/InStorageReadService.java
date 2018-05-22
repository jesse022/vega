package com.sanlux.store.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.store.model.SonEntryGodown;


/**
 * Created by lujm on 2017/3/1.
 */
public interface InStorageReadService {
    /**
     * 根据入库单号分页查询入库子单明细
     * @param entryGodownId 入库单号
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return 入库子单信息
     */
    Response<Paging<SonEntryGodown>> pagingByEntryGodownId(Long entryGodownId,Integer pageNo, Integer pageSize);
}
