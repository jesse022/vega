package com.sanlux.web.front.core.youyuncai.request;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.category.service.VegaCategoryReadService;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.VegaSkuReadService;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.*;
import com.sanlux.web.front.core.util.ArithUtil;
import com.sanlux.web.front.core.youyuncai.order.constants.YouyuncaiConstants;
import com.sanlux.web.front.core.youyuncai.order.dto.*;
import com.sanlux.youyuncai.dto.CategoryDto;
import com.sanlux.youyuncai.dto.SkuAttributeDto;
import com.sanlux.youyuncai.dto.SkuDto;
import com.sanlux.youyuncai.dto.YouyuncaiReturnStatus;
import com.sanlux.youyuncai.enums.YouyuncaiApiType;
import com.sanlux.youyuncai.service.VegaItemSyncReadService;
import com.sanlux.youyuncai.service.VegaItemSyncWriteService;
import com.sanlux.youyuncai.model.VegaItemSync;
import com.sanlux.web.front.core.youyuncai.token.YouyuncaiToken;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.dto.ImageInfo;
import io.terminus.parana.item.dto.ViewedItemDetailInfo;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 友云采对接处理类
 * Created by lujm on 2018/2/6.
 */
@Component
@Slf4j
public class VegaYouyuncaiComponent {

    private final static JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final Integer BATCH_SIZE = 50;     // 每批处理数量
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @RpcConsumer
    private VegaCategoryReadService vegaCategoryReadService;

    @RpcConsumer
    private VegaSkuReadService vegaSkuReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private VegaItemSyncWriteService vegaItemSyncWriteService;

    @RpcConsumer
    private VegaItemSyncReadService vegaItemSyncReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;

    @RpcConsumer
    private PurchaseOrderWriteService purchaseOrderWriteService;

    @RpcConsumer
    private PurchaseSkuOrderWriteService purchaseSkuOrderWriteService;

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @RpcConsumer
    private YouyuncaiOrderReadService youyuncaiOrderReadService;

    @Value("${web.domain}")
    private  String webDomain ;


