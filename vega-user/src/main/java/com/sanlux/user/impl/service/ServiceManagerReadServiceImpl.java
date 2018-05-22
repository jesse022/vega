package com.sanlux.user.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.user.dto.criteria.ServiceManagerCriteria;
import com.sanlux.user.impl.dao.ServiceManagerDao;
import com.sanlux.user.model.ServiceManager;
import com.sanlux.user.service.ServiceManagerReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务经理信息表(用户表)服务实现类
 *
 * Created by lujm on 2017/5/23.
 */
@Slf4j
@Service
@RpcProvider
public class ServiceManagerReadServiceImpl implements ServiceManagerReadService {
    private ServiceManagerDao serviceManagerDao;

    @Autowired
    public ServiceManagerReadServiceImpl(ServiceManagerDao serviceManagerDao){
        this.serviceManagerDao = serviceManagerDao;
    }

    @Override
    public Response<Optional<ServiceManager>> findById(Long id){
        try {
            return Response.ok(Optional.fromNullable(serviceManagerDao.findById(id)));
        } catch (Exception e) {
            log.error("find service manager by id failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.find.fail");
        }
    }

    @Override
    public Response<List<ServiceManager>> findByIds(List<Long> ids){
        try {
            return Response.ok(serviceManagerDao.findByIds(ids));
        } catch (Exception e) {
            log.error("find service manager by id failed, id:{}, cause:{}", ids, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.find.fail");
        }
    }

    @Override
    public Response<Optional<ServiceManager>> findByUserId(Long userId){
        try {
            return Response.ok(Optional.fromNullable(serviceManagerDao.findByUserId(userId)));
        } catch (Exception e) {
            log.error("find service manager by id failed, id:{}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.find.fail");
        }
    }

    @Override
    public Response<Paging<ServiceManager>> paging(ServiceManagerCriteria criteria){
        try {
            return Response.ok(serviceManagerDao.paging(criteria.toMap()));
        } catch (Exception e) {
            log.error("fail to find service manager paging by criteria {},cause:{}",
                    criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.find.fail");
        }

    }
}
