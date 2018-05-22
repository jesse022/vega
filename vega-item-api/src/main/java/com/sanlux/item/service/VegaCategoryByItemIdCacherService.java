package com.sanlux.item.service;

import io.terminus.common.model.Response;

/**
 * Created by cuiwentao
 * on 16/8/19
 */
public interface VegaCategoryByItemIdCacherService {

    /**
     * 根据商品ID从缓存中拿到一级类目ID
     * @param itemId 商品ID
     * @return 一级类目ID
     */
    Response<Long> findByItemId(Long itemId);
}
