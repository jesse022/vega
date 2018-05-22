package com.sanlux.user.service;

import com.sanlux.user.model.ServiceManager;
import io.terminus.common.model.Response;
import io.terminus.parana.user.model.User;

/**
 * 业务经理信息表(用户表)写服务类
 *
 * Created by lujm on 2017/5/23.
 */
public interface ServiceManagerWriteService {
    /**
     * 创建
     * @param  user 用户表信息
     * @param serviceManager 业务经理表信息
     * @return 主键id
     */
    Response<Long> create(User user, ServiceManager serviceManager);

    /**
     * 更新
     * @param user 用户表信息
     * @param serviceManager 业务经理表信息
     * @return 是否成功
     */
    Response<Boolean> update(User user, ServiceManager serviceManager);

    /**
     * 根据主键id删除
     * @param id 用户Id
     * @return 是否成功
     */
    Response<Boolean> delete(Long id);

    /**
     * 根据id修改业务经理信息表用户状态
     *
     * @param id 主键id
     * @param userID 用户Id
     * @param status 用户状态
     * @return 是否成功
     */
    Response<Boolean> updateStatus(Long id, Long userID, Integer status);
}
