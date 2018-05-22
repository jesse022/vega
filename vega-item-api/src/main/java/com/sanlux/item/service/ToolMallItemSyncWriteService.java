package com.sanlux.item.service;

import com.sanlux.item.model.ToolMallItemSync;
import io.terminus.common.model.Response;

/**
 * 土猫网数据同步配置表读服务
 * Created by lujm on 2018/4/19.
 */
public interface ToolMallItemSyncWriteService {
    Response<Boolean> create(ToolMallItemSync toolMallItemSync);

    Response<Boolean> updateByType(ToolMallItemSync toolMallItemSync);
}
