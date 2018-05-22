package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.store.impl.dao.StoreImportsDao;
import com.sanlux.store.model.StoreImports;
import com.sanlux.store.service.StoreImportsReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 进销存批量导入日志读服务实现类
 * Created by lujm on 2017/3/16.
 */
@Service
@RpcProvider
@Slf4j
public class StoreImportsReadServiceImpl implements StoreImportsReadService{

    private final StoreImportsDao storeImportsDao;

    @Autowired
    public StoreImportsReadServiceImpl(StoreImportsDao storeImportsDao) {
        this.storeImportsDao = storeImportsDao;
    }

    @Override
    public Response<StoreImports> findById(Long id) {
        try {
            return Response.ok(storeImportsDao.findById(id));
        } catch (Exception e) {
            log.error("find storeImports by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.find.fail");
        }
    }

    @Override
    public Response<StoreImports> findByKey(String key) {
        try {
            return Response.ok(storeImportsDao.findByKey(key));
        } catch (Exception e) {
            log.error("find storeImports by key :{} failed,  cause:{}", key, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.find.fail");
        }
    }

    @Override
    public Response<List<StoreImports>> findListByKey(String key) {
        try {
            return Response.ok(storeImportsDao.findListByKey(key));
        } catch (Exception e) {
            log.error("find storeImports by key :{} failed,  cause:{}", key, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.find.fail");
        }
    }

    @Override
    public Response<Paging<StoreImports>> findByUserId(Long userId, Integer pageNo, Integer pageSize) {
        try {
            if (userId == null) {
                log.error("user id can not be null");
                return Response.fail("user.id.can.not.be.null");
            }

            PageInfo pageInfo = PageInfo.of(pageNo, pageSize);

            StoreImports criteria = new StoreImports();
            criteria.setUserId(userId);

            Paging<StoreImports> storeImportsPaging = storeImportsDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria);
            return Response.ok(storeImportsPaging);
        } catch (Exception e) {
            log.error("fail to find storeImports by userId={},pageNo={},pageSize={},cause:{}",
                    userId,pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.find.fail");
        }
    }
    @Override
    public Response<Paging<StoreImports>>  Paging(Integer pageNo, Integer pageSize,Map<String, Object> criteria){
        try {
            PageInfo pageInfo = PageInfo.of(pageNo, pageSize);
            Paging<StoreImports> storeImportsPaging = storeImportsDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria);
            return Response.ok(storeImportsPaging);
        } catch (Exception e) {
            log.error("fail to find storeImports by criteria={},pageNo={},pageSize={},cause:{}",
                    criteria,pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("storeImports.find.fail");
        }
    }
}
