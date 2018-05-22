package com.sanlux.common.helper;

import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.exception.NotAllowCreateOrderException;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/17/16
 * Time: 10:44 PM
 */
public class UserTypeHelper {

    /**
     * 根据当前用户获取买家身份
     *
     * @param user 当前登录用户
     * @return OrderUserType
     */
    public static OrderUserType getOrderUserTypeByUser(ParanaUser user) {
        List<String> roles = user.getRoles();
        return extractUserType(roles);
    }

    public static OrderUserType getOrderUserTypeByRoles(List<String> roles) {
        return extractUserType(roles);
    }

    private static OrderUserType extractUserType(List<String> roles) {
        if (roles.contains(VegaUserRole.DEALER_FIRST.name())) {
            return OrderUserType.DEALER_FIRST;
        }
        if (roles.contains(VegaUserRole.DEALER_SECOND.name())) {
            return OrderUserType.DEALER_SECOND;
        }
        if (roles.size() == 1 && (roles.contains(UserRole.BUYER.name()) || roles.contains(UserRole.ADMIN.name()))) {
            return OrderUserType.NORMAL_USER;
        }
        if(roles.contains(VegaUserRole.SUPPLIER.name())){
            return OrderUserType.SUPPLIER;
        }
        if(roles.contains(VegaUserRole.SERVICE_MANAGER.name())){
            return OrderUserType.SERVICE_MANAGER;
        }


        throw new NotAllowCreateOrderException("current.user.not.allow.shopping");
    }
}
