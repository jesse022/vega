package com.sanlux.item.impl.internal;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sanlux.item.service.ItemImportWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Created by cuiwentao
 * on 16/10/17
 */
@Slf4j
@Component
public class ItemImportExecutor {


    private final ExecutorService executorService;

    public ItemImportExecutor() {
        this.executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(1000),
                new ThreadFactoryBuilder().setNameFormat("item-indexer-%d").build(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
                        ImportTask importTask = (ImportTask) runnable;
                        log.error("product import(handler id={}) index request is rejected", importTask.handlerId);
                    }
                });
    }


    public void doImport(ItemImportWriteService writeService, Long handlerId, Integer type) {
        ImportTask task = new ImportTask(writeService, handlerId, type);
        this.executorService.submit(task);
    }

    private class ImportTask implements Runnable {

        private final Long handlerId;
        private final Integer type;
        private final ItemImportWriteService writeService;

        private ImportTask(ItemImportWriteService writeService, Long handlerId, Integer type) {
            this.writeService = writeService;
            this.handlerId = handlerId;
            this.type = type;
        }

        @Override
        public void run() {
            Response<Boolean> resp = writeService.doImportItem(handlerId, type);
            if (!resp.isSuccess()) {
                log.error("do import product failed, handlerId={}, error={}",
                        handlerId, resp.getError());
            }
        }
    }
}
