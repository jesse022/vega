package com.sanlux.web.front.executor;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sanlux.store.service.VegaLocationWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.Objects;

/**
 * 进销存批量导入操作线程池
 * Created by lujm on 2017/3/17.
 */
@Slf4j
@Component
public class BatchImportStoreExecutor {

    private final ExecutorService executorService;

    @RpcConsumer
    private VegaLocationWriteService vegaLocationWriteService;


    public BatchImportStoreExecutor() {
        this.executorService = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(1000),
                new ThreadFactoryBuilder().setNameFormat("store-import-%d").build(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
                        ImportTask importTask = (ImportTask) runnable;
                            log.error("store batch import (ImportTask:{}) request is rejected", importTask);
                    }
                });
    }


    public void doImport(Long userId, List<StorePreInDto> storePreInDtos, List<StoreUploadDto> storeToUploads, String key, Integer sonKey, Long id) {
        ImportTask task = new ImportTask(userId,storePreInDtos,storeToUploads,key,sonKey,id);
        this.executorService.submit(task);
    }

    private class ImportTask implements Runnable {
        private final Long userId;
        private final List<StorePreInDto> storePreInDtos;
        private final List<StoreUploadDto> storeToUploads;
        private final String key;
        private final Integer sonKey;
        private final Long id;

        private ImportTask(Long userId, List<StorePreInDto> storePreInDtos, List<StoreUploadDto> storeToUploads, String key, Integer sonKey, Long id) {
            this.userId = userId;
            this.storePreInDtos=storePreInDtos;
            this.storeToUploads=storeToUploads;
            this.key=key;
            this.sonKey=sonKey;
            this.id=id;
        }

        @Override
        public void run() {
            try {
                log.info("process store import key:{},sonKey:{}", key, sonKey);
                if (!Objects.isNull(storePreInDtos)) {
                    //入库单
                    vegaLocationWriteService.handleStorePreInToUpload(userId, storePreInDtos,
                            key, sonKey, id);
                }
                if (!Objects.isNull(storeToUploads)) {
                    //库位单
                    vegaLocationWriteService.handleStoreToUpload(userId,storeToUploads,
                            key, sonKey, id);
                }
            }catch(Exception e){
                log.warn("process store import key:{},sonKey:{},cause:{}", key, sonKey,e.getMessage());
            }
        }
    }
}
