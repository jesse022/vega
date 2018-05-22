package com.sanlux.web.front.executor;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sanlux.store.enums.StoreImportsStatus;
import com.sanlux.store.enums.StoreImportsType;
import com.sanlux.store.model.StoreImports;
import com.sanlux.store.service.StoreImportsWriteService;
import com.sanlux.store.service.VegaLocationWriteService;
import com.sanlux.web.front.core.util.ObjectUtil;
import com.sanlux.web.front.dto.StoreImportDto;
import com.sanlux.web.front.queue.StoreQueueConsumer;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;
import io.terminus.parana.store.dto.UploadRaw;
import io.terminus.parana.store.web.util.ExcelUtil;
import io.terminus.parana.store.web.util.UploadHelper;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * 进销存批量导入循环获取队列线程池
 * Created by lujm on 2017/4/10.
 */
@Slf4j
@Component
public class StoreImportQueueExecutor {

    @Autowired
    private StoreQueueConsumer storeQueueConsumer;

    @Autowired
    private BatchImportStoreExecutor batchImportStoreExecutor;

    @RpcConsumer
    private StoreImportsWriteService storeImportsWriteService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private VegaLocationWriteService vegaLocationWriteService;

    @PostConstruct
    public void doIndex() {
        new Thread(new IndexTask()).start();
    }

    class IndexTask implements Runnable {
        @Override
        public void run() {
            log.info("start store import");
            while (true) {
                try {
//                    byte[] content = storeQueueConsumer.pop();
                    byte[] content = null;
                    if (!Objects.isNull(content)) {
                        StoreImportDto storeImportDto = (StoreImportDto) ObjectUtil.bytes2Object(content);
                        Thread.sleep(500);
                        log.info("batch store import key:{},path:{}", storeImportDto.getKey(), storeImportDto.getPath());
                        Boolean isSuccess = false;
                        Map<Boolean,String> returnMap = batchImport(storeImportDto.getPath(),storeImportDto.getUserID(),storeImportDto.getType(),storeImportDto.getKey());
                        StoreImports storeImports = new StoreImports();
                        for (Map.Entry entry : returnMap.entrySet()) {
                            Object key = entry.getKey( );
                            if(key.equals(Boolean.TRUE)){
                                isSuccess=true;
                                break;
                            }
                        }
                        if(isSuccess) {
                            //成功
                            storeImports.setStatus(StoreImportsStatus.SUCCESS.value());
                            storeImports.setResult(returnMap.get(Boolean.TRUE));
                        }else {
                            storeImports.setStatus(StoreImportsStatus.FAIL.value());
                            storeImports.setResult(returnMap.get(Boolean.FALSE));
                        }
                        storeImports.setId(storeImportDto.getId());
                        storeImportsWriteService.update(storeImports);
                    }
                } catch (Exception e) {
                    log.warn("fail to batch store import, cause:{}", e.getMessage());
                }
            }
        }

