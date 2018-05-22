package com.sanlux.store.impl.service;

import com.sanlux.store.impl.executor.StoreImportExecutor;
import com.sanlux.store.service.VegaLocationWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 进销存批量导入写服务实现类
 * Created by lujm on 2017/3/15.
 */
@Service
@RpcProvider
@Slf4j
public class VegaLocationWriteServiceImpl implements VegaLocationWriteService {
    private final StoreImportExecutor storeImportExecutor;

    @Autowired
    public VegaLocationWriteServiceImpl(StoreImportExecutor storeImportExecutor) {
        this.storeImportExecutor = storeImportExecutor;
    }

    @Override
    public void handleStorePreInToUpload(Long userId, List<StorePreInDto> storePreInDtos, String key, Integer sonKey, Long id) {
        storeImportExecutor.doImport(userId,storePreInDtos,null,key,sonKey,id);//调用线程池进行批量导入操作
    }

    @Override
    public void handleStoreToUpload(Long userId, List<StoreUploadDto> storeToUploads,String key,Integer sonKey,Long id) {
        storeImportExecutor.doImport(userId,null,storeToUploads,key,sonKey,id);//调用线程池进行批量导入操作
    }
}
