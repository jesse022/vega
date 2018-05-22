package com.sanlux.shop.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.shop.impl.dao.VegaShopExtraBasicInfoDao;
import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import com.sanlux.shop.service.VegaShopExtraBasicInfoReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/12/19.
 */
@Service
@RpcProvider
@Slf4j
public class VegaShopExtraBasicInfoReadServiceImpl implements VegaShopExtraBasicInfoReadService {
    @Autowired
    private VegaShopExtraBasicInfoDao vegaShopExtraBasicInfoDao;

    @Override
    public Response<VegaShopExtraBasicInfo> findByShopId(Long shopId) {
        try {
            return Response.ok(vegaShopExtraBasicInfoDao.findByShopId(shopId));
        }catch (Exception e) {
            log.error("failed to find shop extra basic info by shopId = ({}), cause : {}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.extra.basic.info.find.failed");
        }
    }

    @Override
    public Response<VegaShopExtraBasicInfo> findByMacAddress(String macAddress) {
        try {
            return Response.ok(vegaShopExtraBasicInfoDao.findByMacAddress(macAddress));
        }catch (Exception e) {
            log.error("failed to find shop extra basic info by macAddress = ({}), cause : {}", macAddress, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.extra.basic.info.find.failed");
        }
    }
}
