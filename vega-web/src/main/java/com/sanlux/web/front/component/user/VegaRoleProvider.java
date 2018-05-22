/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.component.user;

import com.google.common.base.Objects;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.enums.VegaShopType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.auth.Role;
import io.terminus.parana.user.auth.RoleProvider;
import io.terminus.parana.user.auth.RoleProviderRegistry;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.util.ParanaUserMaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * Sanlux 用户角色Provider
 *
 * @author panxin
 */
@Slf4j
@Component
public class VegaRoleProvider implements RoleProvider {

    @RpcConsumer
    private ShopReadService shopReadService;
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
        User user = userResp.getResult();
        ParanaUser paranaUser = ParanaUserMaker.from(user);
        // 普通买家不需要添加特殊角色
        if (user.getRoles().contains(UserRole.BUYER.name()) && user.getRoles().size() == 1) {
            log.info("current user ({}) is normal buyer.", paranaUser);
            return null;
        }

        // 其他用户需要添加特殊角色
        Response<Shop> resp = shopReadService.findByUserId(userId);
        if (!resp.isSuccess()) {
            log.warn("failed to find shop by userId = {}, cause : {}", userId, resp.getError());
            return null;
        }

        Shop shop = resp.getResult();
        Role role = new Role();

        // 供应商
        if (Objects.equal(shop.getType(), VegaShopType.SUPPLIER.value())) {
            role.setBase(VegaUserRole.SUPPLIER.name());
        }
        // 一级经销商
        if (Objects.equal(shop.getType(), VegaShopType.DEALER_FIRST.value())) {
            role.setBase(VegaUserRole.DEALER_FIRST.name());
        }
        // 二级经销商
        if (Objects.equal(shop.getType(), VegaShopType.DEALER_SECOND.value())) {
            role.setBase(VegaUserRole.DEALER_SECOND.name());
        }

        role.setType(1);
        role.setContext(new HashMap<String, String>());
        role.getContext().put("shopId", String.valueOf(shop.getId()));

        return role;
    }

}
