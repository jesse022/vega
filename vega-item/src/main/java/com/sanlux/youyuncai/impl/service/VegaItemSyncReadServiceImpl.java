package com.sanlux.youyuncai.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.youyuncai.impl.dao.VegaItemSyncDao;
import com.sanlux.youyuncai.model.VegaItemSync;
import com.sanlux.youyuncai.service.VegaItemSyncReadService;
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
public class VegaItemSyncReadServiceImpl implements VegaItemSyncReadService {
    @Autowired
    private VegaItemSyncDao vegaItemSyncDao;

    @Override
    public Response<VegaItemSync> findByChannelAndTypeAndSyncId(Integer channel, Integer type, Long syncId) {
        try {
            return Response.ok(vegaItemSyncDao.findByChannelAndTypeAndSyncId(channel, type, syncId));
        }catch (Exception e) {
            log.error("failed to find item sync info by channel = ({}), type = ({}), syncId = ({}) " +
                    "cause : {}", channel, type, syncId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.sync.find.failed");
        }
    }

    @Override
    public Response<List<VegaItemSync>> findByChannelAndTypeAndSyncIds(Integer channel, Integer type, List<Long> syncIds) {
        try {
            return Response.ok(vegaItemSyncDao.findByChannelAndTypeAndSyncIds(channel, type, syncIds));
        }catch (Exception e) {
            log.error("failed to find item sync info by channel = ({}), type = ({}), syncIds = ({}) " +
                    "cause : {}", channel, type, syncIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.sync.find.failed");
        }
    }
}
