package com.sanlux.item.service;

import com.sanlux.item.model.ItemImport;
import io.terminus.common.model.Response;

/**
 * Created by lujm on 2018/3/20.
 */
public interface ItemImportReadService {

    /**
     * 查询导入结果
     * @param id 批次Id
     * @return 结果
     */
    Response<ItemImport> findById(Long id);
}
