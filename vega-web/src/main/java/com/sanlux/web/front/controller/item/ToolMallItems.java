package com.sanlux.web.front.controller.item;

import com.alibaba.dubbo.rpc.RpcException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.item.dto.api.*;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.dto.toolmall.ToolMallCategoryReturn;
import com.sanlux.item.dto.toolmall.ToolMallItemAllReturn;
import com.sanlux.item.enums.ToolMallItemImportStatus;
import com.sanlux.item.enums.ToolMallItemImportType;
import com.sanlux.item.enums.VegaItemImportType;
import com.sanlux.item.model.ToolMallItemSync;
import com.sanlux.item.model.ToolMallItemSyncLog;
import com.sanlux.item.service.ItemImportWriteService;
import com.sanlux.item.service.ToolMallItemSyncLogWriteService;
import com.sanlux.item.service.ToolMallItemSyncWriteService;
import com.sanlux.web.front.core.toolmall.ToolMallAPIRequest;
import com.sanlux.web.front.core.util.ItemUploadExcelAnalyzer;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 土猫网商品导入
 * Created by lujm on 2018/4/16.
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/item/tool-mall")
public class ToolMallItems {

    @RpcConsumer
    private ItemImportWriteService itemImportWriteService;

    @Autowired
    private ToolMallAPIRequest toolMallAPIRequest;

    @Autowired
    private VegaItemImport vegaItemImport;

    @RpcConsumer
    private ToolMallItemSyncLogWriteService toolMallItemSyncLogWriteService;

    @RpcConsumer
    private ToolMallItemSyncWriteService toolMallItemSyncWriteService;

    /**
     *土猫网商品批量Excel导入
     *
     * @param file Excel文件
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String itemBatchImportByExcel(MultipartFile file) {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            UploadRaw rawData = ItemUploadExcelAnalyzer.analyze(file.getInputStream());
            Response<Long> uploadRes = itemImportWriteService.importItemsByExcel(user.getShopId(), rawData);
            if (!uploadRes.isSuccess()) {
                log.error("batch import item by excel failed, cause:{}", uploadRes.getError());
                throw new JsonResponseException(uploadRes.getError());
            }

            return uploadRes.getResult().toString();
        } catch (ServiceException e) {
            String error = e.getMessage();
            log.error("batch import item by excel failed, service exception:{}", error);
            throw new JsonResponseException(error);

        } catch (IOException e) {
            log.error("batch import item by excel failed, analyze excel failed, cause:{}", e.getMessage());
            throw new JsonResponseException("upload.failed.analyze.excel.failed");

        } catch (RpcException e) {
            log.error("batch import item by excel failed, invoke product upload write service failed, cause:{}", e.getMessage());
            throw new JsonResponseException("upload.failed.invoke.product.upload.write.service.failed");

        } catch (Exception e) {
            log.error("batch import item by excel failed, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * 类目手工同步接口
     * @param beginTime1   查询开始时间
     * @param endTime1     查询结束时间
     * @return 是否成功
     */
    @RequestMapping(value = "/category", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean categoryBatchImportByApi(@RequestParam(value = "beginTime", required = false) String beginTime1,
                                            @RequestParam(value = "endTime", required = false) String endTime1) {
        // 查询商品类目，设置开始日期和结束日期
        //// TODO: 2018/4/16 测试先默认 
        String beginTime = "2018-04-04 10:00:00";
        String endTime = "2018-04-09 10:00:00";
        Response<ToolMallCategoryReturn> resReturn = toolMallAPIRequest.getCategoryList(beginTime, endTime);
        if (!resReturn.isSuccess()) {
            log.error("batch import tool mall category by api failed, beginTime:{}, endTime:{}, cause:{}",
                    beginTime, endTime, resReturn.getError());
            throw new JsonResponseException(resReturn.getError());
        }

        ToolMallItemSyncLog toolMallItemSyncLog = new ToolMallItemSyncLog();
        toolMallItemSyncLog.setType(ToolMallItemImportType.CATEGORY.value());
        toolMallItemSyncLog.setBeginTime(beginTime);
        toolMallItemSyncLog.setEndTime(endTime);

        ToolMallCategoryReturn result = resReturn.getResult();
        if (Arguments.isNull(result) || Arguments.isNullOrEmpty(result.getData())) {
            toolMallItemSyncLog.setErrorResult("无返回数据");
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE ;
        }

        if (!Objects.equals(result.getCode(), 0)) {
            // 不成功标志
            toolMallItemSyncLog.setErrorResult(result.getMessage());
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE ;
        }

        Boolean isSuccess = categoryBatchImport(result.getData());

        if (!isSuccess) {
            toolMallItemSyncLog.setErrorResult("集乘网内部错误,具体需看日志");
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE ;
        }
        toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.SUCCESS.value());
        updateToolMallSyncNextBeginTimeByType(result.getNextBeginTime(), ToolMallItemImportType.CATEGORY.value());
        syncLogCreate(toolMallItemSyncLog);

        return Boolean.TRUE ;
    }


