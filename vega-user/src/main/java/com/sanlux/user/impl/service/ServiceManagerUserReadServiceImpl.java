package com.sanlux.user.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.user.dto.criteria.ServiceManagerUserCriteria;
import com.sanlux.user.impl.dao.ServiceManagerUserDao;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.service.ServiceManagerUserReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务经理信息会员表读服务实现类
 *
 * Created by lujm on 2017/5/25.
 */
@Slf4j
@Service
@RpcProvider
public class ServiceManagerUserReadServiceImpl implements ServiceManagerUserReadService {

    private final ServiceManagerUserDao serviceManagerUserDao;

    @Autowired
    public ServiceManagerUserReadServiceImpl(ServiceManagerUserDao serviceManagerUserDao) {
        this.serviceManagerUserDao = serviceManagerUserDao;
    }

    @Override
    public Response<Optional<ServiceManagerUser>> findById(Long id) {
        try {
            return Response.ok(Optional.fromNullable(serviceManagerUserDao.findById(id)));
        } catch (Exception e) {
            log.error("find serviceManagerUser by id failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }

    @Override
    public Response<Paging<ServiceManagerUser>> paging(ServiceManagerUserCriteria criteria) {
        try {
            return Response.ok(serviceManagerUserDao.paging(criteria.toMap()));
        } catch (Exception e) {
            log.error("fail to find serviceManagerUser paging by  criteria {},cause:{}",
                    criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }


    @Override
    public Response<List<ServiceManagerUser>> findByServiceManagerId(Long serviceManagerId) {
        try {
            return Response.ok(serviceManagerUserDao.findByServiceManagerId(serviceManagerId));
        } catch (Exception e) {
            log.error("fail to find serviceManagerUser  by serviceManagerId = {},cause:{}",
                     serviceManagerId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }

    @Override
    public Response<List<ServiceManagerUser>> findByServiceManagerIds(List<Long> serviceManagerIds) {
        try {
            return Response.ok(serviceManagerUserDao.findByServiceManagerIds(serviceManagerIds));
        } catch (Exception e) {
            log.error("fail to find serviceManagerUser  by serviceManagerIds = {},cause:{}",
                    serviceManagerIds, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }

    @Override
    public Response<List<ServiceManagerUser>> findByUserId(Long userId) {
        try {
            List<ServiceManagerUser> serviceManagerUsers = serviceManagerUserDao.findByUserId(userId);
            return Response.ok(serviceManagerUsers);
        } catch (Exception e) {
            log.error("fail to find serviceManagerUser by userId:{},cause:{}",
                     userId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }

    @Override
    public Response<Optional<ServiceManagerUser>> findByMobile(String mobile) {
        try {
            ServiceManagerUser serviceManagerUser =serviceManagerUserDao.findByMobile(mobile);
            return Response.ok(Optional.fromNullable(serviceManagerUser));
        }catch (Exception e){
            log.error("fail to find serviceManagerUser by mobile:{},cause:{}",
                    mobile, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }

    @Override
    public Response<List<ServiceManagerUser>> findListByMobile(String mobile) {
        try {
            return Response.ok(serviceManagerUserDao.findListByMobile(mobile));
        }catch (Exception e){
            log.error("fail to find serviceManagerUsers by mobile:{},cause:{}",
                    mobile, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.find.fail");
        }
    }


}
