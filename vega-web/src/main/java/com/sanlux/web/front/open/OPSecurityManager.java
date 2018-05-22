package com.sanlux.web.front.open;

import io.terminus.pampas.openplatform.core.OPHook;
import io.terminus.pampas.openplatform.core.SecurityManager;
import io.terminus.pampas.openplatform.entity.OPClientInfo;
import org.springframework.stereotype.Component;

/**
 * OPEN 权限接口管理
 *
 * Mail: xiao@terminus.io <br>
 * Date: 2016-03-09 8:07 PM  <br>
 * Author: xiao
 */
@Component
public class OPSecurityManager implements SecurityManager {


    @Override
    public OPClientInfo findClientByAppKey(String appKey) {
        return new OPClientInfo(1L, appKey,"sanlux.secret");
    }

    /**
     * 根据clientId获取客户信息
     *
     * @param clientId 客户id
     * @return 对应的客户信息, 主要包括客户id, 客户的appKey, 以及分配给客户的appSecret
     */
    @Override
    public OPClientInfo findClientById(Long clientId) {
        return new OPClientInfo(clientId, "sanlux","sanlux.secret");
    }

    @Override
    public boolean hasPermission(Long aLong, String s) {
        return true;
    }

    @Override
    public OPHook getHook(Long clientId, String method) {
            return null;
    }
}
