package com.sanlux.store.service;

import com.sanlux.store.model.StoreImports;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;


/**
 * Created by lujm on 2017/3/16.
 */
public interface StoreImportsReadService {
    /**
     * 根据id查询进销存批量导入日志
     * @param id 主键id
     * @return 进销存批量导入日志
     */
    Response<StoreImports> findById(Long id);

    /**
     * 根据key查询进销存批量导入日志
     * @param key 导入批次
     * @return 进销存批量导入日志
     */
    Response<StoreImports> findByKey(String key);

    /**
     * 根据key查询进销存批量导入日志
     * @param key 导入批次
     * @return 进销存批量导入日志
     */
    Response<List<StoreImports>> findListByKey(String key);

    /**
     * 根据用户ID分页查询进销存批量导入日志
     * @param userId 用户ID
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return 进销存批量导入日志
     */
    Response<Paging<StoreImports>> findByUserId(Long userId, Integer pageNo, Integer pageSize);

    /**
     * 根据用户ID分页查询进销存批量导入日志
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @param  criteria 查询条件
     * @return 进销存批量导入日志
     */
    Response<Paging<StoreImports>> Paging(Integer pageNo, Integer pageSize,Map<String, Object> criteria);
}
