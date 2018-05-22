package com.sanlux.user.service;

import com.google.common.base.Optional;
import com.sanlux.user.dto.criteria.ServiceManagerCriteria;
import com.sanlux.user.model.ServiceManager;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 业务经理信息表(用户表)读服务类
 *
 * Created by lujm on 2017/5/23.
 */
public interface ServiceManagerReadService {
    /**
     * 根据id查询业务经理表信息
     *
     * @param id 主键id
     * @return 业务经理表信息
     */
    Response<Optional<ServiceManager>> findById(Long id);

    /**
     * 根据id查询业务经理表信息
     *
     * @param ids 主键ids
     * @return 业务经理表信息
     */
    Response<List<ServiceManager>> findByIds(List<Long> ids);

    /**
     * 根据userId查询业务经理表信息
     *
     * @param userId 用户id
     * @return 业务经理表信息
     */
    Response<Optional<ServiceManager>> findByUserId(Long userId);


    /**
     * 根据条件分页查询业务经理表信息
     *
     * @param criteria 分页条件
     * @return 分页信息 Response<ServiceManager>
     */
    Response<Paging<ServiceManager>> paging(ServiceManagerCriteria criteria);

}
