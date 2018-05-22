package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.service.VegaDirectPayInfoReadService;
import com.sanlux.trade.impl.dao.VegaDirectPayInfoDao;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/10/28
 */
@Slf4j
@Service
@RpcProvider
public class VegaDirectPayInfoReadServiceImpl implements VegaDirectPayInfoReadService {
    @Autowired
    VegaDirectPayInfoDao vegaDirectPayInfoDao;


    @Override
    public Response<List<VegaDirectPayInfo>> findManyByStatus(Integer status) {
        try {
            List lists = vegaDirectPayInfoDao.findManyByStatus(status);
            return Response.ok(lists);
        } catch (Exception e) {
            log.error("find many by status failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.many.by.status.fail");

        }

    }

    @Override
    public Response<Paging<VegaDirectPayInfo>> pagingByStatus(Integer pageNo, Integer pageSize, Integer status) {
        try {
            PageInfo pageInfo =new PageInfo(pageNo,pageSize);
            Map<String ,Object> maps =new HashMap<String,Object>();
            maps.put("status",status);
            Paging<VegaDirectPayInfo> paging = vegaDirectPayInfoDao.paging(pageInfo.getOffset(),pageInfo.getLimit(),maps);
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("paging  by status failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("paging..by.status.fail");

        }
    }

    @Override
    public Response<VegaDirectPayInfo> findByBusinessId(String businessId) {
        try {
            VegaDirectPayInfo vegaDirectPayInfo = vegaDirectPayInfoDao.findByBusinessId(businessId);
            return Response.ok(vegaDirectPayInfo);
        } catch (Exception e) {
            log.error("find  by businessId failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.by.businessId.fail");

        }
    }


    @Override
    public Response<VegaDirectPayInfo> findByOrderId(Long orderId) {
        try {
            return Response.ok(vegaDirectPayInfoDao.findByOrderId(orderId));
        } catch (Exception e) {
            log.error("find vegaDirectPayInfo by orderId failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.vega.direct.info.fail");

        }
    }
}
