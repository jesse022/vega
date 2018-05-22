package com.sanlux.user.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.user.impl.cache.VegaRegionIdsByShopIdCacher;
import com.sanlux.user.impl.dao.DeliveryScopeDao;
import com.sanlux.user.service.VegaRegionIdsByShopIdCacherService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
@Slf4j
@Service
@RpcProvider
public class VegaRegionIdsByShopIdCacherServiceImpl implements VegaRegionIdsByShopIdCacherService {

    private final VegaRegionIdsByShopIdCacher vegaRegionIdsByShopIdCacher;

    private final DeliveryScopeDao deliveryScopeDao;

    @Autowired
    public VegaRegionIdsByShopIdCacherServiceImpl(VegaRegionIdsByShopIdCacher vegaRegionIdsByShopIdCacher,
                                                  DeliveryScopeDao deliveryScopeDao) {
        this.vegaRegionIdsByShopIdCacher = vegaRegionIdsByShopIdCacher;
        this.deliveryScopeDao = deliveryScopeDao;
    }

    @Override
    public Response<Optional<List<Integer>>> findByShopId(Long shopId) {
        try {
            List<Integer> deliveryScopeDtos = vegaRegionIdsByShopIdCacher.findByShopId(shopId);
            return Response.ok(Optional.fromNullable(deliveryScopeDtos));
        }catch (Exception e) {
            log.error("find regionIds by shopId: {} failed, cause:{}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.regionIds.fail");
        }
    }


    @Override
    public Response<Optional<List<Long>>> findShopIdsByRegionId(Integer regionId) {
        try {
            List<Long> resp = Lists.newArrayList();
            List<Long> shopIds = deliveryScopeDao.findAllShopIds();

            for(Long shopId : shopIds) {
                List<Integer> deliveryScopes = vegaRegionIdsByShopIdCacher.findByShopId(shopId);
                if (deliveryScopes.contains(regionId)) {
                    resp.add(shopId);
                }
            }
            if (resp.isEmpty()) {
                return Response.ok(Optional.<List<Long>>absent());
            }
            return Response.ok(Optional.of(resp));
        } catch (Exception e) {
            log.error("find shopIds by regionId:{} failed, cause:{}", regionId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.shopIds.by.regionId.fail");
        }
    }


    @Override
    public Response<Boolean> invalidByShopId(Long shopId) {
        try {
            if (vegaRegionIdsByShopIdCacher.invalidByShopId(shopId)) {
                return Response.ok(Boolean.TRUE);
            }
            return Response.fail("delivery.scope.invalid.fail");
        }catch (Exception e) {
            log.error("invalid deliveryScope by shopId: {} failed, cause:{}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("delivery.scope.invalid.fail");
        }
    }
}
