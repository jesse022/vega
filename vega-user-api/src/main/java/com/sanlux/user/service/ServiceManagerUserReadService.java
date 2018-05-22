package com.sanlux.user.service;

import com.google.common.base.Optional;
import com.sanlux.user.dto.criteria.ServiceManagerUserCriteria;
import com.sanlux.user.model.ServiceManagerUser;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 业务经理会员表读服务类
 *
 * Created by lujm on 2017/5/24.
 */

public interface ServiceManagerUserReadService {

    /**
     * 根据id查询业务经理会员表
     *
     * @param id 主键id
     * @return 业务经理会员表信息
     */
    Response<Optional<ServiceManagerUser>> findById(Long id);


    /**
     * 根据条件查询业务经理会员分页信息
     *
     * @param criteria 分页条件
     * @return 会员分页 Response<ServiceManagerUser>
     */
    Response<Paging<ServiceManagerUser>> paging(ServiceManagerUserCriteria criteria);

    /**
     * 根据业务经理ID获取会员信息
     *
     * @param serviceManagerId 业务经理Id
     * @return Response<ServiceManagerUser>
     */
    Response<List<ServiceManagerUser>> findByServiceManagerId(Long serviceManagerId);


    /**
     * 根据业务经理Ids获取会员信息
     *
     * @param serviceManagerIds 业务经理Ids
     * @return Response<ServiceManagerUser>
     */
    Response<List<ServiceManagerUser>> findByServiceManagerIds(List<Long> serviceManagerIds);


    /**
     * 根据用户ID获取会员信息
     * @param userId 用户ID
     * @return ServiceManagerUsers
     */
    Response<List<ServiceManagerUser>> findByUserId(Long userId);

    /**
     * 通过手机号获得会员信息
     * @param mobile 手机号
     * @return ServiceManagerUser
     */
    Response<Optional<ServiceManagerUser>> findByMobile(String mobile);


    /**
     * 通过手机号获得会员信息
     * @param mobile 手机号
     * @return ServiceManagerUser
     */
    Response<List<ServiceManagerUser>> findListByMobile(String mobile);
}
