package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.common.enums.ServiceManagerType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.user.impl.dao.ServiceManagerUserDao;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.service.ServiceManagerUserWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;

/**
 * 业务经理信息会员表写服务实现类
 *
 * Created by lujm on 2017/5/25.
 */
@Slf4j
@Service
@RpcProvider
public class ServiceManagerUserWriteServiceImpl implements ServiceManagerUserWriteService {

    private final ServiceManagerUserDao serviceManagerUserDao;

    private final UserDao userDao;


    @Autowired
    public ServiceManagerUserWriteServiceImpl(ServiceManagerUserDao serviceManagerUserDao, UserDao userDao) {
        this.serviceManagerUserDao = serviceManagerUserDao;
        this.userDao = userDao;
    }


    @Override
    public Response<Long> create(ServiceManagerUser serviceManagerUser) {
        try {
            Boolean isSuccess =  serviceManagerUserDao.create(serviceManagerUser);
            if(isSuccess) {
                return Response.ok(serviceManagerUser.getId());
            }
            return Response.fail("service.manager.user.create.fail");
        } catch (Exception e) {
            log.error("create serviceManagerUser failed, serviceManagerUser:{}, cause:{}", serviceManagerUser, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.create.fail");
        }
    }


    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return Response.ok(serviceManagerUserDao.delete(id));
        } catch (Exception e) {
            log.error("delete serviceManagerUser failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.delete.fail");
        }
    }

    @Override
    public Response<Boolean> addServiceManagerUser(String mobile, Long serviceManagerId, String serviceManagerName, Integer serviceManagerType, String remark) {
        try {
            //检查用户是否存在
            User user = userDao.findByMobile(mobile);
            checkState(Arguments.notNull(user), "user.not.exist.fail");
            List<String> roles = user.getRoles();
            //判断用户是否为普通用户
            checkState(user.getType().equals(2), "shop.user.not.ordinary.fail");
            if (Objects.equals(ServiceManagerType.PLATFORM.value(), serviceManagerType)) {
                // 平台业务经理
                checkState((Objects.equals(roles.size(), 1) && roles.get(0).equals(UserRole.BUYER.name()))
                        || roles.contains(VegaUserRole.DEALER_FIRST.name()) || roles.contains(VegaUserRole.DEALER_SECOND.name()), "shop.user.not.ordinary.and.dealer.fail");

            }
            if (Objects.equals(ServiceManagerType.DEALER_FIRST.value(), serviceManagerType)) {
                // 一级业务经理
                checkState((Objects.equals(roles.size(), 1) && roles.get(0).equals(UserRole.BUYER.name()))
                         || roles.contains(VegaUserRole.DEALER_SECOND.name()), "shop.user.not.ordinary.and.second.dealer.fail");

            }
            if (Objects.equals(ServiceManagerType.DEALER_SECOND.value(), serviceManagerType)) {
                // 二级业务经理
                checkState(Objects.equals(roles.size(), 1) && roles.get(0).equals(UserRole.BUYER.name()), "shop.user.not.ordinary.fail");
            }

            ServiceManagerUser serviceManagerUser = new ServiceManagerUser();
            serviceManagerUser.setServiceManagerId(serviceManagerId);
            serviceManagerUser.setUserId(user.getId());
            serviceManagerUser.setUserName(user.getName());
            serviceManagerUser.setServiceManagerName(serviceManagerName);
            serviceManagerUser.setMobile(mobile);
            serviceManagerUser.setRemark(remark);
            serviceManagerUser.setType(serviceManagerType);
            return Response.ok(serviceManagerUserDao.create(serviceManagerUser));
        } catch (IllegalStateException e) {
            log.error("add serviceManagerUser failed ,mobile:{},serviceManagerId{},serviceManagerName{}, error:{}", mobile, serviceManagerId, serviceManagerName, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("add serviceManagerUser failed ,mobile:{},serviceManagerId{},serviceManagerName{}, cause:{}", mobile, serviceManagerId, serviceManagerName, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.add.fail");
        }
    }

    @Override
    public Response<Boolean> deleteByUserId(Long userId) {
        try {
            return Response.ok(serviceManagerUserDao.deleteByUserId(userId));
        } catch (Exception e) {
            log.error("delete serviceManagerUser failed,userId{}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.delete.fail");
        }
    }

    @Override
    public Response<Boolean> deleteByServiceManagerId(Long serviceManagerId) {

        try {
            return Response.ok(serviceManagerUserDao.deleteByServiceManagerId(serviceManagerId));
        } catch (Exception e) {
            log.error("delete serviceManagerUser failed,serviceManagerId{}, cause:{}", serviceManagerId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.delete.fail");
        }
    }

    @Override
    public Response<Boolean> refreshServiceManagerUserByUserId(Long userId) {
        try {
            User user = userDao.findById(userId);
            if (user == null) {
                log.error("user find failed,userId{}, cause:{}", userId, "user not exist");
                return Response.fail("service.manager.user.refresh.fail");
            }
            return Response.ok(serviceManagerUserDao.refreshServiceManagerUserByUserId(userId, user.getMobile(), user.getName()));
        } catch (Exception e) {
            log.error("refresh serviceManagerUser failed,userId{}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("service.manager.user.refresh.fail");
        }
    }
}
