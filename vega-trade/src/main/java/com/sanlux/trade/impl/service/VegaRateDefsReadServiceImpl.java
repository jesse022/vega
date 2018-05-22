package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.VegaRateDefsDao;
import com.sanlux.trade.model.VegaRateDefs;
import com.sanlux.trade.service.VegaRateDefsReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lujm on 2017/11/16.
 */
@Slf4j
@Service
@RpcProvider
public class VegaRateDefsReadServiceImpl implements VegaRateDefsReadService {
    @Autowired
    private VegaRateDefsDao vegaRateDefsDao;

    @Override
    public Response<VegaRateDefs> findById(Long id) {
        try {
            VegaRateDefs vegaRateDefs = vegaRateDefsDao.findById(id);
            return Response.ok(vegaRateDefs);
        } catch (Exception e) {
            log.error("find rate define by type and name failed cause: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.rate.define.fail");

        }
    }

    @Override
    public Response<List<VegaRateDefs>> findByType(Integer type) {
        try {
            List<VegaRateDefs> lists = vegaRateDefsDao.findByType(type);
            return Response.ok(lists);
        } catch (Exception e) {
            log.error("find rate define by type failed cause: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.rate.define.fail");

        }
    }

    @Override
    public Response<VegaRateDefs> findByTypeAndName(Integer type, String name) {
        try {
            VegaRateDefs vegaRateDefs = vegaRateDefsDao.findByTypeAndName(type, name);
            return Response.ok(vegaRateDefs);
        } catch (Exception e) {
            log.error("find rate define by type and name failed cause: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.rate.define.fail");

        }
    }

    @Override
    public Response<Paging<VegaRateDefs>> paging(Integer pageNo, Integer pageSize) {
        try {
            PageInfo pageInfo =new PageInfo(pageNo,pageSize);
            Paging<VegaRateDefs> paging = vegaRateDefsDao.paging(pageInfo.getOffset(),pageInfo.getLimit());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("paging rate define failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("paging.rate.define.fail");
        }
    }
}