    /**
     * 商品手工同步接口,目前暂定全部采用全量(ItemSyncType.ALL)方式的同步类型
     * @param beginTime1  查询开始时间
     * @param endTime1    查询结束时间
     * @param pageNum     页码
     * @param pageSize    每页显示条数
     * @return
     */
    @RequestMapping(value = "/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean skuBatchImportByApi(@RequestParam(value = "beginTime", required = false) String beginTime1,
                                       @RequestParam(value = "endTime", required = false) String endTime1,
                                       @RequestParam(value = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                       @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize) {
        //// TODO: 2018/4/16 测试先默认
        String beginTime = "2018-04-04 10:00:00";
        String endTime = "2018-04-09 10:00:00";
        Response<Object> resReturn = toolMallAPIRequest.getSkuList(beginTime, endTime, pageNum, pageSize, ToolMallItemImportType.ItemSyncType.ALL.value());
        if (!resReturn.isSuccess()) {
            log.error("batch import tool mall category by api failed, beginTime:{}, endTime:{}, cause:{}",
                    beginTime, endTime, resReturn.getError());
            throw new JsonResponseException(resReturn.getError());
        }

        ToolMallItemSyncLog toolMallItemSyncLog = new ToolMallItemSyncLog();
        toolMallItemSyncLog.setBeginTime(beginTime);
        toolMallItemSyncLog.setEndTime(endTime);
        toolMallItemSyncLog.setPageNum(pageNum);
        toolMallItemSyncLog.setPageSize(pageSize);
        toolMallItemSyncLog.setSyncType(ToolMallItemImportType.ItemSyncType.ALL.value());


        toolMallItemSyncLog.setType(ToolMallItemImportType.ITEM_ALL.value());
        ToolMallItemAllReturn result = (ToolMallItemAllReturn) resReturn.getResult();

        if (Arguments.isNull(result) ||
                Arguments.isNull(result.getData()) ||
                Arguments.isNullOrEmpty(result.getData().getList())) {
            toolMallItemSyncLog.setErrorResult("无返回数据");
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE;
        }

        if (!Objects.equals(result.getCode(), 0)) {
            // 不成功标志
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            toolMallItemSyncLog.setErrorResult(result.getMessage());
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE;
        }

        Boolean isSuccess = itemAllBatchImport(result.getData().getList());


        if (!isSuccess) {
            toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.FAIL.value());
            toolMallItemSyncLog.setErrorResult("集乘网内部错误,具体需看日志");
            syncLogCreate(toolMallItemSyncLog);
            return Boolean.FALSE;
        }
        toolMallItemSyncLog.setStatus(ToolMallItemImportStatus.SUCCESS.value());
        updateToolMallSyncNextBeginTimeByType(result.getNextBeginTime(), ToolMallItemImportType.ITEM_ALL.value());
        syncLogCreate(toolMallItemSyncLog);

        return Boolean.TRUE;
    }

