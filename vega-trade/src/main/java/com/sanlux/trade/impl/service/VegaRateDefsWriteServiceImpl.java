package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.VegaRateDefsDao;
import com.sanlux.trade.model.VegaRateDefs;
import com.sanlux.trade.service.VegaRateDefsWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/11/16.
 */
@Slf4j
@Service
@RpcProvider
public class VegaRateDefsWriteServiceImpl implements VegaRateDefsWriteService {
    @Autowired
    private VegaRateDefsDao vegaRateDefsDao;

    @Override
    public Response<Boolean> updateRateKey(Long id, Long rateKey) {
        try {
            VegaRateDefs vegaRateDefs = new VegaRateDefs();
            vegaRateDefs.setId(id);
            vegaRateDefs.setRateKey(rateKey);
            vegaRateDefsDao.update(vegaRateDefs);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("update rate define (id={}) rateKey: {} fail,cause: {}",id, rateKey,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("update.rate.define.fail");
        }
    }


}
