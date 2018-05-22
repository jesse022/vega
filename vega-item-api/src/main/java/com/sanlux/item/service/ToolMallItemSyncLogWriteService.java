package com.sanlux.item.service;

import com.sanlux.item.model.ToolMallItemSyncLog;
import io.terminus.common.model.Response;

/**
 * 土猫网数据同步日志读服务
 * Created by lujm on 2018/4/18.
 */
public interface ToolMallItemSyncLogWriteService {

    Response<Boolean> create(ToolMallItemSyncLog toolMallItemSyncLog);

    Response<Boolean> update(ToolMallItemSyncLog toolMallItemSyncLog);
}
