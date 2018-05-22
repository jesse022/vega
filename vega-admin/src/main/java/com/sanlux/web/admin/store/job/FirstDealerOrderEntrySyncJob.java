/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.job;

import com.sanlux.web.front.core.store.VegaStorageEntrySyncWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 同步一级经销商购买供应商订单对应的入库单
 *
 * @author : panxin
 */
@Slf4j
public class FirstDealerOrderEntrySyncJob {

    @Autowired
    private VegaStorageEntrySyncWriter vegaStorageEntrySyncWriter;

    public void onSync() {
        log.info("入库单同步 --> 一级经销商进货");
//        Response<Boolean> resp = vegaStorageEntrySyncWriter.syncFirstDealerOrder(VegaOrderStatus.CONFIRMED);
//        if (!resp.isSuccess()) {
//            log.error("failed to sync first dealer entry storage, cause : {}", resp.getError());
//        }
    }

}
