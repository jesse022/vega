package com.sanlux.store.impl.executor;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sanlux.store.enums.StoreImportsStatus;
import com.sanlux.store.impl.dao.StoreImportsDao;
import com.sanlux.store.model.StoreImports;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;
import io.terminus.parana.store.impl.dao.RepertoryMapper;
import io.terminus.parana.store.impl.manager.LocationsManager;
import io.terminus.parana.store.model.Repertory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 进销存批量导入线程池
 * Created by lujm on 2017/4/27.
 */
@Slf4j
@Component
public class StoreImportExecutor {

    private final ExecutorService executorService;

    @Autowired
    private LocationsManager locationsManager;

    @Autowired
    private StoreImportsDao storeImportsDao;

    @Autowired
    private  RepertoryMapper repertoryMapper;


    public StoreImportExecutor() {
        this.executorService = new ThreadPoolExecutor(2, 4, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1000),
                new ThreadFactoryBuilder().setNameFormat("trade-store-import-%d").build(),
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
                if (!Objects.isNull(storeToUploads)) {
                    //库位单导入
                    handleStoreToUpload(userId,storeToUploads, key, sonKey, id);
                }
                if (!Objects.isNull(storePreInDtos)) {
                    //入库单导入
                    handleStorePreInToUpload(userId, storePreInDtos, key, sonKey, id);
                }
            }catch(Exception e){
                log.warn("process store import key:{},sonKey:{},cause:{}", key, sonKey,e.getMessage());
            }
        }

        /**
         * 入库单批量导入操作
         * @param userId 用户ID
         * @param storePreInDtos list
         * @param key 批次号
         * @param sonKey 子批次号
         * @param id 批次主键
         */
        public void handleStorePreInToUpload(Long userId, List<StorePreInDto> storePreInDtos, String key, Integer sonKey, Long id) {
            StoreImports storeImports = new StoreImports();
            try {
                try {
                    //开始批处理之前先更新状态为"执行中"
                    storeImports.setStatus(StoreImportsStatus.IN_HAND.value());
                    storeImports.setId(id);
                    storeImportsDao.update(storeImports);
                } catch (Exception e3) {
                    log.error("update storeImports failed, storeImports:{}", storeImports);
                }

                List<Long> repertoryIds = Lists.transform(storePreInDtos, StorePreInDto::getRepertoryId);
                checkRights(userId, repertoryIds);
                locationsManager.batchPreInAndLocation(userId, storePreInDtos);

                try {
                    //批量执行成功时,记录日志
                    storeImports.setStatus(StoreImportsStatus.SUCCESS.value());
                    storeImports.setResult("第" + sonKey + "批次导入成功,共导入" + storePreInDtos.size() + "条记录");
                    storeImports.setId(id);
                    storeImportsDao.update(storeImports);
                } catch (Exception e2) {
                    log.error("update storeImports failed, storeImports:{}", storeImports);
                }
            } catch (Exception e) {
                log.error("handle store pre in data error, cause:{}", Throwables.getStackTraceAsString(e));
                try {
                    //批量执行失败
                    String result = "第" + sonKey + "批次,从'仓库ID:" + storePreInDtos.get(0).getRepertoryId()
                            + ",库位ID:" + storePreInDtos.get(0).getLocationId()
                            + ",SKUID:" + storePreInDtos.get(0).getSkuId()
                            + "'这一行开始的" + storePreInDtos.size()
                            + "条记录导入失败,原因:" + e.getMessage();
                    storeImports.setStatus(StoreImportsStatus.FAIL.value());//失败标志
                    storeImports.setResult(result);
                    storeImports.setId(id);
                    storeImportsDao.update(storeImports);
                } catch (Exception e2) {
                    log.error("update storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e2));
                }
            }
        }

        /**
         * 入库单批量导入操作
         * @param userId 用户ID
         * @param storeToUploads list
         * @param key 批次号
         * @param sonKey 子批次号
         * @param id 批次主键
         */
        public void handleStoreToUpload(Long userId, List<StoreUploadDto> storeToUploads,String key,Integer sonKey,Long id) {
            StoreImports storeImports = new StoreImports();
            try {
                try {
                    //开始批处理之前先更新状态为"执行中"
                    storeImports.setId(id);
                    storeImports.setStatus(StoreImportsStatus.IN_HAND.value());
                    storeImportsDao.update(storeImports);
                } catch (Exception e3) {
                    log.error("update storeImports failed, storeImports:{}", storeImports);
                }

                List<Long> repertoryIds = Lists.transform(storeToUploads, StoreUploadDto::getRepertoryId);
                checkRights(userId, repertoryIds);
                locationsManager.batchCreate(storeToUploads);

                try {
                    //批量执行成功
                    storeImports.setResult("第" + sonKey + "批次导入成功,共导入" + storeToUploads.size() + "条记录");
                    storeImports.setStatus(StoreImportsStatus.SUCCESS.value());//执行成功
                    storeImports.setId(id);
                    storeImportsDao.update(storeImports);
                } catch (Exception e2) {
                    log.error("update storeImports failed, storeImports:{}", storeImports);
                }
            } catch (Exception e) {
                log.error("handle store data error, cause:{}", Throwables.getStackTraceAsString(e));
                try {
                    //批量执行失败
                    String result="第"+sonKey+"批次,从'仓库ID:"+storeToUploads.get(0).getRepertoryId()
                            +",库位:"+storeToUploads.get(0).getLocationName()
                            +",SKUID:"+storeToUploads.get(0).getSkuId()
                            +"'这一行开始的"+storeToUploads.size()
                            +"条记录导入失败,原因:"+e.getMessage();
                    storeImports.setStatus(StoreImportsStatus.FAIL.value());//失败标志
                    storeImports.setResult(result);
                    storeImports.setId(id);
                    storeImportsDao.update(storeImports);
                } catch (Exception e2) {
                    log.error("update storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e2));
                }
            }
        }

        /**
         * 权限验证
         * @param userId 用户ID
         * @param repertoryIds 仓库IDs
         */
        private void checkRights(Long userId, List<Long> repertoryIds) {
            try {
                Set<Long> ids = new HashSet<>(repertoryIds);
                List<Repertory> repertories = repertoryMapper.selectByPrimaryKeys(ids);
                if (ids.size() != repertories.size()) {
                    log.error("check rights fail, userId:{}, cause: some repertory not find", userId);
                    throw new ServiceException("some.repertory.not.find");
                }
                List<Long> ownerIds = Lists.transform(repertories, Repertory::getOwnerId);
                Set<Long> ownerIdSet = Sets.newHashSet(ownerIds);
                if (ownerIdSet.size() != 1 || !ownerIdSet.contains(userId)) {
                    log.error("check rights fail, userId:{}", userId);
                    throw new ServiceException("check.rights.fail");
                }

            } catch (Exception e) {
                log.error("check rights fail, userId:{}, cause:{}", userId, e.getMessage());
                throw new ServiceException(e.getMessage());
            }
        }
    }
}

