package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.ServiceManagerDao;
import com.sanlux.user.manager.ServiceManagerManager;
import com.sanlux.user.model.ServiceManager;
import com.sanlux.user.service.ServiceManagerWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 业务经理信息表(用户表)写服务实现类
 *
 * Created by lujm on 2017/5/23.
 */
@Slf4j
@Service
@RpcProvider
public class ServiceManagerWriteServiceImpl implements ServiceManagerWriteService {
    private ServiceManagerDao serviceManagerDao;
    private ServiceManagerManager serviceManagerUserManager;

    @Autowired
    public ServiceManagerWriteServiceImpl(ServiceManagerDao serviceManagerDao, ServiceManagerManager serviceManagerUserManager){
        this.serviceManagerDao = serviceManagerDao;
        this.serviceManagerUserManager = serviceManagerUserManager;
    }

    @Override
    public Response<Long> create(User user, ServiceManager serviceManager){
        try {
            Boolean isSuccess = serviceManagerUserManager.create(user, serviceManager);
            if(!isSuccess){
                Response.fail("service.manager.create.fail");
            }
            return Response.ok(serviceManager.getId());
        } catch (Exception e) {
            log.error("create service manager failed, serviceManager:{}, cause:{}", serviceManager, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.create.fail");
        }
    }


    @Override
    public Response<Boolean> update(User user, ServiceManager serviceManager){
        try {
            Boolean isSuccess = serviceManagerUserManager.update(user, serviceManager);
            if(!isSuccess){
                Response.fail("service.manager.update.fail");
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("update service manager failed, serviceManager:{}, cause:{}", serviceManager, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.update.fail");
        }
    }

    @Override
    public Response<Boolean> delete(Long id){
        try {
            return Response.ok(serviceManagerDao.delete(id));
        } catch (Exception e) {
            log.error("delete service manager user failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.delete.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatus(Long id, Long userID, Integer status){
        try {
            Boolean isSuccess = serviceManagerUserManager.updateStatus(id, userID, status);
            if(!isSuccess){
                Response.fail("service.manager.user.update.fail");
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("update service manager user failed, id:{} userID:{} status:{}, cause:{}", id, userID, status, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.update.fail");
        }
    }
}
