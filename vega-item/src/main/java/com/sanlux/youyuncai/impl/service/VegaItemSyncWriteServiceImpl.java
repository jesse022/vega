package com.sanlux.youyuncai.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.youyuncai.impl.dao.VegaItemSyncDao;
import com.sanlux.youyuncai.model.VegaItemSync;
import com.sanlux.youyuncai.service.VegaItemSyncWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lujm on 2018/1/31.
 */
@Slf4j
@Service
@RpcProvider
public class VegaItemSyncWriteServiceImpl implements VegaItemSyncWriteService {

    @Autowired
    private VegaItemSyncDao vegaItemSyncDao;

    @Override
    public Response<Boolean> create(VegaItemSync vegaItemSync) {
        try {
            return Response.ok(vegaItemSyncDao.create(vegaItemSync));
        }catch (Exception e) {
            log.error("failed to create item sync info : ({}), vegaItemSync : ({}), cause : {}", vegaItemSync, Throwables.getStackTraceAsString(e));
            return Response.fail("create.item.sync.info.failed");
        }
    }

    @Override
    public Response<Integer> creates(List<VegaItemSync> vegaItemSyncs) {
        try {
            return Response.ok(vegaItemSyncDao.creates(vegaItemSyncs));
        }catch (Exception e) {
            log.error("failed to create item sync info : ({}), vegaItemSyncs : ({}), cause : {}", vegaItemSyncs, Throwables.getStackTraceAsString(e));
            return Response.fail("create.item.sync.info.failed");
        }
    }

    @Override
    public Response<Boolean> update(VegaItemSync vegaItemSync) {
        try {
            return Response.ok(vegaItemSyncDao.update(vegaItemSync));
        }catch (Exception e) {
            log.error("failed to update item sync info : ({}), vegaItemSync : ({}), cause : {}", vegaItemSync, Throwables.getStackTraceAsString(e));
            return Response.fail("update.item.sync.info.failed");
        }
    }

    @Override
    public Response<Boolean> updateByChannelAndTypeAndSyncId(VegaItemSync vegaItemSync) {
        try {
            return Response.ok(vegaItemSyncDao.updateByChannelAndTypeAndSyncId(vegaItemSync));
        }catch (Exception e) {
            log.error("failed to update item sync info by channel : ({}) and type : ({}) and sync : ({}), " +
                    "vegaItemSync : ({}), cause : {}", vegaItemSync.getChannel(), vegaItemSync.getType(),
                    vegaItemSync.getSyncId(), Throwables.getStackTraceAsString(e));
            return Response.fail("update.item.sync.info.failed");
        }
    }
}
