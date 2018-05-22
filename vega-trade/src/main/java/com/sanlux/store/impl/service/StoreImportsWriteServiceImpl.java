package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.store.impl.dao.StoreImportsDao;
import com.sanlux.store.model.StoreImports;
import com.sanlux.store.service.StoreImportsWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 进销存批量导入写服务实现类
 * Created by lujm on 2017/3/16.
 */
@Slf4j
@Service
@RpcProvider
public class StoreImportsWriteServiceImpl implements StoreImportsWriteService{
    private final StoreImportsDao storeImportsDao;

    @Autowired
    public StoreImportsWriteServiceImpl(StoreImportsDao storeImportsDao) {
        this.storeImportsDao = storeImportsDao;
    }

    @Override
    public Response<Long> create(StoreImports storeImports) {
        try {
            storeImportsDao.create(storeImports);
            return Response.ok(storeImports.getId());
        } catch (Exception e) {
            log.error("create storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(StoreImports storeImports) {
        try {
            return Response.ok(storeImportsDao.update(storeImports));
        } catch (Exception e) {
            log.error("update storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.update.fail");
        }
    }
}
