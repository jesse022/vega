package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.cache.VegaBrandExtraByBrandIdCacher;
import com.sanlux.item.impl.dao.VegaBrandExtraDao;
import com.sanlux.item.model.VegaBrandExtra;
import com.sanlux.item.service.VegaBrandExtraWriteService;
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
public class VegaBrandExtraWriteServiceImpl implements VegaBrandExtraWriteService{

    private final VegaBrandExtraDao vegaBrandExtraDao;

    private final VegaBrandExtraByBrandIdCacher vegaBrandExtraByBrandIdCacher;

    @Autowired
    public VegaBrandExtraWriteServiceImpl(VegaBrandExtraDao vegaBrandExtraDao, VegaBrandExtraByBrandIdCacher vegaBrandExtraByBrandIdCacher) {
        this.vegaBrandExtraDao = vegaBrandExtraDao;
        this.vegaBrandExtraByBrandIdCacher = vegaBrandExtraByBrandIdCacher;
    }


    @Override
    public Response<Long> create(VegaBrandExtra vegaBrandExtra) {
        try {
            vegaBrandExtraDao.create(vegaBrandExtra);
            Long id = vegaBrandExtra.getId();
            return Response.ok(id);
        } catch (Exception e) {
            log.error("create brand extra failed, vegaBrandExtra:{}, cause:{}", vegaBrandExtra, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.extra.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateByBrandId(Long brandId, String detail) {
        try {
            return Response.ok(vegaBrandExtraDao.updateByBrandId(brandId, detail));
        } catch (Exception e) {
            log.error("update brand extra by brand id failed, brandId:{}, cause:{}", brandId, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.extra.update.fail");
        }

    }

    @Override
    public Response<Boolean> invalidByBranId(Long brandId) {
        try {
            return Response.ok(vegaBrandExtraByBrandIdCacher.invalidByBranId(brandId));
        } catch (Exception e) {
            log.error("invalid brand extra cache by brand id failed, brandId:{}, cause:{}", brandId, Throwables.getStackTraceAsString(e));
            return Response.ok(Boolean.FALSE);
        }
    }

}
