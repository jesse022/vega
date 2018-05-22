package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.dto.YouyuncaiOrderCriteria;
import com.sanlux.trade.impl.dao.YouyuncaiOrderDao;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.YouyuncaiOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/3/9.
 */
@Slf4j
@RpcProvider
@Service
public class YouyuncaiOrderReadServiceImpl implements YouyuncaiOrderReadService {

    private final YouyuncaiOrderDao youyuncaiOrderDao;

    @Autowired
    public YouyuncaiOrderReadServiceImpl (YouyuncaiOrderDao youyuncaiOrderDao) {
        this.youyuncaiOrderDao = youyuncaiOrderDao;
    }

    @Override
    public Response<YouyuncaiOrder> findByOrderId(Long orderId) {
        try {
            return Response.ok(youyuncaiOrderDao.findByOrderId(orderId));
        } catch (Exception e) {
            log.error("find you yun cai order by orderId = {} failed, orderId:{}, cause:{}", orderId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.youyuncai.order.fail");
        }
    }

    @Override
    public Response<Paging<YouyuncaiOrder>> paging(YouyuncaiOrderCriteria youyuncaiOrderCriteria) {
        try {
            return Response.ok(youyuncaiOrderDao.paging(youyuncaiOrderCriteria.getOffset(), youyuncaiOrderCriteria.getLimit(), youyuncaiOrderCriteria.toMap()));
        } catch (Exception e) {
            log.error("failed to page youyuncai order by criteria = {}, cause : {}", youyuncaiOrderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.youyuncai.order.failed");
        }
    }
}
