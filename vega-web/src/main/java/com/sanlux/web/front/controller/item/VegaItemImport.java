package com.sanlux.web.front.controller.item;


import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sanlux.category.service.VegaCategoryReadService;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.item.dto.api.*;
import com.sanlux.item.model.ItemImport;
import com.sanlux.item.service.ItemImportReadService;
import com.sanlux.item.service.ItemImportWriteService;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.service.VegaShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryWriteService;
import io.terminus.parana.delivery.dto.DeliveryFeeTemplateDetail;
import io.terminus.parana.delivery.service.DeliveryFeeReadService;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;


/**
 * 集乘网商品同步接口
 * Created by lujm on 2018/3/16.
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/item")
public class VegaItemImport {

    @RpcConsumer
    private ItemImportWriteService itemImportWriteService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private DeliveryFeeReadService deliveryFeeReadService;

    @RpcConsumer
    private ItemImportReadService itemImportReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private VegaCategoryReadService vegaCategoryReadService;

    @RpcConsumer
    private BackCategoryWriteService backCategoryWriteService;


    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiReturnStatus itemsAdd(@Valid @RequestBody SubmittedItemImportDto submittedItemImportDto,
                                    BindingResult bindingResult,
                                    @RequestParam(value = "type", required = false) Integer type) {
        ApiReturnStatus apiReturnStatus = new ApiReturnStatus();
        apiReturnStatus.setStatus(0);

        try {
            if (Arguments.notNull(bindingResult) && bindingResult.hasErrors()) {
                log.error("fail to import item by api sync, submittedItemImportDto:{},cause:{}",
                        submittedItemImportDto, bindingResult.getFieldError().getDefaultMessage());
                apiReturnStatus.setErrorMsg("必填项验证失败!,错误信息:" + bindingResult.getFieldError().getDefaultMessage() + ",同步失败");
                return apiReturnStatus;
            }

            Shop shop = jcforVerification(submittedItemImportDto.getHeader().getClientId(), submittedItemImportDto.getHeader().getClientSecret());
            if (Arguments.isNull(shop)) {
                apiReturnStatus.setErrorMsg("安全认证失败!同步失败");
                return apiReturnStatus;
            }
            
            //// TODO: 2018/3/28 数据量大小限制.......... 

            Response<Optional<DeliveryFeeTemplateDetail>> deliveryFeeRes = deliveryFeeReadService.findDefaultDeliveryFeeTemplateByShopId(shop.getId());
            if (!deliveryFeeRes.isSuccess() || !deliveryFeeRes.getResult().isPresent()) {
                log.error("fail to find default delivery fee template by shop id:{},cause:{}", shop.getId(), deliveryFeeRes.getError());
                apiReturnStatus.setErrorMsg("集乘网内部错误!错误信息:获取运费模板错误,同步失败");
                return apiReturnStatus;
            }

            Map<String, String> tagMap = shop.getTags();
            String outIdTag = "";

            if (!Arguments.isNull(tagMap)) {
                outIdTag = tagMap.get(SystemConstant.ITEM_IMPORT_API_OUT_ID_TAG);
            }

            Response<Long> uploadRes = itemImportWriteService.importItemsByApi(shop.getId(), outIdTag ,
                    deliveryFeeRes.getResult().get().getDeliveryFeeTemplate().getId(), submittedItemImportDto, type);
            if (!uploadRes.isSuccess() || Arguments.isNull(uploadRes.getResult())) {
                log.error("import item by api sync failed, shopId={}, cause:{}", shop.getId(), uploadRes.getError());
                apiReturnStatus.setErrorMsg("集乘网内部错误!错误信息:" + uploadRes.getError()+",同步失败");
                return apiReturnStatus;
            }

            int index = 0;

            while (index <= 10) {
                ItemImport itemImport = findItemImportById(uploadRes.getResult());
                if (Arguments.isNull(itemImport) || Objects.equals(itemImport.getStatus(), ItemImport.Status.INIT.value())) {
                    // 后台还在执行中,没有结果
                    index ++;
                    // 等待2秒,继续查询,最多等待20秒
                    Thread.sleep(2000);
                    continue;
                }

                if (Objects.equals(itemImport.getStatus(), ItemImport.Status.SUCCESS.value())) {
                    // 导入成功
                    apiReturnStatus.setStatus(1);
                    return apiReturnStatus;
                }

                if (Objects.equals(itemImport.getStatus(), ItemImport.Status.FAIL.value())) {
                    // 导入失败
                    apiReturnStatus.setErrorMsg("集乘网内部错误!错误信息:" + itemImport.getErrorResult()+",同步失败");
                    return apiReturnStatus;
                }
            }

            apiReturnStatus.setErrorMsg("集乘网数据导入还在等待中,稍后请和管理员确认同步结果.");
            return apiReturnStatus;
        } catch (Exception e) {
            log.error("fail to import item by api sync, submittedItemImportDto:{},cause:{}", submittedItemImportDto, Throwables.getStackTraceAsString(e));
            apiReturnStatus.setErrorMsg("集乘网内部错误!同步失败");
            return apiReturnStatus;
        }

    }


    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiReturnStatus itemsUpdate(@RequestBody SubmittedItemImportDto submittedItemImportDto) {
        ApiReturnStatus apiReturnStatus = new ApiReturnStatus();
        apiReturnStatus.setStatus(0);

        try {
            Shop shop = jcforVerification(submittedItemImportDto.getHeader().getClientId(), submittedItemImportDto.getHeader().getClientSecret());
            if (Arguments.isNull(shop)) {
                apiReturnStatus.setErrorMsg("安全认证失败!同步失败");
                return apiReturnStatus;
            }

            //// TODO: 2018/3/28 数据量大小限制..........

            Map<String, String> tagMap = shop.getTags();
            String outIdTag = "";

            if (!Arguments.isNull(tagMap)) {
                outIdTag = tagMap.get(SystemConstant.ITEM_IMPORT_API_OUT_ID_TAG);
            }

            Response<Boolean> updateRes = itemImportWriteService.updateItemsByApi(shop.getId(), outIdTag, submittedItemImportDto);
            if (!updateRes.isSuccess() || Arguments.isNull(updateRes.getResult())) {
                log.error("import item by api update failed, shopId={}, cause:{}", shop.getId(), updateRes.getError());
                apiReturnStatus.setErrorMsg("集乘网内部错误!错误信息:" + updateRes.getError()+",同步失败");
                return apiReturnStatus;
            }

            apiReturnStatus.setStatus(1);
            return apiReturnStatus;
        } catch (Exception e) {
            log.error("fail to import update by api sync, submittedItemImportDto:{},cause:{}", submittedItemImportDto, Throwables.getStackTraceAsString(e));
            apiReturnStatus.setErrorMsg("集乘网内部错误!同步失败");
            return apiReturnStatus;
        }

    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiReturnStatus itemsDelete(@RequestBody SubmittedItemDeleteDto submittedItemDeleteDto) {
        ApiReturnStatus apiReturnStatus = new ApiReturnStatus();
        apiReturnStatus.setStatus(0);

        try {
            Shop shop = jcforVerification(submittedItemDeleteDto.getHeader().getClientId(), submittedItemDeleteDto.getHeader().getClientSecret());
            if (Arguments.isNull(shop)) {
                apiReturnStatus.setErrorMsg("安全认证失败!同步失败");
                return apiReturnStatus;
            }

            //// TODO: 2018/3/28 数据量大小限制..........

            Map<String, String> tagMap = shop.getTags();
            String outIdTag = "";

            if (!Arguments.isNull(tagMap)) {
                outIdTag = tagMap.get(SystemConstant.ITEM_IMPORT_API_OUT_ID_TAG);
            }

            List<String> skuOutIds = submittedItemDeleteDto.getBody();
            if (Arguments.isNullOrEmpty(skuOutIds)) {
                apiReturnStatus.setErrorMsg("skuOutId不能为空!同步失败");
                return apiReturnStatus;
            }

            List<Long> deleteSkuList = Lists.newArrayList();
            List<Long> deleteAllItemList = Lists.newArrayList();
            for (String s : skuOutIds) {
                Response<List<Sku>> skuRes =skuReadService.findSkuByCode(shop.getId(), outIdTag.concat(s));

                if (Arguments.isNullOrEmpty(skuRes.getResult())) {
                    log.error("sku not exist, shopId:{}, skuCode:{}", shop.getId(), outIdTag.concat(s));

                    apiReturnStatus.setErrorMsg("skuOutId={"+s+"}商品集乘网未找到或已经删除!同步失败");
                    return apiReturnStatus;
                }

                deleteSkuList.add(skuRes.getResult().get(0).getId());
                deleteAllItemList.add(skuRes.getResult().get(0).getItemId());
            }

            // 去重
            Set<Long> deleteItemSet = Sets.newHashSet();
            deleteItemSet.addAll(deleteAllItemList);

            List<Long> deleteItemList = Lists.newArrayList();
            deleteItemList.addAll(deleteItemSet);

            for (Long id : deleteItemSet) {
                Response<List<Sku>> skuRes =skuReadService.findSkusByItemId(id);
                if (skuRes.isSuccess()) {
                    List<Long> skuIds = Lists.transform(skuRes.getResult(), Sku::getId);
                    if (!deleteSkuList.containsAll(skuIds)) {
                        // 未删除所有SKU的商品信息时,商品信息不删除
                        deleteItemList.remove(id);
                    }
                }
            }


            Response<Boolean> deleteRes = itemImportWriteService.deleteItemsByApi(shop.getId(), deleteItemList, deleteSkuList);
            if (!deleteRes.isSuccess() || !deleteRes.getResult()) {
                log.error("import item by api delete failed, shopId={} , itemIds = {}, skuIds = {}, cause:{}", shop.getId(), deleteItemList, deleteSkuList, deleteRes.getError());
                apiReturnStatus.setErrorMsg("集乘网内部错误!错误信息:" + deleteRes.getError()+",同步失败");
                return apiReturnStatus;
            }

            apiReturnStatus.setStatus(1);
            return apiReturnStatus;
        } catch (Exception e) {
            log.error("fail to import delete by api sync, submittedItemDeleteDto:{},cause:{}", submittedItemDeleteDto, Throwables.getStackTraceAsString(e));
            apiReturnStatus.setErrorMsg("集乘网内部错误!同步失败");
            return apiReturnStatus;
        }
    }

    @RequestMapping(value = "/category/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiReturnStatus categoryAdd(@Valid @RequestBody SubmittedCategoryImportDto submittedCategoryImportDto, BindingResult bindingResult) {
        ApiReturnStatus apiReturnStatus = new ApiReturnStatus();
        apiReturnStatus.setStatus(0);

        try {
            if (Arguments.notNull(bindingResult) && bindingResult.hasErrors()) {
                log.error("fail to import category by api sync, submittedCategoryImportDto:{},cause:{}",
                        submittedCategoryImportDto, bindingResult.getFieldError().getDefaultMessage());
                apiReturnStatus.setErrorMsg("必填项验证失败!,错误信息:" + bindingResult.getFieldError().getDefaultMessage() + ",同步失败");
                return apiReturnStatus;
            }

            Shop shop = jcforVerification(submittedCategoryImportDto.getHeader().getClientId(), submittedCategoryImportDto.getHeader().getClientSecret());
            if (Arguments.isNull(shop)) {
                apiReturnStatus.setErrorMsg("安全认证失败!同步失败");
                return apiReturnStatus;
            }

            Map<String, String> tagMap = shop.getTags();
            Long categoryPid = 0L;

            if (!Arguments.isNull(tagMap)) {
                if (Arguments.isNull(tagMap.get(SystemConstant.ITEM_IMPORT_API_CATEGORY_PID_TAG))) {
                    apiReturnStatus.setErrorMsg("集乘网类目根节点还未初始化!同步失败");
                    return apiReturnStatus;
                }
                categoryPid = Long.valueOf(tagMap.get(SystemConstant.ITEM_IMPORT_API_CATEGORY_PID_TAG));
            }

            List<String> errorCategoryCodes = Lists.newArrayList();
            List<CategoryCreateApiDto> categoryCreatesByApis = submittedCategoryImportDto.getBody();

            Map<String, CategoryCreateApiDto> hashMap = Maps.newHashMap();
            for (CategoryCreateApiDto  categoryCreateApiDto : categoryCreatesByApis) {
                if (hashMap.get(categoryCreateApiDto.getCode()) != null) {
                    apiReturnStatus.setErrorMsg("类目编号"+categoryCreateApiDto.getCode()+"重复!同步失败");
                    return apiReturnStatus;
                }
                hashMap.put(categoryCreateApiDto.getCode(), categoryCreateApiDto);
            }

            //按照上级id从小到大排序
            Collections.sort(categoryCreatesByApis, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    CategoryCreateApiDto r0=(CategoryCreateApiDto)o1;
                    CategoryCreateApiDto r1=(CategoryCreateApiDto)o2;
                    if (Strings.isNullOrEmpty(r0.getParentCode())) {
                        r0.setParentCode("0");
                    }
                    if (Strings.isNullOrEmpty(r1.getParentCode())) {
                        r1.setParentCode("0");
                    }
                    return r0.getParentCode().compareTo(r1.getParentCode());
                }
            });


            for (CategoryCreateApiDto categoryCreateApiDto : categoryCreatesByApis) {
                BackCategory toCreate = new BackCategory();

                Response<BackCategory> backResp = vegaCategoryReadService.findBackCategoryByOutId(categoryCreateApiDto.getCode());
                if (backResp.isSuccess() && Arguments.notNull(backResp.getResult()) ) {
                    log.warn("failed to create because out id code:{} is repeat ", categoryCreateApiDto.getCode());
                    errorCategoryCodes.add(categoryCreateApiDto.getCode());
                    continue;
                }

                toCreate.setOuterId(categoryCreateApiDto.getCode());
                toCreate.setName(categoryCreateApiDto.getName());

                toCreate.setPid(categoryPid); // 默认值
                if (!Strings.isNullOrEmpty(categoryCreateApiDto.getParentCode())) {

                    Response<BackCategory> backCategoryRes = vegaCategoryReadService.findBackCategoryByOutId(categoryCreateApiDto.getParentCode());
                    if (backCategoryRes.isSuccess() && Arguments.notNull(backCategoryRes.getResult()) ) {
                        if (backCategoryRes.getResult().getLevel() < 4) {
                            toCreate.setPid(backCategoryRes.getResult().getId());
                        } else {
                            toCreate.setPid(backCategoryRes.getResult().getPid());
                        }
                    }
                }

                Response<BackCategory> backCategoryResponse = backCategoryWriteService.create(toCreate);
                if(!backCategoryResponse.isSuccess()) {
                    log.warn("failed to create {}, error code:{}", toCreate, backCategoryResponse.getError());
                    errorCategoryCodes.add(categoryCreateApiDto.getCode());
                }
            }


            if (!Arguments.isNullOrEmpty(errorCategoryCodes)) {
                if (errorCategoryCodes.size() == categoryCreatesByApis.size()) {
                    log.error("import category by api sync failed, shopId={}, data={}", shop.getId(), submittedCategoryImportDto);
                    apiReturnStatus.setErrorMsg("类目已经存在,同步失败");
                    return apiReturnStatus;
                }

                apiReturnStatus.setStatus(1); //部分成功状态也设置为成功
                apiReturnStatus.setErrorMsg("部分类目信息同步失败,错误类目编号为:" + Joiner.on(",").join(errorCategoryCodes));
                return apiReturnStatus;
            }

            apiReturnStatus.setStatus(1);
            return apiReturnStatus;
        } catch (Exception e) {
            log.error("fail to import category by api sync, submittedCategoryImportDto:{},cause:{}", submittedCategoryImportDto, Throwables.getStackTraceAsString(e));
            apiReturnStatus.setErrorMsg("集乘网内部错误!同步失败");
            return apiReturnStatus;
        }

    }


    /**
     * 集乘网安全验证,成功返回供应商信息
     * @param clientId     集乘网认证Id
     * @param clientSecret 集乘网认证密码
     * @return 供应商信息
     */
    private Shop jcforVerification(String clientId, String clientSecret) {
        VegaShopCriteria criteria = new VegaShopCriteria();
        criteria.setShopStatus(VegaShopStatus.NORMAL.value());// 正常状态
        criteria.setType(VegaShopType.SUPPLIER.value()); //供应商
        criteria.setPageNo(0);
        criteria.setPageSize(1000); // 供应商数量
        Response<Paging<VegaShop>> resp = vegaShopReadService.paging(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging vegaShop by criteria : ({}), cause : {}", criteria, resp.getError());
            return null;
        }
        List<VegaShop> shops = resp.getResult().getData();

        for (VegaShop vegaShop : shops) {
            Shop shop = vegaShop.getShop();
            Map<String, String> tagMap = shop.getTags();

            if (!Arguments.isNull(tagMap)) {
                String shopClientId = tagMap.get(SystemConstant.ITEM_IMPORT_API_CLIENT_ID);
                String shopClientSecret = tagMap.get(SystemConstant.ITEM_IMPORT_API_CLIENT_SECRET);
                if (Objects.equals(clientId, shopClientId) && Objects.equals(clientSecret, shopClientSecret)) {
                    return shop;
                }
            }
        }
        return null;
    }

    /**
     * 根据任务id获取导入日志
     * @param id 任务Id
     * @return 导入日志
     */
    private ItemImport findItemImportById (Long id) {
        Response<ItemImport> itemImportResponse = itemImportReadService.findById(id);
        if (!itemImportResponse.isSuccess()) {
            log.error("failed find item import by id : {}, cause : {}", id, itemImportResponse.getError());
            return null;
        }
        return itemImportResponse.getResult();
    }

}
