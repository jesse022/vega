package com.sanlux.category.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.BackCategory;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/19
 */
public interface VegaCategoryReadService {

    /**
     * 根据商品ID获取商品一级类目ID
     * @param itemId 商品ID
     * @return 一级类目ID
     */
    Response<Long> findBackCategoryId(Long itemId);

    /**
     * 根据类目ID获取类目树
     * @param categoryId categoryId
     * @return 类目树List<BackCategory>
     */
    Response<List<BackCategory>> findAncestorsByBackCategoryId (Long categoryId);

    /**
     * 根据类目树获取叶子类目
     * @param path 类目树
     * @return 叶子类目
     */
    Response<BackCategory> findLeafByBackCategoryPath(List<String> path);

    /**
     * 根据状态查询后台类目信息
     * @param status 状态
     * @return backCategoryList
     */
    Response<Paging<BackCategory>> pagingByStatus(Integer pageNo, Integer pageSize, Integer status);


    /**
     * 根据状态查询后台类目信息(剔除已经同步过的记录)
     * @param status  状态
     * @param channel 同步渠道
     * @return backCategoryList
     */
    Response<Paging<BackCategory>> pagingByNotSync(Integer pageNo, Integer pageSize, Integer status, Integer channel);

    /**
     * 根据外部id获取类目信息
     * @param outId 外部id
     * @return 类目信息
     */
    Response<BackCategory> findBackCategoryByOutId(String outId);
 }
