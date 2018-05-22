package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.YouyuncaiOrderDao;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.YouyuncaiOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/3/9.
 */
@Service
@RpcProvider
@Slf4j
public class YouyuncaiOrderWriteServiceImpl implements YouyuncaiOrderWriteService {
    private final YouyuncaiOrderDao youyuncaiOrderDao;

    @Autowired
    public YouyuncaiOrderWriteServiceImpl (YouyuncaiOrderDao youyuncaiOrderDao) {
        this.youyuncaiOrderDao = youyuncaiOrderDao;
    }

    @Override
    public Response<Boolean> create(YouyuncaiOrder youyuncaiOrder) {
        try {
            return Response.ok(youyuncaiOrderDao.create(youyuncaiOrder));
        } catch (Exception e) {
            log.error("create you yun cai order failed, youyuncaiOrder:{}, cause:{}", youyuncaiOrder, Throwables.getStackTraceAsString(e));
            return Response.fail("create.youyuncai.order.fail");
        }
    }

    @Override
    public Response<Boolean> update(YouyuncaiOrder youyuncaiOrder) {
        try {
            return Response.ok(youyuncaiOrderDao.update(youyuncaiOrder));
        } catch (Exception e) {
            log.error("update you yun cai order failed, youyuncaiOrder:{}, cause:{}", youyuncaiOrder, Throwables.getStackTraceAsString(e));
            return Response.fail("update.youyuncai.order.fail");
        }
    }
}
