package com.sanlux.category.service;

import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;

import java.util.List;
import java.util.Map;

/**
 * Created by jesselu on 2017/2/9.
 */
public interface FrontCategoryExtReadService {
    /**
     * 根据类目ID获取非叶子类目信息
     *
     * @param id 类目ID
     * @return List<FrontCategory>
     */
    Response<List<FrontCategory>> findCategoryByIdAndHasChildren (Long id);

    /**
     * 判断类目信息是否合法
     *
     * @param criteria 类目信息
     * @return Boolean
     */
    Response<Long> checkCategoryByIdAndLevelAndHasChildren (Map<String, Object> criteria);
}
