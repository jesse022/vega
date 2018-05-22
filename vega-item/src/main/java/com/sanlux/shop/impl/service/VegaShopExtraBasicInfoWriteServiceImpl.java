package com.sanlux.shop.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.shop.impl.dao.VegaShopExtraBasicInfoDao;
import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import com.sanlux.shop.service.VegaShopExtraBasicInfoWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/12/19.
 */
@Slf4j
@Service
@RpcProvider
public class VegaShopExtraBasicInfoWriteServiceImpl implements VegaShopExtraBasicInfoWriteService {

    @Autowired
    private VegaShopExtraBasicInfoDao vegaShopExtraBasicInfoDao;


    @Override
    public Response<Long> create(VegaShopExtraBasicInfo vegaShopExtraBasicInfo) {
        try {
            Boolean isSuccess = vegaShopExtraBasicInfoDao.create(vegaShopExtraBasicInfo);
            if (!isSuccess) {
                return null;
            }
            return Response.ok(vegaShopExtraBasicInfo.getId());
        }catch (Exception e) {
            log.error("failed to create shop extra basic info : ({}), cause : {}", vegaShopExtraBasicInfo, Throwables.getStackTraceAsString(e));
            return Response.fail("create.shop.extra.basic.info.failed");
        }
    }


    @Override
    public Response<Long> updateByShopId(VegaShopExtraBasicInfo vegaShopExtraBasicInfo) {
        try {
            Boolean isSuccess = vegaShopExtraBasicInfoDao.updateByShopId(vegaShopExtraBasicInfo);
            if (!isSuccess) {
                return null;
            }
            return Response.ok(vegaShopExtraBasicInfo.getId());
        }catch (Exception e) {
            log.error("failed to shop extra basic info : ({}), cause : {}", vegaShopExtraBasicInfo, Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.extra.basic.info.failed");
        }
    }
}
