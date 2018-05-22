package com.sanlux.common.helper;

import com.sanlux.common.enums.VegaUserRole;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/17/16
 * Time: 10:44 PM
 */
public class UserRoleHelper {



    public static String getUserRoleName(ParanaUser user){

        if(user.getRoles().contains(VegaUserRole.DEALER_FIRST.name())){
            return VegaUserRole.DEALER_FIRST.name();
        }
        if(user.getRoles().contains(VegaUserRole.DEALER_SECOND.name())){
            return VegaUserRole.DEALER_SECOND.name();
        }
        if(user.getRoles().contains(VegaUserRole.SUPPLIER.name()) ||
                user.getRoles().contains(VegaUserRole.SERVICE_MANAGER.name()) ){
            //供应商(业务经理)购买作为普通用户身份处理
            return UserRole.BUYER.name();
        }
        if(user.getRoles().size()==1
                &&(user.getRoles().contains(UserRole.BUYER.name()))
                || user.getRoles().contains(UserRole.ADMIN.name())){
            return UserRole.BUYER.name();
        }

        throw new JsonResponseException("current.user.not.allow.shopping");

    }

}
