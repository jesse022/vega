package com.sanlux.user.service;

import com.sanlux.user.model.ServiceManagerUser;
import io.terminus.common.model.Response;

/**
 * 业务经理会员表写服务类
 *
 * Created by lujm on 2017/5/24.
 */

public interface ServiceManagerUserWriteService {

    /**
     * 创建
     * @param serviceManagerUser 专属会员
     * @return 主键id
     */
    Response<Long> create(ServiceManagerUser serviceManagerUser);


    /**
     * 根据主键id删除
     * @param id 专属会员Id
     * @return 是否成功
     */
    Response<Boolean> delete(Long id);


    /**
     * 业务经理通过手机号添加新的用户,如果已经存在返回用户详细信息并提示,如果不存在则添加并返回用户详细信息。
     * @param mobile 手机号
     * @param serviceManagerId 业务经理ID
     * @param serviceManagerName 业务经理姓名
     * @param serviceManagerType 业务经理类型
     * @param remark 备注
     * @return 是否成功
     */
    Response<Boolean> addServiceManagerUser(String mobile, Long serviceManagerId, String serviceManagerName, Integer serviceManagerType, String remark);


    /**
     * 根据用户ID删除
     * @param userId
     * @return 是否成功
     */
    Response<Boolean> deleteByUserId(Long userId);


    /**
     * 根据业务经理ID删除会员
     * @param serviceManagerId 业务经理Id
     * @return 是否成功
     */
    Response<Boolean> deleteByServiceManagerId(Long serviceManagerId);

    /**
     * 根据用户ID同步业务经理会员表中用户的信息
     * @param userId 用户ID
     * @return 用户信息是否同步成功
     */
    Response<Boolean> refreshServiceManagerUserByUserId(Long userId);


}