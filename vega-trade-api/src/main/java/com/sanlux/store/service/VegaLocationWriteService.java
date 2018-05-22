package com.sanlux.store.service;

import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;

import java.util.List;

/**
 * Created by lujm on 2017/3/15.
 */
public interface VegaLocationWriteService {

    /**
     * 批量导入入库单信息
     * @param userId 用户ID
     * @param storePreInDtos 入库单信息
     * @param key 导入批次
     * @param sonKey 导入子批次
     * @param id 导入批次主键
     * @return 是否成功
     */
    void handleStorePreInToUpload(Long userId, List<StorePreInDto> storePreInDtos,String key,Integer sonKey,Long id);

    /**
     * 批量导入库位信息
     * @param userId 用户ID
     * @param storeToUploads 库位信息
     * @param key 导入批次
     * @param sonKey 导入子批次
     * @param id 导入批次主键
     * @return 是否成功
     */
    void handleStoreToUpload(Long userId, List<StoreUploadDto> storeToUploads, String key, Integer sonKey, Long id);
}
