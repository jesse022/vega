package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.VegaDirectPayInfoDao;
import com.sanlux.trade.impl.manager.DirectPayInfoManager;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.service.VegaDirectPayInfoWriteService;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by liangfujie on 16/10/28
 */
@Slf4j
@Service
@RpcProvider
public class VegaDirectPayInfoWriteServiceImpl implements VegaDirectPayInfoWriteService {

    @Autowired
    private VegaDirectPayInfoDao vegaDirectPayInfoDao;

    @Autowired
    private DirectPayInfoManager directPayInfoManager;

    @Override
    public Response<Boolean> updateStatusByBusinessId(String businessId, Integer newStatus) {
        try {

            return Response.ok(vegaDirectPayInfoDao.updateStatusByBusinessId(businessId, newStatus));
        } catch (Exception e) {
            log.error("update status  by businessId failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("update.status.by.businessId.fail");

        }
    }

    @Override
    public Response<Boolean> create(VegaDirectPayInfo vegaDirectPayInfo,
                                    VegaSellerTradeDailySummary sellerTradeDailySummary) {
        try {
            directPayInfoManager.createVegaDirectPayInfo(vegaDirectPayInfo, sellerTradeDailySummary);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("create vegaDirectPayInfo failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("create.vega.direct.info.fail");

        }
    }

    @Override
    public Response<Boolean> updateVegaDirectPayInfoAndSettleOrderDetail(VegaDirectPayInfo vegaDirectPayInfo,
                                                                         VegaSellerTradeDailySummary sellerTradeDailySummary) {
        try {
            directPayInfoManager.updateVegaDirectPayInfoStatus(vegaDirectPayInfo,sellerTradeDailySummary);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("create vegaDirectPayInfo failed cause {}", Throwables.getStackTraceAsString(e));
            return Response.fail("update.vega.direct.info.fail");

        }
    }



}