        /**
         * 线程池中调用,分析完没有问题进行导入操作
         * @param path 文件路径
         * @param userId 用户ID
         * @param type 上传类型 1:库位 2:入库单
         * @param key 导入批次号
         * @return 是否成功
         */
        public Map<Boolean,String> batchImport (String path, Long userId, Integer type,String key) {
            Map<Boolean, String> isSuccess=Maps.newHashMapWithExpectedSize(1);
            try {
                // 下载下来excel
                UploadRaw rawData = new UploadRaw();
                try {
                    URL url = new URL("http:" + path.replaceAll("http:", ""));
                    if(log.isDebugEnabled()){
                        log.debug("start analyze excel from network ");
                    }
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(30000);
                    if (type == 1) {
                        //库位导入
                        rawData =  ExcelUtil.analyzeStoreExcel(conn.getInputStream());
                    } else {
                        //入库单导入
                        rawData = ExcelUtil.analyzePreInExcel(conn.getInputStream());
                    }
                    conn.disconnect();
                    if(log.isDebugEnabled()){
                        log.debug("end analyze excel from network");
                    }
                } catch (MalformedURLException e) {
                    log.error("failed to parse excel, error : {}", e.getMessage());
                    isSuccess.put(Boolean.FALSE,e.getMessage());
                    return isSuccess;
                } catch (IOException e) {
                    log.error("failed to parse excel, cause : {}", e.getMessage());
                    isSuccess.put(Boolean.FALSE,e.getMessage());
                    return isSuccess;
                } catch (Exception e){
                    log.error("failed to parse excel, cause : {}", Throwables.getStackTraceAsString(e));
                    isSuccess.put(Boolean.FALSE,e.getMessage());
                    return isSuccess;
                }
                Response<Map<Boolean,String>> response;
                if (type == 1) {
                    response = uploadStoreToImportRaw(userId, rawData, key);
                } else {
                    response  = uploadPreInToImportRaw(userId, rawData, key);
                }
                if (!response.isSuccess()) {
                    log.error("batch import fail, data error, cause:{}", response.getError());
                    isSuccess.put(Boolean.FALSE,response.getError());
                    return isSuccess;
                }
                return response.getResult();
            }catch (Exception e) {
                log.error("update store excel failed, cause:{}", Throwables.getStackTraceAsString(e));
                isSuccess.put(Boolean.FALSE,e.getMessage());
                return isSuccess;
            }
        }