    /**
     * 出货通知接口
     * @param orderId                订单号
     * @param shipmentCompanyName    物流公司名称
     * @param shipmentSerialNo       物流单号
     * @return
     */
    public Boolean shipInfo(Long orderId, String shipmentCompanyName, String shipmentSerialNo){

        YouyuncaiShipInfoDto youyuncaiShipInfoDto = new YouyuncaiShipInfoDto();

        VegaOrderDetail vegaOrderDetail = findOrderDetailByOrderId(orderId);
        YouyuncaiOrder youyuncaiOrder = findYouyuncaiOrderDetailByOrderId(orderId);
        if (Arguments.isNull(vegaOrderDetail) || Arguments.isNull(youyuncaiOrder)) {
            log.error("fail to find order detail vegaOrderDetail = {}, youyuncaiOrder={}");
            return Boolean.FALSE;
        }

        List<YouyuncaiOrderDetailDto> youyuncaiOrderDetailDtos =
                JsonMapper.nonDefaultMapper().fromJson(youyuncaiOrder.getOrderDetailJson(), JSON_MAPPER.createCollectionType(List.class, YouyuncaiOrderDetailDto.class));
        List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();

        if (Arguments.isNullOrEmpty(youyuncaiOrderDetailDtos) || Arguments.isNullOrEmpty(skuOrders)) {
            log.error("find order detail is null or empty youyuncaiOrderDetailDtos = {}, skuOrders={}");
            return Boolean.FALSE;
        }

        Map<String, YouyuncaiOrderDetailDto> youyuncaiOrderDetailDtoMap = Maps.uniqueIndex(youyuncaiOrderDetailDtos, YouyuncaiOrderDetailDto::getSkuCode);
        List<YouyuncaiShipInfoOrderDetailDto> shipInfoDetailDtos = Lists.newArrayList();

        int index = 1;
        for (SkuOrder skuOrder : skuOrders) {
            YouyuncaiShipInfoOrderDetailDto youyuncaiShipInfoOrderDetailDto = new YouyuncaiShipInfoOrderDetailDto();

            youyuncaiShipInfoOrderDetailDto.setLineNumber(youyuncaiOrderDetailDtoMap.get(skuOrder.getSkuId().toString()).getLineNumber());
            youyuncaiShipInfoOrderDetailDto.setSupplierlineNumber(String.valueOf(index));
            youyuncaiShipInfoOrderDetailDto.setShipSheduleDate(DFT.print(DateTime.now())); // 出货日期
            youyuncaiShipInfoOrderDetailDto.setQuantity(skuOrder.getQuantity().toString());
            youyuncaiShipInfoOrderDetailDto.setWayBillNumber(shipmentSerialNo);
            youyuncaiShipInfoOrderDetailDto.setCarrierIdentifier(shipmentCompanyName);

            index ++;
            shipInfoDetailDtos.add(youyuncaiShipInfoOrderDetailDto);
        }


        youyuncaiShipInfoDto.setOrderCode(youyuncaiOrder.getOrderCode());
        youyuncaiShipInfoDto.setSupplierOrderID(youyuncaiOrder.getOrderId().toString());
        youyuncaiShipInfoDto.setOrderDetail(shipInfoDetailDtos);



        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.build(youyuncaiToken);
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(2);
        YouyuncaiHeaderDto youyuncaiHeaderDto = new YouyuncaiHeaderDto();
        youyuncaiHeaderDto.setClientId(youyuncaiToken.getClientId());
        youyuncaiHeaderDto.setClientSecret(youyuncaiToken.getClientSecret());
        params.put(YouyuncaiConstants.HEADER,objectToJson(youyuncaiHeaderDto));
        params.put(YouyuncaiConstants.BODY,objectToJson(youyuncaiShipInfoDto));


        log.info("[YOU-YUN-CAI]:delivery order begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        YouyuncaiReturnStatus youyuncaiReturnStatus = youyuncaiRequest.youyuncaiApi(params, YouyuncaiApiType.ORDER_SHIP_INFO.value());

        stopwatch.stop();
        log.info("[YOU-YUN-CAI]:delivery order done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return youyuncaiOrderReturn(youyuncaiReturnStatus, null, YouyuncaiApiType.ORDER_SHIP_INFO.value());
    }

    /**
     * 订单交期确认
     * @return 是否成功
     */
    public Boolean deliveryOrder(Long orderId, Integer type){

        YouyuncaiDeliveryOrderDto youyuncaiDeliveryOrderDto = new YouyuncaiDeliveryOrderDto();

        VegaOrderDetail vegaOrderDetail = findOrderDetailByOrderId(orderId);
        YouyuncaiOrder youyuncaiOrder = findYouyuncaiOrderDetailByOrderId(orderId);
        if (Arguments.isNull(vegaOrderDetail) || Arguments.isNull(youyuncaiOrder)) {
            log.error("fail to find order detail vegaOrderDetail = {}, youyuncaiOrder={}");
            return Boolean.FALSE;
        }

        List<YouyuncaiOrderDetailDto> youyuncaiOrderDetailDtos =
                JsonMapper.nonDefaultMapper().fromJson(youyuncaiOrder.getOrderDetailJson(), JSON_MAPPER.createCollectionType(List.class, YouyuncaiOrderDetailDto.class));
        List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();

        if (Arguments.isNullOrEmpty(youyuncaiOrderDetailDtos) || Arguments.isNullOrEmpty(skuOrders)) {
            log.error("find order detail is null or empty youyuncaiOrderDetailDtos = {}, skuOrders={}");
            return Boolean.FALSE;
        }

        Map<String, YouyuncaiOrderDetailDto> youyuncaiOrderDetailDtoMap = Maps.uniqueIndex(youyuncaiOrderDetailDtos, YouyuncaiOrderDetailDto::getSkuCode);
        List<YouyuncaiDeliveryOrderDetailDto> deliveryOrderDetailDtos = Lists.newArrayList();

        int index = 1;
        for (SkuOrder skuOrder : skuOrders) {
            YouyuncaiDeliveryOrderDetailDto youyuncaiDeliveryOrderDetailDto = new YouyuncaiDeliveryOrderDetailDto();

            youyuncaiDeliveryOrderDetailDto.setLineNumber(youyuncaiOrderDetailDtoMap.get(skuOrder.getSkuId().toString()).getLineNumber());
            youyuncaiDeliveryOrderDetailDto.setSupplierlineNumber(String.valueOf(index));
            youyuncaiDeliveryOrderDetailDto.setSkuCode(skuOrder.getSkuId().toString());
            youyuncaiDeliveryOrderDetailDto.setShipScheduleDate("7~15天"); //预计发货日期
            youyuncaiDeliveryOrderDetailDto.setQuantity(skuOrder.getQuantity().toString());
            youyuncaiDeliveryOrderDetailDto.setNakedPrice(NumberUtils.formatPrice(ArithUtil.div(skuOrder.getFee(), skuOrder.getQuantity())));
            youyuncaiDeliveryOrderDetailDto.setPrice(NumberUtils.formatPrice(ArithUtil.div(skuOrder.getFee(), skuOrder.getQuantity())));
            youyuncaiDeliveryOrderDetailDto.setTaxPrice(YouyuncaiConstants.taxRate);

            index ++;
            deliveryOrderDetailDtos.add(youyuncaiDeliveryOrderDetailDto);
        }


        youyuncaiDeliveryOrderDto.setOrderCode(youyuncaiOrder.getOrderCode());
        youyuncaiDeliveryOrderDto.setOrderState(type.toString().trim());
        youyuncaiDeliveryOrderDto.setSupplierOrderID(youyuncaiOrder.getOrderId().toString());
        youyuncaiDeliveryOrderDto.setOrderDetail(deliveryOrderDetailDtos);



        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.build(youyuncaiToken);
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(2);
        YouyuncaiHeaderDto youyuncaiHeaderDto = new YouyuncaiHeaderDto();
        youyuncaiHeaderDto.setClientId(youyuncaiToken.getClientId());
        youyuncaiHeaderDto.setClientSecret(youyuncaiToken.getClientSecret());
        params.put(YouyuncaiConstants.HEADER,objectToJson(youyuncaiHeaderDto));
        params.put(YouyuncaiConstants.BODY,objectToJson(youyuncaiDeliveryOrderDto));


        log.info("[YOU-YUN-CAI]:delivery order begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        YouyuncaiReturnStatus youyuncaiReturnStatus = youyuncaiRequest.youyuncaiApi(params, YouyuncaiApiType.ORDER_DELIVERY_ORDER.value());

        stopwatch.stop();
        log.info("[YOU-YUN-CAI]:delivery order done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return youyuncaiOrderReturn(youyuncaiReturnStatus, null, YouyuncaiApiType.ORDER_DELIVERY_ORDER.value());
    }

    /**
     * 商品分类初始化
     * @return 是否成功
     */
    public Boolean categoryInit(){
        int pageNo = 1;
        ParanaUser currentUser = UserUtil.getCurrentUser();
        List<BackCategory> backCategoryList = getNotSyncBackCategoryList(pageNo, BATCH_SIZE, VegaItemSync.Channel.YOU_YUN_CAI.value());
        if (Arguments.isNullOrEmpty(backCategoryList)) {
            log.info("[YOU-YUN-CAI]:find wait sync back category empty by status:{}", Boolean.TRUE);
            throw new JsonResponseException("find.wait.sync.back.category.empty");
        }

        List<CategoryDto> InitCategory = Lists.newArrayList();
        List<VegaItemSync> vegaItemSyncs = Lists.newArrayList();
        backCategoryList.stream().forEach(backCategory -> {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setCode(backCategory.getId().toString());
            categoryDto.setName(backCategory.getName());
            categoryDto.setParentCode(backCategory.getPid().toString());
            InitCategory.add(categoryDto);

            VegaItemSync vegaItemSync = new VegaItemSync();
            vegaItemSync.setChannel(VegaItemSync.Channel.YOU_YUN_CAI.value());
            vegaItemSync.setType(VegaItemSync.Type.CATEGORY.value());
            vegaItemSync.setStatus(VegaItemSync.Status.SUCCESS.value());
            vegaItemSync.setSyncId(backCategory.getId());
            vegaItemSync.setSyncPid(backCategory.getPid());
            vegaItemSync.setSyncName(backCategory.getName());
            vegaItemSync.setUserId(currentUser.getId());
            vegaItemSync.setUserName(currentUser.getName());
            vegaItemSyncs.add(vegaItemSync);
        });


        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.build(youyuncaiToken);
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(1);
        params.put("data",InitCategory);

        log.info("[YOU-YUN-CAI]:category init begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        YouyuncaiReturnStatus youyuncaiReturnStatus = youyuncaiRequest.youyuncaiApi(params, YouyuncaiApiType.CATEGORY_INIT.value());

        stopwatch.stop();
        log.info("[YOU-YUN-CAI]:category init done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return youyuncaiReturn(youyuncaiReturnStatus, vegaItemSyncs, YouyuncaiApiType.CATEGORY_INIT.value());
    }

    /**
     * 商品初始化
     * @return 是否成功
     */
    public Boolean skuInit(){
        int pageNo = 1;
        List<SkuDto> skuDtos = getNotSyncSkuList(pageNo, BATCH_SIZE, VegaItemSync.Channel.YOU_YUN_CAI.value());
        if (Arguments.isNullOrEmpty(skuDtos)) {
            log.info("[YOU-YUN-CAI]:find wait sync sku empty by status:{}", Boolean.TRUE);
            throw new JsonResponseException("find.wait.sync.sku.empty");
        }

        return skuSyncPackage(skuDtos, YouyuncaiApiType.ITEM_INIT.value());
    }

    /**
     * 单个商品更新(新增,修改,删除)(含多个SKU)
     * @param itemId 商品Id
     * @param apiType 接口类型
     * @return 是否成功
     */
    public Boolean skuSyncByItem(Long itemId, Integer apiType){

        List<SkuDto> skuDtos = getSkuListByItem(itemId, VegaItemSync.Channel.YOU_YUN_CAI.value(),
                VegaItemSync.Type.ITEM.value(), apiType);
        if (Arguments.isNullOrEmpty(skuDtos)) {
            log.info("[YOU-YUN-CAI]:find wait sync sku empty");
            throw new JsonResponseException("find.wait.sync.sku.empty");
        }
        return skuSyncPackage(skuDtos, apiType);
    }

    /**
     * 商品批量更新(自动拆分成新增,修改,删除接口)
     * @param itemIds 商品Ids
     * @return 是否成功
     */
    public Boolean skuSyncByItems(List<Long> itemIds){
        Boolean addIsSuccess = Boolean.TRUE;
        Boolean updateIsSuccess = Boolean.TRUE;
        Boolean deleteIsSuccess = Boolean.TRUE;

        Map<String,List<SkuDto>> skuDtosMap = getSkuListForSplitByItems(itemIds, VegaItemSync.Channel.YOU_YUN_CAI.value(), VegaItemSync.Type.ITEM.value());
        if (Arguments.isNull(skuDtosMap)) {
            log.info("[YOU-YUN-CAI]:find wait sync sku empty");
            throw new JsonResponseException("find.wait.sync.sku.empty");
        }

        List<SkuDto> addSkusDtos = skuDtosMap.get(VegaItemSync.SyncType.ADD.name());
        List<SkuDto> updateSkusDtos = skuDtosMap.get(VegaItemSync.SyncType.UPDATE.name());
        List<SkuDto> deleteSkusDtos = skuDtosMap.get(VegaItemSync.SyncType.DELETE.name());

        if (!Arguments.isNullOrEmpty(addSkusDtos)) {
            addIsSuccess = skuSyncPackage(addSkusDtos, YouyuncaiApiType.ITEM_ADD.value());
        }
        if (!Arguments.isNullOrEmpty(updateSkusDtos)) {
            updateIsSuccess = skuSyncPackage(updateSkusDtos, YouyuncaiApiType.ITEM_UPDATE.value());
        }
        if (!Arguments.isNullOrEmpty(deleteSkusDtos)) {
            deleteIsSuccess = skuSyncPackage(deleteSkusDtos, YouyuncaiApiType.ITEM_DELETE.value());
        }

        return addIsSuccess && updateIsSuccess && deleteIsSuccess;
    }

    /**
     * 友云采商品同步封装
     * @param skuDtos  同步数据
     * @param apiType  接口类型
     * @return 是否成功
     */
    private Boolean skuSyncPackage(List<SkuDto> skuDtos, Integer apiType) {
        ParanaUser currentUser = UserUtil.getCurrentUser();
        List<VegaItemSync> vegaItemSyncs = Lists.newArrayList();
        skuDtos.stream().forEach(skuDto -> {
            VegaItemSync vegaItemSync = new VegaItemSync();
            vegaItemSync.setChannel(VegaItemSync.Channel.YOU_YUN_CAI.value());
            vegaItemSync.setType(VegaItemSync.Type.ITEM.value());
            vegaItemSync.setStatus(VegaItemSync.Status.SUCCESS.value());
            vegaItemSync.setSyncId(Long.valueOf(skuDto.getSkuCode()));
            vegaItemSync.setSyncPid(Long.valueOf(skuDto.getMemo()));
            vegaItemSync.setSyncName(skuDto.getName());
            vegaItemSync.setUserId(currentUser.getId());
            vegaItemSync.setUserName(currentUser.getName());
            vegaItemSyncs.add(vegaItemSync);

            skuDto.setMemo(null); //备注清空
        });


        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.build(youyuncaiToken);
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(1);
        params.put("data", skuDtos);

        log.info("[YOU-YUN-CAI]:sku sync begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        YouyuncaiReturnStatus youyuncaiReturnStatus = youyuncaiRequest.youyuncaiApi(params, apiType);

        stopwatch.stop();
        log.info("[YOU-YUN-CAI]:sku sync done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return youyuncaiReturn(youyuncaiReturnStatus, vegaItemSyncs, apiType);
    }

    /**
     * 友云采返回信息解析,成功记录同步日志
     * @param youyuncaiReturnStatus youyuncaiReturnStatus
     * @param vegaItemSyncs vegaItemSyncs
     * @param apiType 接口类型
     * @return 是否成功
     */
    private Boolean youyuncaiReturn (YouyuncaiReturnStatus youyuncaiReturnStatus, List<VegaItemSync> vegaItemSyncs, Integer apiType) {
        String str1 = "";
        String str2 = "";

        switch (YouyuncaiApiType.from(apiType)) {
            case CATEGORY_INIT:
                str1 = "category init";
                str2 = "category.init";
                break;
            case ITEM_INIT:
                str1 = "sku init";
                str2 = "sku.init";
                break;
            case ITEM_UPDATE:
                str1 = "sku update";
                str2 = "sku.update";
                break;
            case ITEM_DELETE:
                str1 = "sku delete";
                str2 = "sku.delete";
                break;
        }

        if (Arguments.isNull(youyuncaiReturnStatus)) {
            log.info("[YOU-YUN-CAI]:post "+str1+" api is success but return is null");
            throw new JsonResponseException(str2+".return.empty");
        }

        if (Arguments.notNull(youyuncaiReturnStatus.getStatus()) && youyuncaiReturnStatus.getStatus() > 0) {
            // 成功
            if (!Objects.equal(apiType, YouyuncaiApiType.ITEM_DELETE.value()) && !Objects.equal(apiType, YouyuncaiApiType.ITEM_UPDATE.value())) {
                // 修改和删除时不更新同步日志,同步出错时直接报错
                Response<Integer> createResp = vegaItemSyncWriteService.creates(vegaItemSyncs);
                if (!createResp.isSuccess()) {
                    log.error("fail to " + str1 + " sync info:{},cause:{}", vegaItemSyncs, createResp.getError());
                    throw new JsonResponseException(str2 + ".success.but.add.sync.log.fail");
                }
            }

            return Boolean.TRUE;
        }
        // 注:同步失败时,不记录日志,保证每次批量初始化的数据都是未同步过的
        throw new JsonResponseException(youyuncaiReturnStatus.getErrormsg());
    }

    /**
     * 友云采订单对接返回信息解析,成功记录同步日志
     * @param youyuncaiReturnStatus youyuncaiReturnStatus
     * @param purchaseOrderId 采购单Id
     * @param apiType 接口类型
     * @return 是否成功
     */
    public Boolean youyuncaiOrderReturn (YouyuncaiReturnStatus youyuncaiReturnStatus, Long purchaseOrderId, Integer apiType) {
        String str1 = "";
        String str2 = "";

        switch (YouyuncaiApiType.from(apiType)) {
            case ORDER_CHECK_OUT:
                str1 = "order check out";
                str2 = "order.check.out";
                break;
            case ORDER_DELIVERY_ORDER:
                str1 = "order delivery order";
                str2 = "order.delivery.order";
                break;
        }

        if (Arguments.isNull(youyuncaiReturnStatus)) {
            log.info("[YOU-YUN-CAI]:post "+str1+" api is success but return is null");
            throw new JsonResponseException(str2+".return.empty");
        }

        if (Arguments.notNull(youyuncaiReturnStatus.getStatus()) && youyuncaiReturnStatus.getStatus() > 0) {

            if (Objects.equal(apiType, YouyuncaiApiType.ORDER_CHECK_OUT.value())) {
                //清空友云采订单备注信息
                PurchaseOrder purchaseOrder = new PurchaseOrder();
                purchaseOrder.setId(purchaseOrderId);
                Map<String, String> extraMap = Maps.newHashMap();
                extraMap.put(YouyuncaiConstants.BUYER_COOKIE, "");
                extraMap.put(YouyuncaiConstants.USER_CODE, "");
                extraMap.put(YouyuncaiConstants.USER_NAME, "");
                extraMap.put(YouyuncaiConstants.CHECKOUT_REDIRECT_URL, "");
                purchaseOrder.setExtra(extraMap);
                Response<Boolean> response = purchaseOrderWriteService.updatePurchaseOrder(purchaseOrder);
                if(!response.isSuccess()){
                    log.error("update purchase order :{} fail,error:{}",purchaseOrder,response.getError());
                    // 不报错
                }

                //清空采购单SKU信息
                Response<Boolean> rsp = purchaseSkuOrderWriteService.deleteByPurchaseOrderId(purchaseOrderId);
                if(!rsp.isSuccess()){
                    log.error("delete purchase sku order failed , purchaseOrderId = {} , error:{}",purchaseOrderId, rsp.getError());
                    // 不报错
                }
            }

            return Boolean.TRUE;
        }
        throw new JsonResponseException(youyuncaiReturnStatus.getErrormsg());
    }




    /**
     * 获取未同步的类目信息
     * @param pageNo 当前页
     * @param size   每页数量
     * @return 结果
     */
    private List<BackCategory> getNotSyncBackCategoryList(int pageNo, int size, Integer channel) {
        Integer status = 1;//只查询已启用状态的类目
        Response<Paging<BackCategory>> pagingRes = vegaCategoryReadService.pagingByNotSync(pageNo, size, status, channel);
        if (!pagingRes.isSuccess()) {
            log.error("paging back category not sync fail, status:{}, channel:{}, error:{}", Boolean.TRUE, channel, pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult().getData();
    }

    /**
     * 获取未同步的商品(SKU)信息
     * @param pageNo 当前页
     * @param size   每页数量
     * @return 结果
     */
    private List<SkuDto> getNotSyncSkuList(int pageNo, int size, Integer channel) {
        Integer status = 1;//只查询已上架状态的商品
        Integer syncStatus = 1; //同步标志,剔除已经同步的记录(非空即可)
        Response<Paging<Sku>> pagingRes = vegaSkuReadService.pagingBySkuSync(pageNo, size, status, channel, syncStatus);
        if (!pagingRes.isSuccess()) {
            log.error("paging sku not sync fail, status:{}, channel:{}, syncStatus:{}, error:{}",
                    Boolean.TRUE, channel, syncStatus, pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }
        List<Sku> skuList = pagingRes.getResult().getData();

        return getYouyuncaiSkuDataPackage(skuList);
    }

    /**
     * 根据商品Id获取待同步的SKU信息
     * @param itemId  商品Id
     * @param channel 同步渠道
     * @param type    类型(分类/商品)
     * @param apiType 接口类型
     * @return
     */
    private List<SkuDto> getSkuListByItem(Long itemId,Integer channel, Integer type, Integer apiType) {
        Response<List<Sku>> findSkus = skuReadService.findSkusByItemId(itemId);
        if (!findSkus.isSuccess()) {
            log.error("fail to find skus by itemId={},cause:{}", itemId, findSkus.getError());
            throw new JsonResponseException(findSkus.getError());
        }
        List<Sku> skus = findSkus.getResult();
        List<Long> skuIds = Lists.transform(skus, Sku::getId);

        if (Objects.equal(apiType, YouyuncaiApiType.ITEM_ADD.value())) {
            // 商品新增同步时需要判断是否重复
            Response<List<VegaItemSync>> itemSyncRsp = vegaItemSyncReadService.findByChannelAndTypeAndSyncIds(channel, type, skuIds);
            if (!itemSyncRsp.isSuccess()) {
                log.error("fail to find item sync by channel={}, type={}, skuIds={} ,cause:{}", channel, type, skuIds, itemSyncRsp.getError());
                throw new JsonResponseException(itemSyncRsp.getError());
            }
            List<Long> exitsSkuIds = Lists.transform(itemSyncRsp.getResult(), VegaItemSync::getSyncId);

            skus.removeIf(sku -> exitsSkuIds.contains(sku.getId())); //删除已经同步的记录
        }

        if (Arguments.isNullOrEmpty(skus)) {
            return null;
        }

        if (Objects.equal(apiType, YouyuncaiApiType.ITEM_DELETE.value())) {
            // 删除
            List<SkuDto> skuDtos = Lists.newArrayList();
            skuIds.stream().forEach(skuId -> {
                SkuDto skuDto = new SkuDto();
                skuDto.setSkuCode(skuId.toString());
                skuDto.setMemo(itemId.toString()); //商品ID临时记录到备注信息
                skuDtos.add(skuDto);
            });
            return skuDtos;
        }

        return getYouyuncaiSkuDataPackage(skus);
    }

    /**
     * 根据商品IDs拆分组装新增/修改/删除接口数据
     * @param itemIds 商品Ids
     * @param channel 同步渠道
     * @param type    类型
     * @return 拆分好的数据
     */
    private Map<String,List<SkuDto>> getSkuListForSplitByItems(List<Long> itemIds,Integer channel, Integer type) {
        Map<String,List<SkuDto>> skuDtoListMap = Maps.newHashMap();
        Response<List<Sku>> findSkus = vegaSkuReadService.findAllByItemIds(itemIds);
        if (!findSkus.isSuccess()) {
            log.error("fail to find all skus by itemIds={},cause:{}", itemIds, findSkus.getError());
            throw new JsonResponseException(findSkus.getError());
        }
        List<Sku> allSkus = findSkus.getResult();
        if (Arguments.isNull(allSkus)) {
            return null;
        }

        List<SkuDto> deleteSkuDtos = Lists.newArrayList();
        List<Sku> deleteSkus = Lists.newArrayList();
        List<Integer> deleteStatus = ImmutableList.of(
                DefaultItemStatus.ITEM_DELETE,
                DefaultItemStatus.ITEM_FREEZE,
                DefaultItemStatus.ITEM_REFUSE
        );
        allSkus.stream().forEach(sku -> {
            if (deleteStatus.contains(sku.getStatus())) {
                // 1.删除:SKU编辑时存在个别删除的情况
                SkuDto skuDto = new SkuDto();
                skuDto.setSkuCode(sku.getId().toString());
                skuDto.setMemo(sku.getItemId().toString()); //商品ID临时记录到备注信息
                deleteSkuDtos.add(skuDto);

                deleteSkus.add(sku);
            }
        });
        skuDtoListMap.put(VegaItemSync.SyncType.DELETE.name(), deleteSkuDtos);


        allSkus.removeAll(deleteSkus);
        Map<Long, Sku> skuMapBySkuId = Maps.uniqueIndex(allSkus, Sku::getId);
        List<Sku> addSkus = Lists.newArrayList();
        List<Sku> updateSkus = Lists.newArrayList();
        List<Long> skuIds = Lists.transform(allSkus, Sku::getId);
        for (Long skuId : skuIds) {
            Response<VegaItemSync> itemSyncRsp = vegaItemSyncReadService.findByChannelAndTypeAndSyncId(channel, type, skuId);
            if (!itemSyncRsp.isSuccess()) {
                log.error("fail to find item sync by channel={}, type={}, skuId={} ,cause:{}", channel, type, skuId, itemSyncRsp.getError());
                throw new JsonResponseException(itemSyncRsp.getError());
            }
            if (Arguments.isNull(itemSyncRsp.getResult())) {
                // 2.新增
                addSkus.add(skuMapBySkuId.get(skuId));
            } else {
                // 3.修改
                updateSkus.add(skuMapBySkuId.get(skuId));
            }
        }


        if (!Arguments.isNullOrEmpty(addSkus)) {
            List<SkuDto> addSkusDtos = getYouyuncaiSkuDataPackage(addSkus);
            skuDtoListMap.put(VegaItemSync.SyncType.ADD.name(), addSkusDtos);
        }

        if (!Arguments.isNullOrEmpty(updateSkus)) {
            List<SkuDto> updateSkusDtos = getYouyuncaiSkuDataPackage(updateSkus);
            skuDtoListMap.put(VegaItemSync.SyncType.UPDATE.name(), updateSkusDtos);
        }

        return skuDtoListMap;
    }

    /**
     * 友云采商品对接信息组装
     * @param skuList 集乘网skuList
     * @return 友云采skuList
     */
    private List<SkuDto> getYouyuncaiSkuDataPackage(List<Sku> skuList) {

        List<Long> itemIds = Lists.transform(skuList, Sku::getItemId);
        Response<List<Item>> itemsResp = itemReadService.findByIds(itemIds);
        if (!itemsResp.isSuccess()) {
            log.error("find item by ids:{}, cause:{}", itemIds, itemsResp.getError());
            throw new JsonResponseException(itemsResp.getError());
        }
        Map<Long, Item> itemIndexById = Maps.uniqueIndex(itemsResp.getResult(), Item::getId);

        List<SkuDto> skuDtos = Lists.newArrayList();
        skuList.stream().forEach(sku -> {
            SkuDto skuDto = new SkuDto();
            skuDto.setSkuCode(sku.getId().toString());
            skuDto.setName(itemIndexById.get(sku.getItemId()).getName());

            String attrs = "";
            List<SkuAttribute> skuAttributes = sku.getAttrs();
            List<SkuAttributeDto> skuAttributeDtos = Lists.newArrayList();
            if (!Arguments.isNullOrEmpty(skuAttributes)) {
                //规格
                for (SkuAttribute skuAttribute : skuAttributes) {
                    String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                    attrs += attr;
                }
                skuAttributes.stream().forEach(skuAttribute -> {
                    SkuAttributeDto skuAttributeDto = new SkuAttributeDto();
                    skuAttributeDto.setKey(skuAttribute.getAttrKey());
                    skuAttributeDto.setValue(skuAttribute.getAttrVal());
                    skuAttributeDtos.add(skuAttributeDto);
                });

            }

            skuDto.setSubject(itemIndexById.get(sku.getItemId()).getName() + " " + attrs);
            skuDto.setBrands(itemIndexById.get(sku.getItemId()).getBrandName());
            skuDto.setProductClass(itemIndexById.get(sku.getItemId()).getCategoryId().toString());
            skuDto.setStatusCode(sku.getStatus().toString());
            Map<String, String> extraMap = itemIndexById.get(sku.getItemId()).getExtra();
            skuDto.setCunit(Arguments.isNull(extraMap.get("unit")) ? "无" : extraMap.get("unit"));

            //取平台店铺的销售价
            ShopSku shopSku = findShopSku(sku.getId());
            if (Arguments.isNull(shopSku)) {
                return;
            }
            skuDto.setTaxPrice(NumberUtils.formatPrice(shopSku.getPrice()));
            skuDto.setTaxrate(SkuDto.TaxRate.STANDARD.value());
            skuDto.setPrice(NumberUtils.formatPrice(shopSku.getPrice()));

            ViewedItemDetailInfo viewedItemDetailInfo = findItemDetailInfoByItemId(sku.getItemId());
            List<ImageInfo> imageInfos = viewedItemDetailInfo.getItemDetail().getImages();
            List<String> imageUrls = Lists.newArrayList();
            String mainUrl = itemIndexById.get(sku.getItemId()).getMainImage();
            if (Arguments.notNull(mainUrl)) {
                imageUrls.add("http:" + mainUrl.trim().replaceAll("http:", ""));
            }
            if (!Arguments.isNullOrEmpty(imageInfos)) {
                imageInfos.stream().forEach(imageInfo -> {
                    String url = imageInfo.getUrl();
                    if (Arguments.notNull(url)) {
                        imageUrls.add("http:" + url.trim().replaceAll("http:", ""));
                    }
                });
            }
            if (Arguments.isNullOrEmpty(imageUrls)) {
                imageUrls.add("");
            }
            skuDto.setPictures(imageUrls.toArray());
            skuDto.setDetailInfo(Arguments.isNull(viewedItemDetailInfo.getItemDetail().getDetail()) ? " " : viewedItemDetailInfo.getItemDetail().getDetail());
            skuDto.setParameter(objectToJson(skuAttributeDtos));
            skuDto.setPunchoutUrl(webDomain+"/items/"+sku.getItemId());
            skuDto.setKeywords(itemIndexById.get(sku.getItemId()).getName() + " " + attrs);
            skuDto.setMemo(sku.getItemId().toString()); //商品ID临时记录到备注信息

            skuDtos.add(skuDto);
        });

        return skuDtos;
    }


    /**
     * 查找shopSku
     * @param skuId skuId
     * @return 结果
     */
    private ShopSku findShopSku(Long skuId) {
        Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId((long) VegaShopType.PLATFORM.value(),skuId);
        if (!findShopSku.isSuccess()) {
            log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                    VegaShopType.PLATFORM.value(), skuId, findShopSku.getError());
            throw new ServiceException(findShopSku.getError());
        }
        if (findShopSku.getResult().isPresent()) {
            return findShopSku.getResult().get();
        }
        return null;
    }

    /**
     * 获取商品详情
     * @param itemId 商品ID
     * @return 商品详情
     */
    private ViewedItemDetailInfo findItemDetailInfoByItemId(Long itemId) {
        Response<ViewedItemDetailInfo> viewedItemResp = itemReadService.findItemDetailInfoByItemId(itemId);
        if (!viewedItemResp.isSuccess()) {
            log.error("fail to find item detail by itemId:{},cause:{}", itemId, viewedItemResp.getError());
            throw new JsonResponseException(viewedItemResp.getError());
        }
        return viewedItemResp.getResult();
    }

    /**
     * 根据订单Id获取订单详情
     * @param orderId 订单Id
     * @return 订单详情
     */
    private VegaOrderDetail findOrderDetailByOrderId (Long orderId) {
        Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderId);
        if (!vegaOrderDetailRS.isSuccess()) {
            log.error("find OrderDetail by orderId:{} fail, cause:{}",
                    orderId, vegaOrderDetailRS.getError());
            throw new JsonResponseException(vegaOrderDetailRS.getError());
        }
        return vegaOrderDetailRS.getResult();
    }


    /**
     * 根据订单号获取友云采订单信息
     * @param orderId 订单号
     * @return 友云采订单详情
     */
    private YouyuncaiOrder findYouyuncaiOrderDetailByOrderId (Long orderId) {
        Response<YouyuncaiOrder> orderRS = youyuncaiOrderReadService.findByOrderId(orderId);
        if (!orderRS.isSuccess()) {
            log.error("find OrderDetail by orderId:{} fail, cause:{}",
                    orderId, orderRS.getError());
            throw new JsonResponseException(orderRS.getError());
        }
        return orderRS.getResult();
    }




    public String objectToJson(Object o) {
        try {
            if (Arguments.isNull(o)) {
                return null;
            }
            return JSON_MAPPER.toJson(o);
        } catch (Exception e) {
            log.error("[YOU-YUN-CAI]:fail to json from data={},cause:{}", o, Throwables.getStackTraceAsString(e));
            return null;
        }
    }
}
