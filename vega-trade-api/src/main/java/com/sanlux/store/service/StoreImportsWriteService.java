package com.sanlux.store.service;

import com.sanlux.store.model.StoreImports;
import io.terminus.common.model.Response;

/**
 * 进销存批量导入写服务
 * Created by lujm on 2017/3/16.
 */
public interface StoreImportsWriteService {
    /**
     * 创建StoreImports
     * @param storeImports
     * @return 主键id
     */
    Response<Long> create(StoreImports storeImports);

    /**
     * 创建StoreImports
     * @param storeImports
     * @return 是否成功
     */
    Response<Boolean> update(StoreImports storeImports);
}
