/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.job;

import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.web.front.core.store.VegaStorageLeaveSyncWriter;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 出库单状态同步 Job
 *
 * @author : panxin
 */
@Slf4j
public class BuyerOrderStorageOutSyncJob {

    @Autowired
    private VegaStorageLeaveSyncWriter vegaStorageLeaveSyncWriter;

    public void onSync() {
        log.info("出库单同步 --> 普通用户购买");
        Response<Boolean> resp = vegaStorageLeaveSyncWriter.syncBuyerOrder(VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER);
        if (!resp.isSuccess()) {
            log.error("failed to sync order storage out, cause : {}", resp.getError());
        }
    }

}
