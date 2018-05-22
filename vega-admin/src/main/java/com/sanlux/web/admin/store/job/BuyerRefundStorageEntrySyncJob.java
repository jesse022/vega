/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.store.job;

import lombok.extern.slf4j.Slf4j;

/**
 * 买家退款对应的入库单状态同步 Job
 *
 * @author : panxin
 */
@Slf4j
public class BuyerRefundStorageEntrySyncJob {

    public void onSync() {
        log.info("入库单同步 --> 普通用户退货");
    }

}
