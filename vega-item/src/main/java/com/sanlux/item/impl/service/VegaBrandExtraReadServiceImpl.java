package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.cache.VegaBrandExtraByBrandIdCacher;
import com.sanlux.item.impl.dao.VegaBrandExtraDao;
import com.sanlux.item.model.VegaBrandExtra;
import com.sanlux.item.service.VegaBrandExtraReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/1/22.
 */
@Service
@RpcProvider
@Slf4j
public class VegaBrandExtraReadServiceImpl implements VegaBrandExtraReadService {

    private final VegaBrandExtraByBrandIdCacher vegaBrandExtraByBrandIdCacher;

    private final VegaBrandExtraDao vegaBrandExtraDao;

    @Autowired
    public VegaBrandExtraReadServiceImpl(VegaBrandExtraByBrandIdCacher vegaBrandExtraByBrandIdCacher,
                                         VegaBrandExtraDao vegaBrandExtraDao) {
        this.vegaBrandExtraByBrandIdCacher = vegaBrandExtraByBrandIdCacher;
        this.vegaBrandExtraDao = vegaBrandExtraDao;
    }

    @Override
    public Response<VegaBrandExtra> findByBrandId(Long brandId) {
        try {
            return Response.ok(vegaBrandExtraDao.findByBrandId(brandId));
        } catch (Exception e) {
            log.error("find brand extra failed, brandId:{}, cause:{}", brandId, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.extra.find.fail");
        }
    }

    @Override
    public Response<VegaBrandExtra> findBrandExtraByCacher(Long brandId) {
        try {
            return Response.ok(vegaBrandExtraByBrandIdCacher.findByBrandId(brandId));
        } catch (Exception e) {
            log.error("find brand extra failed, brandId:{}, cause:{}", brandId, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.extra.find.fail");
        }
    }
}