    /**
     * 类目导入
     * @param datas 导入数据
     * @return 是否成功
     */
    private Boolean categoryBatchImport(List<ToolMallCategoryReturn.CategoryReturnDate> datas) {
        List<CategoryCreateApiDto> toolMallFirstLevelCategory = Lists.newArrayList();
        List<CategoryCreateApiDto> toolMallSecondLevelCategory = Lists.newArrayList();
        List<CategoryCreateApiDto> toolMallThreeLevelCategory = Lists.newArrayList();
        List<CategoryCreateApiDto> toolMallFourLevelCategory = Lists.newArrayList();

        for(ToolMallCategoryReturn.CategoryReturnDate firstData :datas) {
            //一级类目
            CategoryCreateApiDto firstCategory = new CategoryCreateApiDto();
            firstCategory.setCode(firstData.getId().toString());
            firstCategory.setName(firstData.getName());
            firstCategory.setParentCode("");

            toolMallFirstLevelCategory.add(firstCategory);

            if (!Arguments.isNullOrEmpty(firstData.getChildren())) {
                for (ToolMallCategoryReturn.CategoryReturnDate secondData : firstData.getChildren()) {
                    // 二级类目
                    CategoryCreateApiDto secondCategory = new CategoryCreateApiDto();
                    secondCategory.setCode(secondData.getId().toString());
                    secondCategory.setName(secondData.getName());
                    secondCategory.setParentCode(firstData.getId().toString());

                    toolMallSecondLevelCategory.add(secondCategory);

                    if (!Arguments.isNullOrEmpty(secondData.getChildren())) {
                        // 三级类目
                        for (ToolMallCategoryReturn.CategoryReturnDate threeData : secondData.getChildren()) {
                            CategoryCreateApiDto threeCategory = new CategoryCreateApiDto();
                            threeCategory.setCode(threeData.getId().toString());
                            threeCategory.setName(threeData.getName());
                            threeCategory.setParentCode(secondData.getId().toString());

                            toolMallThreeLevelCategory.add(threeCategory);

                            if (!Arguments.isNullOrEmpty(threeData.getChildren())) {
                                for (ToolMallCategoryReturn.CategoryReturnDate fourData : threeData.getChildren()) {
                                    // 四级类目
                                    CategoryCreateApiDto fourCategory = new CategoryCreateApiDto();
                                    fourCategory.setCode(fourData.getId().toString());
                                    fourCategory.setName(fourData.getName());
                                    fourCategory.setParentCode(threeData.getId().toString());

                                    toolMallFourLevelCategory.add(fourCategory);
                                }
                            }
                        }
                    }
                }
            }

        }

        SubmittedHeader submittedHeader = getToolMallHeader();
        if (!Arguments.isNullOrEmpty(toolMallFirstLevelCategory)) {
            SubmittedCategoryImportDto firstLevelSubmitted = new SubmittedCategoryImportDto();
            firstLevelSubmitted.setHeader(submittedHeader);
            firstLevelSubmitted.setBody(toolMallFirstLevelCategory);
            ApiReturnStatus firstResult = vegaItemImport.categoryAdd(firstLevelSubmitted, null);

            if (!Arguments.isNullOrEmpty(toolMallSecondLevelCategory) && firstResult.getStatus() == 1) {
                SubmittedCategoryImportDto secondLevelSubmitted = new SubmittedCategoryImportDto();
                secondLevelSubmitted.setHeader(submittedHeader);
                secondLevelSubmitted.setBody(toolMallSecondLevelCategory);
                ApiReturnStatus secondResult = vegaItemImport.categoryAdd(secondLevelSubmitted, null);

                if (!Arguments.isNullOrEmpty(toolMallThreeLevelCategory) && secondResult.getStatus() == 1) {
                    SubmittedCategoryImportDto threeLevelSubmitted = new SubmittedCategoryImportDto();
                    threeLevelSubmitted.setHeader(submittedHeader);
                    threeLevelSubmitted.setBody(toolMallThreeLevelCategory);
                    ApiReturnStatus threeResult = vegaItemImport.categoryAdd(threeLevelSubmitted, null);

                    if (!Arguments.isNullOrEmpty(toolMallFourLevelCategory) && threeResult.getStatus() == 1) {
                        SubmittedCategoryImportDto fourLevelSubmitted = new SubmittedCategoryImportDto();
                        fourLevelSubmitted.setHeader(submittedHeader);
                        fourLevelSubmitted.setBody(toolMallFourLevelCategory);
                        ApiReturnStatus fourResult = vegaItemImport.categoryAdd(fourLevelSubmitted, null);

                        return fourResult.getStatus() == 1 ? Boolean.TRUE : Boolean.FALSE;
                    }

                    return threeResult.getStatus() == 1 ? Boolean.TRUE : Boolean.FALSE;
                }

                return secondResult.getStatus() == 1 ? Boolean.TRUE : Boolean.FALSE;
            }
            return firstResult.getStatus() == 1 ? Boolean.TRUE : Boolean.FALSE;
        }

        return Boolean.FALSE;
    }