        /**
         * 入库单批量导入
         * @param userId 用户ID
         * @param rawData 导入数据
         * @param key 批次
         * @return
         */
        private Response<Map<Boolean,String>> uploadPreInToImportRaw(Long userId, UploadRaw rawData,String key) {
            Map<Boolean, String> isSuccess=Maps.newHashMapWithExpectedSize(1);
            if (rawData == null) {
                log.error("upload store excel fail, data error");
                isSuccess.put(Boolean.FALSE,"upload store excel fail, data error");
                return Response.ok(isSuccess);
            }
            try {
                UploadHelper uploadHelper = new UploadHelper(rawData, Sets.newHashSet("sku_id"));
                Integer lineNum = uploadHelper.lineCount();
                if (lineNum <= 0) {
                    // TODO: 暂时 0 代表终结
                    isSuccess.put(Boolean.FALSE,"upload store excel fail, data empty");
                    return Response.ok(isSuccess);
                }

                List<StorePreInDto> storePreInDtoList = Lists.newArrayList();
                for (int loop = 0; loop < lineNum; loop++) {
                    UploadHelper.LineHelper line = uploadHelper.getLine(loop);
                    StorePreInDto storePreInDto = new StorePreInDto();

                    storePreInDto.setRepertoryId(line.getValue("repertory_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                    storePreInDto.setLocationId(line.getValue("location_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                    storePreInDto.setSkuId(line.getValue("sku_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                    storePreInDto.setQuantity(line.getValue("quantity", false, UploadHelper.INT_VALUE_PROCESSOR));
                    storePreInDto.setPrice(line.getValue("price", false, UploadHelper.INT_VALUE_PROCESSOR));
                    storePreInDto.setMaxCanLocation(line.getValue("max_can_content", true, UploadHelper.INT_VALUE_PROCESSOR));
                    if (storePreInDto.getQuantity() != null && storePreInDto.getPrice() != null) {
                        if (storePreInDto.getMaxCanLocation() < storePreInDto.getQuantity()) {
                            log.error("locationId:{} quantity > max can content",storePreInDto.getLocationId());
                            isSuccess.put(Boolean.FALSE,"locationId:"+storePreInDto.getLocationId()+" quantity > max can content");
                            return Response.ok(isSuccess);
                        }
                        storePreInDtoList.add(storePreInDto);
                    }
                }
                fillItemInfo(storePreInDtoList);
                List<List<StorePreInDto>> lists = createPreInList(storePreInDtoList);

                Response<User> resp = userReadService.findById(userId);
                String userName=null;
                if (!resp.isSuccess()) {
                    log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
                }else {
                    userName=resp.getResult().getName();
                }
                int sonKey=1;
                for (List<StorePreInDto> list : lists) {
                    //开始导入之前,先把日志标记为未开始
                    StoreImports storeImports = new StoreImports();
                    Long id=0L;
                    try {
                        storeImports.setUserId(userId);
                        storeImports.setUserName(userName);
                        storeImports.setBatchNo(key);
                        storeImports.setSonBatchNo(sonKey);
                        storeImports.setStatus(StoreImportsStatus.NOT_STARTED.value());//未开始
                        storeImports.setType(StoreImportsType.IN_STORE.value());//入库单导入
                        Response<Long> res = storeImportsWriteService.create(storeImports);
                        if(res.isSuccess()){
                            id=res.getResult();
                        }
                    }catch (Exception e2) {
                        log.error("create storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e2));
                    }
                    //batchImportStoreExecutor.doImport(userId,list,null,key,sonKey,id);//调用线程池进行批量导入操作
                    vegaLocationWriteService.handleStorePreInToUpload(userId,list,key,sonKey,id);
                    sonKey++;
                }
                isSuccess.put(Boolean.TRUE,"第" +key + "批次已开始导入,共计"+(sonKey-1)+"子批次,具体导入情况请查看各子批次日志详情.");
                return Response.ok(isSuccess);
            }catch (ServiceException e) {
                log.error("update store excel failed, userId:{}, cause:{}",
                        userId, Throwables.getStackTraceAsString(e));
                isSuccess.put(Boolean.FALSE,e.getMessage());
                return Response.ok(isSuccess);
            } catch (Exception e) {
                log.error("update store excel failed, userId:{}, cause:{}",
                        userId, Throwables.getStackTraceAsString(e));
                isSuccess.put(Boolean.FALSE,e.getMessage());
                return Response.ok(isSuccess);
            }
        }
        /**
         * 库位批量导入
         * @param userId 用户ID
         * @param rawData 导入数据
         * @param key 批次
         * @return
         */
        private Response<Map<Boolean,String>> uploadStoreToImportRaw(Long userId, UploadRaw rawData,String key) {
            Map<Boolean, String> isSuccess=Maps.newHashMapWithExpectedSize(1);
            if (rawData == null) {
                log.error("upload store excel fail, data error");
                isSuccess.put(Boolean.FALSE,"upload store excel fail, data error");
                return Response.ok(isSuccess);
            }
            try {
                UploadHelper uploadHelper = new UploadHelper(rawData, Sets.newHashSet("sku_id"));
                Integer lineNum = uploadHelper.lineCount();
                if (lineNum <= 0) {
                    // TODO: 暂时 0 代表终结
                    isSuccess.put(Boolean.FALSE,"upload store excel fail, data empty");
                    return Response.ok(isSuccess);
                }

                List<StoreUploadDto> storeToUploads = Lists.newArrayList();
                for (int loop = 0; loop < lineNum; loop++) {
                    UploadHelper.LineHelper line = uploadHelper.getLine(loop);

                    StoreUploadDto storeToUpload = new StoreUploadDto();
                    storeToUpload.setRepertoryId(line.getValue("repertory_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                    storeToUpload.setAreaName(line.getValue("area_name", true, UploadHelper.STRING_VALUE_PROCESSOR));
                    storeToUpload.setGroupName(line.getValue("group_name", true, UploadHelper.STRING_VALUE_PROCESSOR));
                    storeToUpload.setShelfName(line.getValue("shelf_name", true, UploadHelper.STRING_VALUE_PROCESSOR));
                    storeToUpload.setLocationName(line.getValue("location_name", true, UploadHelper.STRING_VALUE_PROCESSOR));
                    storeToUpload.setSkuId(line.getValue("sku_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                    storeToUpload.setMaxContent(line.getValue("max_content", true, UploadHelper.INT_VALUE_PROCESSOR));
                    storeToUpload.setWarnContent(line.getValue("warn_content", false, UploadHelper.INT_VALUE_PROCESSOR));

                    storeToUploads.add(storeToUpload);
                }
                fillItemAndCategoryId(storeToUploads);
                List<List<StoreUploadDto>> lists = createStoreList(storeToUploads);

                Response<User> resp = userReadService.findById(userId);
                String userName=null;
                if (!resp.isSuccess()) {
                    log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
                }else {
                    userName=resp.getResult().getName();
                }

                int sonKey=1;
                for (List<StoreUploadDto> list : lists) {
                    //开始导入之前,先把日志标记为未开始
                    StoreImports storeImports = new StoreImports();
                    Long id=0L;
                    try {
                        storeImports.setUserId(userId);
                        storeImports.setUserName(userName);
                        storeImports.setBatchNo(key);
                        storeImports.setSonBatchNo(sonKey);
                        storeImports.setStatus(StoreImportsStatus.NOT_STARTED.value());//未开始
                        storeImports.setType(StoreImportsType.LOCATION.value());//库位导入
                        Response<Long> res = storeImportsWriteService.create(storeImports);
                        if(res.isSuccess()){
                            id=res.getResult();
                        }
                    }catch (Exception e2) {
                        log.error("create storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e2));
                    }
                    //batchImportStoreExecutor.doImport(userId,null,list,key,sonKey,id);//调用线程池进行批量导入操作
                    vegaLocationWriteService.handleStoreToUpload(userId,list,key,sonKey,id);
                    sonKey++;
                }
                isSuccess.put(Boolean.TRUE,"第" +key + "批次已开始导入,共计"+(sonKey-1)+"子批次,具体导入情况请查看各子批次日志详情.");
                return Response.ok(isSuccess);
            } catch (ServiceException e) {
                log.error("update store excel failed, cause:{}", e.getMessage());
                isSuccess.put(Boolean.FALSE,e.getMessage());
                return Response.ok(isSuccess);
            } catch (Exception e) {
                log.error("update store excel failed, cause:{}", Throwables.getStackTraceAsString(e));
                isSuccess.put(Boolean.FALSE,e.getMessage());
                return Response.ok(isSuccess);
            }
        }

        private void fillItemAndCategoryId(List<StoreUploadDto> storeToUploads) {
            List<Long> skuIds = Lists.transform(storeToUploads, StoreUploadDto::getSkuId);
            Set<Long> skuIdSet = Sets.newHashSet();
            skuIdSet.addAll(skuIds);
            List<Sku> skus = Lists.newArrayList();
            List<List<Long>> skuIdList = createList(new ArrayList<>(skuIdSet));

            for (List<Long> cIds : skuIdList) {
                skus.addAll(findSkusByIds(cIds));
            }

            Map<Long, Sku> skuIndexById = Maps.uniqueIndex(skus, Sku::getId);
            List<Long> itemIds = Lists.transform(skus, Sku::getItemId);
            Set<Long> itemIdSet = Sets.newHashSet();
            itemIdSet.addAll(itemIds);

            List<Item> items = Lists.newArrayList();
            List<List<Long>> itemIdList = createList(new ArrayList<>(itemIdSet));
            for (List<Long> cIds : itemIdList) {
                items.addAll(findItemByIds(cIds));
            }

            Map<Long, Item> itemIndexById = Maps.uniqueIndex(items, Item::getId);
            storeToUploads.forEach(storeUploadDto -> {
                Sku sku = skuIndexById.get(storeUploadDto.getSkuId());
                if (sku == null) {
                    log.error("skuId:{} error, not find sku", storeUploadDto.getSkuId());
                    throw new ServiceException("sku.not.found");
                }
                storeUploadDto.setItemId(sku.getItemId());
                Item item = itemIndexById.get(storeUploadDto.getItemId());
                if (item == null) {
                    log.error("itemId:{} error, not find item", storeUploadDto.getItemId());
                    throw new ServiceException("item.not.found");
                }
                storeUploadDto.setCategoryId(item.getCategoryId());
            });
        }

        private void fillItemInfo(List<StorePreInDto> storePreInDtoList) {
            List<Long> skuIds = Lists.transform(storePreInDtoList, StorePreInDto::getSkuId);
            Set<Long> skuIdSet = Sets.newHashSet();
            skuIdSet.addAll(skuIds);
            List<Sku> skus = Lists.newArrayList();
            List<List<Long>> skuIdList = createList(new ArrayList<>(skuIdSet));

            for (List<Long> cIds : skuIdList) {
                skus.addAll(findSkusByIds(cIds));
            }

            Map<Long, Sku> skuIndexById = Maps.uniqueIndex(skus, Sku::getId);
            List<Long> itemIds = Lists.transform(skus, Sku::getItemId);
            Set<Long> itemIdSet = Sets.newHashSet();
            itemIdSet.addAll(itemIds);

            List<Item> items = Lists.newArrayList();
            List<List<Long>> itemIdList = createList(new ArrayList<>(itemIdSet));
            for (List<Long> cIds : itemIdList) {
                items.addAll(findItemByIds(cIds));
            }

            Map<Long, Item> itemIndexById = Maps.uniqueIndex(items, Item::getId);
            storePreInDtoList.forEach(storePreInDto -> {
                storePreInDto.setItemName(itemIndexById.get(skuIndexById.get(storePreInDto.getSkuId()).getItemId()).getName());
                storePreInDto.setItemImage(itemIndexById.get(skuIndexById.get(storePreInDto.getSkuId()).getItemId()).getMainImage());

            });

        }

        private List<Sku> findSkusByIds (List<Long> skuIds) {
            Response<List<Sku>> skuResp = skuReadService.findSkusByIds(skuIds);
            if (!skuResp.isSuccess()) {
                log.error("find sku by ids:{} fail, cause:{}", skuIds, skuResp.getError());
                throw new ServiceException(skuResp.getError());
            }
            return skuResp.getResult();
        }

        private List<Item> findItemByIds (List<Long> itemIds) {
            Response<List<Item>> itemResp = itemReadService.findByIds(itemIds);
            if (!itemResp.isSuccess()) {
                log.error("find item by ids:{} fail, cause:{}", itemIds, itemResp.getError());
                throw new ServiceException(itemResp.getError());
            }
            return itemResp.getResult();
        }

        private List<List<Long>> createList(List<Long> targe) {
            int size = 1000;
            List<List<Long>> listArr = Lists.newArrayList();
            //获取被拆分的数组个数
            int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
            for (int i = 0; i < arrSize; i++) {
                List<Long> sub = Lists.newArrayList();
                //把指定索引数据放入到list中
                for (int j = i * size; j <= size * (i + 1) - 1; j++) {
                    if (j <= targe.size() - 1) {
                        sub.add(targe.get(j));
                    }
                }
                listArr.add(sub);
            }
            return listArr;
        }

        private List<List<StoreUploadDto>> createStoreList(List<StoreUploadDto> targe) {
            int size = 1000;
            List<List<StoreUploadDto>> listArr = Lists.newArrayList();
            //获取被拆分的数组个数
            int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
            for (int i = 0; i < arrSize; i++) {
                List<StoreUploadDto> sub = Lists.newArrayList();
                //把指定索引数据放入到list中
                for (int j = i * size; j <= size * (i + 1) - 1; j++) {
                    if (j <= targe.size() - 1) {
                        sub.add(targe.get(j));
                    }
                }
                listArr.add(sub);
            }
            return listArr;
        }

        private List<List<StorePreInDto>> createPreInList(List<StorePreInDto> targe) {
            int size = 1000;
            List<List<StorePreInDto>> listArr = Lists.newArrayList();
            //获取被拆分的数组个数
            int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
            for (int i = 0; i < arrSize; i++) {
                List<StorePreInDto> sub = Lists.newArrayList();
                //把指定索引数据放入到list中
                for (int j = i * size; j <= size * (i + 1) - 1; j++) {
                    if (j <= targe.size() - 1) {
                        sub.add(targe.get(j));
                    }
                }
                listArr.add(sub);
            }
            return listArr;
        }

    }
}
