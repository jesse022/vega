package com.sanlux.category.service;

import io.terminus.common.model.Response;

/**
 * Created by jesselu on 2017/2/9.
 */
public interface FrontCategoryExtWriteService {
    /**
     * 根据类目ID修改父类目ID和类目级别信息
     * @param pid 父类目ID
     * @param  level 类目级别
     * @param id 类目ID
     * @return Boolean
     */
    Response<Boolean> updatePidAndLevel(Long pid, Integer level, Long id);
}