    /**
     * 商品导入
     * @param allListDate 导入数据
     * @return 是否成功
     */
    private Boolean itemAllBatchImport(List<ToolMallItemAllReturn.AllListDate> allListDate) {
        List<ItemCreateApiDto> toolMallItems = Lists.newArrayList();

        for(ToolMallItemAllReturn.AllListDate data :allListDate) {
            ItemCreateApiDto toolMallItem = new ItemCreateApiDto();


            toolMallItem.setCategoryId(data.getCategoryId().toString());
            toolMallItem.setName(Arguments.isNull(data.getSpuName()) ? data.getFullName() : data.getSpuName());
            toolMallItem.setBrandName(data.getBrandName());
            toolMallItem.setItemOutId(data.getSpuId());
            toolMallItem.setUnitMeasure(data.getUnit());
            toolMallItem.setItemOutUrl(data.getLink());
            toolMallItem.setNormalAttrs(data.getParamAttrs());

            List<ToolMallItemAllReturn.SkuImages> images = data.getSkuImages();
            if (!Arguments.isNullOrEmpty(images)) {
                List<String> imagesArray = Lists.newArrayList();
                List<Map<String, String>> imagesList = Lists.newArrayList();
                String itemDetail = "";
                for (int i = 0; i < images.size(); i++) {
                    Map<String, String> imagesMap = Maps.newHashMap();
                    imagesMap.put("url", images.get(i).getSource());
                    imagesList.add(imagesMap);

                    String imageUrl = "http:" + images.get(i).getSource().replaceAll("http:", "").replaceAll("https:", "");
                    itemDetail += "<img src=\"" + imageUrl + "\">";

                    imagesArray.add(images.get(i).getSource());
                }
                String[] itemImages = new String[imagesArray.size()];
                imagesArray.toArray(itemImages);
                toolMallItem.setImagesArray(itemImages);
                toolMallItem.setItemDetail(itemDetail); //商品详情,从商品主图中获取
            }

            List<SkuCreateApiDto> skuCreateApiDtos = Lists.newArrayList();
            SkuCreateApiDto toolMallSku = new SkuCreateApiDto();
            toolMallSku.setSkuOutId(data.getSkuCode());
            toolMallSku.setItemPrice(NumberUtils.formatPrice(data.getAgreePrice())); //分,集乘网供货价,土猫网协议价
            toolMallSku.setSellAttrs(data.getSellAttrs());
            toolMallSku.setStockQuantity(Arguments.isNull(data.getStock()) ? "0" : data.getStock().toString());
            toolMallSku.setToolMallIsMarketable(data.getIsMarketable()); // 土猫网上下架标志
            skuCreateApiDtos.add(toolMallSku);

            toolMallItem.setChildren(skuCreateApiDtos);
            toolMallItems.add(toolMallItem);
        }

        SubmittedHeader submittedHeader = getToolMallHeader();
        if (!Arguments.isNullOrEmpty(toolMallItems)) {
            SubmittedItemImportDto submittedItemImportDto = new SubmittedItemImportDto();
            submittedItemImportDto.setHeader(submittedHeader);
            submittedItemImportDto.setBody(toolMallItems);
            ApiReturnStatus result = vegaItemImport.itemsAdd(submittedItemImportDto, null, VegaItemImportType.TOOL_MALL_API.value());


            return result.getStatus() == 1 ? Boolean.TRUE : Boolean.FALSE;
        }

        return Boolean.FALSE;
    }


    /**
     * 土猫网数据同步日志
     * @param toolMallItemSyncLog toolMallItemSyncLog
     */
    private void syncLogCreate(ToolMallItemSyncLog toolMallItemSyncLog) {
        Response<Boolean> resCreate = toolMallItemSyncLogWriteService.create(toolMallItemSyncLog);
        if (!resCreate.isSuccess()) {
            log.error("import tool mall category failed, cause:{}", resCreate.getError());
        }
    }

    /**
     * 修改下次获取数据开始时间
     * @param nextBeginTime 下次开始时间
     * @param type 类型
     */
    private void updateToolMallSyncNextBeginTimeByType(String nextBeginTime, Integer type) {
        ToolMallItemSync toolMallItemSync = new ToolMallItemSync();
        toolMallItemSync.setNextBeginTime(nextBeginTime);
        toolMallItemSync.setType(type);
        Response<Boolean> resUpdate = toolMallItemSyncWriteService.updateByType(toolMallItemSync);
        if (!resUpdate.isSuccess()) {
            log.error("update tool mall sync nextBeginTime failed, nextBeginTime:{}, type:{},  cause:{}",
                    nextBeginTime, type, resUpdate.getError());
        }
    }

    /**
     * 获取土猫网在集乘网的认证Id
     * @return 返回值
     */
    private SubmittedHeader getToolMallHeader() {
        SubmittedHeader submittedHeader = new SubmittedHeader();

        submittedHeader.setClientId("toolmao");
        submittedHeader.setClientSecret("123456");

        return submittedHeader;
    }


}
