package com.sanlux.web.front.component.user;


import com.google.common.base.Optional;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.user.model.ServiceManager;
import com.sanlux.user.service.ServiceManagerReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.user.auth.Role;
import io.terminus.parana.user.auth.RoleProvider;
import io.terminus.parana.user.auth.RoleProviderRegistry;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * 业务经理角色Provider
 * <p/>
 * Created by lujm on 2017/5/26.
 */
@Slf4j
@Component
public class VegaServiceManagerRoleProvider implements RoleProvider {
    @RpcConsumer
    private ServiceManagerReadService serviceManagerReadService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @Autowired
    private RoleProviderRegistry roleProviderRegistry;

    @PostConstruct
    public void init() {
        this.roleProviderRegistry.addRoleProvider(this);
    }

    @Override
    public int acceptType() {
        return UserType.NORMAL.value();
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        Response<User> userResp = userReadService.findById(userId);
        if (!userResp.isSuccess()) {
            log.error("failed to find user by userId = {}, cause : {}", userId, userResp.getError());
            return null;
        }
        // 业务经理角色
        Role role = new Role();
        Response<Optional<ServiceManager>> resp = serviceManagerReadService.findByUserId(userId);
        if (!resp.isSuccess()) {
            log.warn(" service manager find by userId fail,userId={},error={}", userId, resp.getError());
            return null;
        }
        if (!resp.getResult().isPresent()) {
            log.warn(" service manager find by userId fail,userId={},error={}", userId, resp.getError());
            return null;
        }
        ServiceManager serviceManager = resp.getResult().get();
        role.setBase(VegaUserRole.SERVICE_MANAGER.name());
        role.setType(1);
        role.setContext(new HashMap<String, String>());
        role.getContext().put("serviceManagerId", String.valueOf(serviceManager.getId()));
        return role;
    }
}
