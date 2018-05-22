package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.ToolMallItemSyncDao;
import com.sanlux.item.model.ToolMallItemSync;
import com.sanlux.item.service.ToolMallItemSyncWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/4/19.
 */
@Slf4j
@Service
@RpcProvider
public class ToolMallItemSyncWriteServiceImpl implements ToolMallItemSyncWriteService {
    private final ToolMallItemSyncDao toolMallItemSyncDao;

    @Autowired
    public ToolMallItemSyncWriteServiceImpl(ToolMallItemSyncDao toolMallItemSyncDao) {
        this.toolMallItemSyncDao = toolMallItemSyncDao;
    }

    @Override
    public Response<Boolean> create(ToolMallItemSync toolMallItemSync) {
        try {
            return Response.ok(toolMallItemSyncDao.create(toolMallItemSync));
        } catch (Exception e) {
            log.error("create toolMall item sync failed, toolMallItemSync:{}, cause:{}", toolMallItemSync, Throwables.getStackTraceAsString(e));
            return Response.fail("toolMall.item.sync.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateByType(ToolMallItemSync toolMallItemSync) {
        try {
            return Response.ok(toolMallItemSyncDao.updateByType(toolMallItemSync));
        } catch (Exception e) {
            log.error("update toolMall item sync failed, toolMallItemSync:{}, cause:{}", toolMallItemSync, Throwables.getStackTraceAsString(e));
            return Response.fail("toolMall.item.sync.update.fail");
        }
    }
}
