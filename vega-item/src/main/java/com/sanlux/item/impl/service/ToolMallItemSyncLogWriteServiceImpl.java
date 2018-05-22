package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.ToolMallItemSyncLogDao;
import com.sanlux.item.model.ToolMallItemSyncLog;
import com.sanlux.item.service.ToolMallItemSyncLogWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/4/18.
 */
@Slf4j
@Service
@RpcProvider
public class ToolMallItemSyncLogWriteServiceImpl implements ToolMallItemSyncLogWriteService {
    private final ToolMallItemSyncLogDao toolMallItemSyncLogDao;

    @Autowired
    public ToolMallItemSyncLogWriteServiceImpl(ToolMallItemSyncLogDao toolMallItemSyncLogDao) {
        this.toolMallItemSyncLogDao = toolMallItemSyncLogDao;
    }

    @Override
    public Response<Boolean> create(ToolMallItemSyncLog toolMallItemSyncLog) {
        try {
            return Response.ok(toolMallItemSyncLogDao.create(toolMallItemSyncLog));
        } catch (Exception e) {
            log.error("create toolMall item sync log failed, toolMallItemSyncLog:{}, cause:{}", toolMallItemSyncLog, Throwables.getStackTraceAsString(e));
            return Response.fail("toolMall.item.sync.log.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ToolMallItemSyncLog toolMallItemSyncLog) {
        try {
            return Response.ok(toolMallItemSyncLogDao.update(toolMallItemSyncLog));
        } catch (Exception e) {
            log.error("update toolMall item sync log failed, toolMallItemSyncLog:{}, cause:{}",
                    toolMallItemSyncLog, Throwables.getStackTraceAsString(e));
            return Response.fail("toolMall.item.sync.log.update.fail");
        }
    }
}
