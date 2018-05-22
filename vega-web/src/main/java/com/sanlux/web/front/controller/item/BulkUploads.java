package com.sanlux.web.front.controller.item;

import com.alibaba.dubbo.rpc.RpcException;
import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Resources;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.category.service.VegaCategoryReadService;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.enums.VegaTaskJobStatus;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.model.TaskJob;
import com.sanlux.item.service.*;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.events.ShopAddItemEvent;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiEvent;
import com.sanlux.web.front.core.util.ItemUploadExcelAnalyzer;
import com.sanlux.web.front.core.utils.ExportHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.cache.CategoryAttributeCacher;
import io.terminus.parana.category.dto.GroupedCategoryAttribute;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.ItemWriteService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.OrderReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.web.core.util.RichTextCleaner;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Objects;

/**
 * Created by cuiwentao
 * on 16/10/14
 */
@Slf4j
@RestController
@RequestMapping("/api/bulk-upload")
public class BulkUploads {

    @RpcConsumer
    private VegaCategoryReadService vegaCategoryReadService;

    @Autowired
    private CategoryAttributeCacher categoryAttributeCacher;

    @Autowired
    private ItemUploadExcelAnalyzer itemUploadExcelAnalyzer;

    @RpcConsumer
    private ItemImportWriteService itemImportWriteService;

    @RpcConsumer
    private VegaItemWriteService vegaItemWriteService;

    @RpcConsumer
    private VegaItemReadService vegaItemReadService;

    @RpcConsumer
    private ItemWriteService itemWriteService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private VegaTaskJobWriteService vegaTaskJobWriteService;

    @RpcConsumer
    private VegaTaskJobReadService vegaTaskJobReadService;

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @Autowired
    private EventBus eventBus;

    /**
     * 供应商导出导入商品模板
     * Download the default template to upload products
     */
    @RequestMapping(value = "/product-template/download", method = RequestMethod.GET)
    public void buildTemplate(@RequestParam Long categoryId, HttpServletResponse response) {
        Response<List<BackCategory>> categoryListResp = vegaCategoryReadService.findAncestorsByBackCategoryId(categoryId);
        if (!categoryListResp.isSuccess()) {
            log.error("find back category tree by categoryId:{} fail, cause:{}", categoryId, categoryListResp.getError());
            throw new JsonResponseException(categoryListResp.getError());
        }
        List<BackCategory> categoryList = categoryListResp.getResult();
        String[] path = new String[categoryList.size()];
        for (int i = 0; i < path.length; ++i) {
            path[i] = categoryList.get(i).getName();
        }

        String fileName = "商品导入模版(" + Joiner.on('-').useForNull("").join(path) + ").xlsx";
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new JsonResponseException(500, "excel.build.fail");
        }
        response.setContentType("application/ms-excel; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-disposition", "attachment; filename*=UTF-8''" + fileName);

        List<GroupedCategoryAttribute> attributeList = categoryAttributeCacher.findGroupedAttributeByCategoryId(categoryId);

        try {
            ItemUploadExcelAnalyzer.buildTemplateExcel(path, attributeList, response.getOutputStream());
        } catch (IOException e) {
            throw new JsonResponseException(500, "excel.build.fail");
        }
    }


    /**
     * 供应商导入商品
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String upload(MultipartFile file) {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            UploadRaw rawData = ItemUploadExcelAnalyzer.analyze(file.getInputStream());
            Response<Long> uploadRes = itemImportWriteService.uploadToImportRaw(user.getShopId(), rawData);
            if (!uploadRes.isSuccess()) {
                log.error("upload to import raw failed, cause:{}", uploadRes.getError());
                throw new JsonResponseException(uploadRes.getError());
            }

            return uploadRes.getResult().toString();
        } catch (ServiceException e) {
            String error = e.getMessage();
            log.error("upload failed, service exception:{}", error);
            throw new JsonResponseException(error);

        } catch (IOException e) {
            log.error("upload failed, analyze excel failed, cause:{}", e.getMessage());
            throw new JsonResponseException("upload.failed.analyze.excel.failed");

        } catch (RpcException e) {
            log.error("upload failed, invoke product upload write service failed, cause:{}", e.getMessage());
            throw new JsonResponseException("upload.failed.invoke.product.upload.write.service.failed");

        } catch (Exception e) {
            log.error("upload failed, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }


    /**
     * 批量设置商品图片
     *
     * @param categoryId
     * @param mainImage
     * @return
     */
    @RequestMapping(value = "/{categoryId}/main-image", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer batchUpdateImage(@PathVariable("categoryId") Long categoryId,
                                    @RequestParam("mainImage") String mainImage) {
        ParanaUser user = UserUtil.getCurrentUser();
        Response<Integer> updateResp =
                vegaItemWriteService.updateImageByCategoryIdAndShopId(categoryId, user.getShopId(), mainImage);
        if (!updateResp.isSuccess()) {
            log.error("batch update item mainImage by categoryId:{} and shopId:{} fail, mainImage:{}, cause:{}",
                    categoryId, user.getShopId(), mainImage, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        List<Long> itemIds = findItemIdsByCategoryIdAndShopId(categoryId, user.getShopId());
        if (updateResp.getResult() > 0 && !Arguments.isNullOrEmpty(itemIds)) {
            // 供应商批量更新图片成功,同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIds));
        }

        return updateResp.getResult();
    }

    /**
     * 批量设置商品详情
     *
     * @param categoryId
     * @param richText
     * @return
     */
    @RequestMapping(value = "/{categoryId}/item-detail", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchUpdateItemDetail(@PathVariable("categoryId") Long categoryId,
                                         @RequestParam("richText") String richText) {

        ParanaUser user = UserUtil.getCurrentUser();
        String safeRichText = RichTextCleaner.safe(richText);
        List<Long> itemIds = findItemIdsByCategoryIdAndShopId(categoryId, user.getShopId());

        Response<Boolean> response =
                vegaItemWriteService.batchUpdateRichText(itemIds, safeRichText);

        if (!response.isSuccess()) {
            log.error("batch update item detail fail, itemIds:{}, cause:{}", itemIds, response.getError());
            throw new JsonResponseException(response.getError());
        }

        if (response.getResult() && !Arguments.isNullOrEmpty(itemIds)) {
            // 供应商批量更商品详情,同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIds));
        }

        return response.getResult();

    }

    /**
     * 根据类目Id和店铺Id获取商品信息,剔除删除状态数据
     * @param categoryId 类目Id
     * @param shopId     店铺Id
     * @return itemIds
     */
    private List<Long> findItemIdsByCategoryIdAndShopId(Long categoryId, Long shopId) {
        List<Integer> statuses = Lists.newArrayList(DefaultItemStatus.ITEM_ONSHELF,
                DefaultItemStatus.ITEM_FREEZE,
                DefaultItemStatus.ITEM_WAIT_AUDIT,
                DefaultItemStatus.ITEM_REFUSE);


        Response<Optional<List<Long>>> itemIdsResp =
                vegaItemReadService.findItemIdsByCategoryIdAndShopId(categoryId, shopId, statuses);
        if (!itemIdsResp.isSuccess()) {
            log.error("find itemIds by categoryId:{} fail, cause:{}", categoryId, itemIdsResp.getError());
            throw new JsonResponseException(itemIdsResp.getError());
        }
        if (!itemIdsResp.getResult().isPresent()) {
            log.error("shop(id:{}) has no items in category(id:{})", shopId, categoryId);
            throw new JsonResponseException("shop.has.no.items.in.category");
        }

        return itemIdsResp.getResult().get();
    }


    /**
     * 经销商导出授权类目下商品
     *
     * @param response http
     */
    @RequestMapping(value = "/download/items", method = RequestMethod.GET)
    public void downloadCategoryItems(HttpServletResponse response) {
        final ParanaUser user = UserUtil.getCurrentUser();
        try {
            List<Long> categoryIds = findAuthCategoryIdsByShopId(user.getShopId());
            int size=5;
            if(!CollectionUtils.isEmpty(categoryIds)&&categoryIds.size()>10){
                size=categoryIds.size()/10;
            }

            List<Sku> skusAll = Lists.newArrayList();
            List<List<Long>> categoryIdList = createList(categoryIds, size);
            for (List<Long> cIds : categoryIdList) {
                List<Item> items = Lists.newArrayList();
                items.addAll(findItemsByCategoryIds(cIds));
                List<Sku> skus = Lists.newArrayList();
                if (!CollectionUtils.isEmpty(items)) {
                    List<Long> itemIds = Lists.transform(items, Item::getId);
                    Map<Long, Item> itemIndexById = Maps.uniqueIndex(items, Item::getId);

                    Response<List<Sku>> skusResp = skuReadService.findSkusByItemIds(itemIds);
                    if (!skusResp.isSuccess()) {
                        log.error("find skus by itemIds:{} fail, cause:{}",
                                itemIds, skusResp.getError());
                    }
                    skus = skusResp.getResult();
                    skus.forEach(sku -> sku.setName(itemIndexById.get(sku.getItemId()).getName()));
                }
                skusAll.addAll(skus);
            }
            //按照商品ID从小到大排序
            Collections.sort(skusAll, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    Sku sku0=(Sku)o1;
                    Sku sku1=(Sku)o2;
                    return sku0.getItemId().compareTo(sku1.getItemId());
                }
            });

            String xlsFileName = URLEncoder.encode("商品", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            buildSkusTemplateFile(skusAll, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of item failed, shopId:{} ", user.getShopId());
            throw new JsonResponseException("download.item.excel.fail");
        }
    }

    /**
     * 导出订单详情到excel
     * Created by lujm on 2017/02/13
     * @param orderId 订单号
     * @param supplierIsSeller 供应商作为卖家登录
     * @param response http
     */
    @RequestMapping(value = "/download/{orderId}/order/excel", method = RequestMethod.GET)
    public void downLoadOrderDetailExcel (@PathVariable("orderId")Long orderId,
                                          @RequestParam(value = "supplierIsSeller",defaultValue = "0",required = false) Boolean supplierIsSeller,
                                          HttpServletResponse response) {
        try {
            Response<VegaOrderDetail> vegaOrderDetailResponse = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderId);
            if (!vegaOrderDetailResponse.isSuccess()) {
                log.error("find OrderDetail by orderId:{} fail, cause:{}",
                        orderId, vegaOrderDetailResponse.getError());
                throw new JsonResponseException(vegaOrderDetailResponse.getError());
            }
            String xlsFileName = URLEncoder.encode("订单详情", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            buildOrderDetailTemplateFile(vegaOrderDetailResponse.getResult(), supplierIsSeller, response.getOutputStream());
        }catch(Exception e){
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, orderId:{} ", orderId);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
    }

    /**
     * 批量导出订单详情到excel
     *
     * @param orderIds 订单号IDs
     * @param response http
     */
    @RequestMapping(value = "/download/order/excel", method = RequestMethod.GET)
    public void batchDownLoadOrderDetailToExcel(@RequestParam(value = "orderIds", required = true) List<Long> orderIds,
                                           HttpServletResponse response) {
        try {
            String xlsFileName = URLEncoder.encode("订单详情", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            String headerKey = "Content-Disposition";
            response.setHeader(headerKey, headerValue);
            batchGetOrderDetailTemplateFile(orderIds, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, orderId:{} ", orderIds);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
    }


    /**
     * 经销商维护库存表格
     *
     * @param path excel真实路径
     * @return String
     */
    @RequestMapping(value = "/upload/items", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> uploadItems(String path) {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            if (user == null) {
                throw new JsonResponseException(401, "user.not.login");
            }
            TaskJob job = new TaskJob();
            job.setExtra(path);
            job.setUserId(user.getId());
            job.setStatus(VegaTaskJobStatus.WAIT_HANDLE.value());

            Response<String> response = vegaTaskJobWriteService.create(job);
            if (!response.isSuccess()) {
                log.error("create task job fail, cause:{}", response.getError());
                throw new JsonResponseException(500, response.getError());
            }
            eventBus.post(ShopAddItemEvent.from(user.getId(), user.getShopId(), path));
            Map<String, String> resultMap = Maps.newHashMap();
            resultMap.put("key", response.getResult());
            return resultMap;
        } catch (Exception e) {
            log.error("first shop and second shop upload items excel fail, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * 查看上传结果
     *
     * @param key
     * @return
     */
    @RequestMapping(value = "/upload/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskJob findTaskJobByKey(@RequestParam String key) {
        Response<TaskJob> response = vegaTaskJobReadService.findByKey(key);
        if (!response.isSuccess()) {
            log.error("find task job by key:{} fail, cause:{}", key, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    private void buildSkusTemplateFile(List<Sku> skus, OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("商品ID", 18 * 100);
            columnMaps.put("商品名称", 18 * 400);
            columnMaps.put("SKUID", 18 * 400);
            columnMaps.put("批号", 18 * 400);
            columnMaps.put("销售属性", 18 * 400);
            columnMaps.put("库存", 18 * 100);
            columnMaps.put("运费ID", 18 * 100);
            XSSFSheet xssfSheet = xssfWorkbook.createSheet("商品");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            Row row1 = xssfSheet.createRow(1);
            List<String> contents = Lists.newArrayList();
            contents.add("item_id");
            contents.add("item_name");
            contents.add("sku_id");
            contents.add("outer_sku_id");
            contents.add("sku_attr");
            contents.add("stock_quantity");
            contents.add("delivery_fee_template_id");
            ExportHelper.setContent(row1, contents);

            if (skus != null && skus.size() > 0) {
                for (int i = 0; i < skus.size(); i++) {
                    Row row = xssfSheet.createRow(i + 2);
                    ExportHelper.setContent(row, getOrderContent(skus.get(i)));
                }
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 根据订单号IDs批量组装订单详情到excel
     *
     * @param orderIds     订单IDs
     * @param outputStream OutputStream
     */
    private void batchGetOrderDetailTemplateFile(List<Long> orderIds, OutputStream outputStream) {
        try {
            InputStream inStream = Resources.asByteSource(Resources.getResource("excel/order-detail-template.xlsx")).openStream();
            XSSFWorkbook Workbook = new XSSFWorkbook(inStream);
            List<VegaOrderDetail> vegaOrderDetails = Lists.newArrayList();
            Long currentShopId = ((ParanaUser)UserUtil.getCurrentUser()).getShopId();

            for (int i = 1; i <= orderIds.size(); i++) {
                Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderIds.get(i - 1));
                if (!vegaOrderDetailRS.isSuccess()) {
                    log.error("find OrderDetail by orderId:{} fail, cause:{}",
                            orderIds.get(i), vegaOrderDetailRS.getError());
                    throw new JsonResponseException(vegaOrderDetailRS.getError());
                }
                if (!Objects.equals(currentShopId, vegaOrderDetailRS.getResult().getShopOrder().getShopId())) {
                    log.error("find OrderDetail fail, cause orderId = {} is not belong to this shop ", orderIds.get(i - 1));
                    continue;
                }
                vegaOrderDetails.add(vegaOrderDetailRS.getResult());
            }

            //订单汇总
            setSheetOrderDetailCollect(Workbook,vegaOrderDetails);

            Workbook.write(outputStream);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 组装订单汇总信息到sheet
     *
     * @param Workbook         XSSFWorkbook
     * @param vegaOrderDetails 订单信息集合
     */
    private void setSheetOrderDetailCollect(XSSFWorkbook Workbook, List<VegaOrderDetail> vegaOrderDetails) {
        try {
            XSSFSheet collectSheet = Workbook.getSheetAt(0);//汇总页模板
            XSSFSheet sourceSheet = Workbook.getSheetAt(1);//单个订单详情页模板

            //先按照商品订单日期排序,日期相同按照订单号排序
            Collections.sort(vegaOrderDetails, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    VegaOrderDetail vegaOrderDetail0=(VegaOrderDetail)o1;
                    VegaOrderDetail vegaOrderDetail1=(VegaOrderDetail)o2;
                    int i = vegaOrderDetail0.getShopOrder().getCreatedAt().compareTo(vegaOrderDetail1.getShopOrder().getCreatedAt());
                    if(i == 0){
                        int j = vegaOrderDetail0.getShopOrder().getId().compareTo(vegaOrderDetail1.getShopOrder().getId());
                        return j;
                    }
                    return i;
                }
            });
            Collections.reverse(vegaOrderDetails);

            int index0 = 0;
            int index1 = 0;
            int index2 = 0;

            int rowStart = 2; //订单日期合并默认起始行
            int rowEnd = 2;//订单日期合并默认截止行
            XSSFSheet sheet;
            for(VegaOrderDetail vegaOrderDetail : vegaOrderDetails  ){
                List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();
                //替换shopOrder的价格
                Long fee = 0L;
                ParanaUser user = UserUtil.getCurrentUser();
                OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);

                if(index2 > 0) {
                    //复制的sheet
                    sheet = Workbook.cloneSheet(1);
                    Workbook.setSheetName(index2 + 1, "订单号(" + vegaOrderDetail.getShopOrder().getId() + ")");
                    //单个订单订单详情页组装
                    setSheetOrderDetail(sheet, vegaOrderDetail);

                    //供应商作为卖家登录 只展示供货价(单价 原价字段合并成供货价)
                    if(userType.equals(OrderUserType.SUPPLIER)){
                        ExportHelper.setContentAddMergedRegionByRowAndColumn(sheet, 9, 9, 4, 6, "供货价");
                        for (int i = 9 ; i <= sheet.getLastRowNum(); i++) {
                            ExportHelper.setContentAddMergedRegionByRowAndColumn(sheet, i + 1, i + 1, 4, 6, ExportHelper.getCellValueByCell(sheet.getRow(i).getCell(5)));
                        }
                    }
                }

                for (SkuOrder skuOrder : skuOrders){
                    String attrs = "";
                    List<SkuAttribute> skuAttributes = skuOrder.getSkuAttrs();
                    if (!CollectionUtils.isEmpty(skuAttributes)) {
                        for (SkuAttribute skuAttribute : skuAttributes) {
                            String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                            attrs += attr;
                        }
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 6, attrs);//规格
                    }

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 5, Arguments.isNull(skuOrder.getItemName()) ? "" :
                            skuOrder.getItemName());//产品名称

                    Map<String, String> tags = skuOrder.getTags();
                    String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE);

                    //二级经销商作为卖家登录 显示二级成本价
                    if(userType.equals(OrderUserType.DEALER_SECOND)){
                        orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE);
                    }
                    if (!Arguments.isNull(orderSkuSellerPrice) && !Objects.equals(orderSkuSellerPrice, "0")) {
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 7, NumberUtils.formatPrice(Integer.valueOf(orderSkuSellerPrice)));//服务商成本价
                    }

                    //根据shopId skuId获取商品原价
                    Long skuId = Arguments.isNull(skuOrder.getSkuId()) ? -1 : skuOrder.getSkuId();
                    //取平台店铺的销售价
                    Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId((long) VegaShopType.PLATFORM.value(), skuId);
                    if (!findShopSku.isSuccess()) {
                        log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                                VegaShopType.PLATFORM.value(), skuId, findShopSku.getError());
                        throw new ServiceException(findShopSku.getError());
                    }
                    if (findShopSku.getResult().isPresent()) {
                        double itemPrice = findShopSku.getResult().get().getPrice() / 100.00;//分转化为(元)
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 8, df1.format(itemPrice));//产品原价
                    }

                    if (!Arguments.isNull(skuOrder.getOriginFee()) && !Arguments.isNull(skuOrder.getQuantity())) {
                        double price = (skuOrder.getOriginFee() / skuOrder.getQuantity()) / 100.00;//分转化为(元)

                        //供应商作为卖家登录 显示供货价
                        if(userType.equals(OrderUserType.SUPPLIER)){
                            price = 0.0;
                            orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
                            if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                                price = Long.valueOf(orderSkuSellerPrice) / 100.00;
                                skuOrder.setFee(skuOrder.getQuantity() * Long.valueOf(orderSkuSellerPrice));
                                fee += skuOrder.getFee();
                            }
                        }
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 9, df1.format(price));//产品单价
                    }

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 10, Arguments.isNull(skuOrder.getQuantity()) ? "" :
                            skuOrder.getQuantity().toString());//数量


                    //根据itemId获取商品"单位"
                    Long itemId = Arguments.isNull(skuOrder.getItemId()) ? -1 : skuOrder.getItemId();
                    Response<Item> itemRes = itemReadService.findById(itemId);
                    if (!itemRes.isSuccess()) {
                        log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
                        throw new JsonResponseException(itemRes.getError());
                    }
                    Map<String, String> extraMap = itemRes.getResult().getExtra();
                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 11, Arguments.isNull(extraMap.get("unit")) ? "" :
                            extraMap.get("unit"));//单位

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 12, NumberUtils.formatPrice(skuOrder.getFee()));//实付款

                    index1 ++;
                }

                //供应商作为卖家登录 实付款(订单)需要显示供货价
                if(userType.equals(OrderUserType.SUPPLIER)) {
                    vegaOrderDetail.getShopOrder().setOriginFee(fee);
                    fee += vegaOrderDetail.getShopOrder().getShipFee();
                    vegaOrderDetail.getShopOrder().setFee(fee);
                }

                /**
                 * 订单日期合并处理逻辑:
                 *
                 * 1.只有一个订单时,直接对日期进行赋值
                 * 2.有多个订单,循环订单列表
                 *   1)第一个订单时,根据SKU数量修改截止行数位置
                 *   2)其他订单,取当前订单日期和上一个订单日期进行对比,如果相同就更新截止行数位置,不同时就对上一订单日期进行赋值,并更新起始和截止位置
                 *   3)最后一个订单时,直接对当前订单进行日期赋值
                 */
                if (vegaOrderDetails.size() == 1) {
                    //只有一条记录
                    rowStart = rowStart + 1;
                    rowEnd = rowEnd + vegaOrderDetail.getSkuOrders().size();
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, Arguments.isNull(vegaOrderDetail.getShopOrder().getCreatedAt()) ? "" :
                            DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetail.getShopOrder().getCreatedAt())));//订单日期

                } else {
                    if (index2 > 0) {
                        if (!Objects.equals(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetail.getShopOrder().getCreatedAt())),
                                DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt())))) {
                            //日期和前一行不相同
                            ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, Arguments.isNull(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt()) ? "" :
                                    DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt())));//订单日期
                            rowStart = rowEnd + 1;
                            rowEnd = rowStart + vegaOrderDetail.getSkuOrders().size() - 1;
                        } else {
                            //日期和前一行相同
                            rowEnd = rowEnd + vegaOrderDetail.getSkuOrders().size();
                        }
                    } else {
                        //第一行
                        rowStart = rowStart + 1;
                        rowEnd = rowEnd + vegaOrderDetail.getSkuOrders().size();
                    }
                    if (index2 == vegaOrderDetails.size() - 1) {
                        //最后一行
                        ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, Arguments.isNull(vegaOrderDetail.getShopOrder().getCreatedAt()) ? "" :
                                DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetail.getShopOrder().getCreatedAt())));//订单日期
                    }
                }


                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 2, 2, Arguments.isNull(vegaOrderDetail.getShopOrder().getId()) ? "" :
                        vegaOrderDetail.getShopOrder().getId().toString());//订单号

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 3, 3, Arguments.isNull(vegaOrderDetail.getShopOrder().getShopName()) ? "" :
                        vegaOrderDetail.getShopOrder().getShopName());//卖家

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 4, 4, Arguments.isNull(vegaOrderDetail.getShopOrder().getBuyerName()) ? "" :
                        vegaOrderDetail.getShopOrder().getBuyerName());//买家

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 13, 13, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getShipFee()));//运费

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 14, 14, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getFee()));//实付款(订单)


                index0 = index0 +skuOrders.size();
                index1 = 0;//重新赋值
                index2 ++;
            }

            //供应商作为卖家登录 只展示供货价(单价 原价字段合并成供货价)
            ParanaUser user = UserUtil.getCurrentUser();
            OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);
            if(userType.equals(OrderUserType.SUPPLIER)){
                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 2, 2, 7, 9, "供货价");
                for (int i = 2 ; i <= collectSheet.getLastRowNum(); i++) {
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, i + 1, i + 1, 7, 9, ExportHelper.getCellValueByCell(collectSheet.getRow(i).getCell(8)));
                }
            }

            //最后进行模板页订单详情组装
            if(vegaOrderDetails.size() > 0) {
                Workbook.setSheetName(1, "订单号(" + vegaOrderDetails.get(0).getShopOrder().getId() + ")");
                setSheetOrderDetail(sourceSheet, vegaOrderDetails.get(0));

                //供应商作为卖家登录 只展示供货价(单价 原价字段合并成供货价)
                if(userType.equals(OrderUserType.SUPPLIER)){
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(sourceSheet, 9, 9, 4, 6, "供货价");
                    for (int i = 9 ; i <= sourceSheet.getLastRowNum(); i++) {
                        ExportHelper.setContentAddMergedRegionByRowAndColumn(sourceSheet, i + 1, i + 1, 4, 6, ExportHelper.getCellValueByCell(sourceSheet.getRow(i).getCell(5)));
                    }
                }
            }

        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 组装订单详情到sheet
     *
     * @param sheet           sheet
     * @param vegaOrderDetail 订单信息
     */
    private void setSheetOrderDetail(XSSFSheet sheet, VegaOrderDetail vegaOrderDetail) {
        try {
            /**  1.订单详情   */
            ShopOrder shopOrder = vegaOrderDetail.getShopOrder();
            buildOrderDetail(sheet, shopOrder);

            /**  2.买家信息   */
            List<OrderReceiverInfo> orderReceiverInfos = vegaOrderDetail.getOrderReceiverInfos();
            buildBuyerInfo(sheet, shopOrder, orderReceiverInfos);

            /**  3.SKU信息   */
            List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();
            buildSkuOrdersInfo(sheet, skuOrders, Boolean.TRUE);

            /**  4.发票信息   */
            List<Invoice> invoices = vegaOrderDetail.getInvoices();
            buildInvoices(sheet, invoices);

        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }


    /**
     * 组装订单详情到excel
     * Created by lujm on 2017/02/13
     *
     * @param vegaOrderDetail 订单号
     * @param supplierIsSeller 供应商作为卖家登录
     * @param outputStream    OutputStream
     */
    private void buildOrderDetailTemplateFile(VegaOrderDetail vegaOrderDetail, Boolean supplierIsSeller, OutputStream outputStream) {
        try {
            InputStream inputStream = Resources.asByteSource(Resources.getResource("excel/order-detail-template.xlsx")).openStream();
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(1);
            xssfWorkbook.removeSheetAt(0);//删除订单汇总sheet
            /**  1.订单详情   */
            ShopOrder shopOrder = vegaOrderDetail.getShopOrder();
            buildOrderDetail(xssfSheet,shopOrder);

            /**  2.买家信息   */
            List<OrderReceiverInfo> orderReceiverInfos= vegaOrderDetail.getOrderReceiverInfos();
            buildBuyerInfo(xssfSheet,shopOrder,orderReceiverInfos);

            /**  3.SKU信息   */
            List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();
            buildSkuOrdersInfo(xssfSheet,skuOrders, supplierIsSeller);

            /**  4.发票信息   */
            List<Invoice> invoices = vegaOrderDetail.getInvoices();
            buildInvoices(xssfSheet,invoices);

            ParanaUser user = UserUtil.getCurrentUser();
            OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);

            //供应商作为卖家登录 只展示供货价(单价 原价字段合并成供货价)
            if(userType.equals(OrderUserType.SUPPLIER) && supplierIsSeller){
                ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, 9, 9, 4, 6, "供货价");
                for (int i = 9 ; i <= xssfSheet.getLastRowNum(); i++) {
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, i + 1, i + 1, 4, 6, ExportHelper.getCellValueByCell(xssfSheet.getRow(i).getCell(5)));
                }
            }

            // 买家身份不显示成本价
            if(!supplierIsSeller){
                ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, 9, 9, 4, 5, "原价");
                for (int i = 9 ; i <= xssfSheet.getLastRowNum(); i++) {
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, i + 1, i + 1, 4, 5, ExportHelper.getCellValueByCell(xssfSheet.getRow(i).getCell(4)));
                }
            }

            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 订单详情组装
     * Created by lujm on 2017/02/15
     *
     * @param Sheet     Sheet
     * @param shopOrder shopOrder
     */
    private void buildOrderDetail(XSSFSheet Sheet, ShopOrder shopOrder) {
        try {
            ExportHelper.setContentByRowAndColumn(Sheet, 2, 2, Arguments.isNull(shopOrder.getId()) ? "" :
                    shopOrder.getId().toString());//订单号
            ExportHelper.setContentByRowAndColumn(Sheet, 2, 9, Arguments.isNull(shopOrder.getCreatedAt()) ? "" :
                    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime(shopOrder.getCreatedAt())));//创建时间
            ExportHelper.setContentByRowAndColumn(Sheet, 8, 2, NumberUtils.formatPrice(shopOrder.getShipFee()));//运费
        }catch (Exception e) {
            log.error("build order detail info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 买家信息组装
     * Created by lujm on 2017/02/15
     *
     * @param Sheet     Sheet
     * @param shopOrder shopOrder
     * @param orderReceiverInfos orderReceiverInfos
     */
    private void buildBuyerInfo(XSSFSheet Sheet, ShopOrder shopOrder,List<OrderReceiverInfo> orderReceiverInfos) {
        try {
            String buyerInfo="收货人地址:";
            if (!CollectionUtils.isEmpty(orderReceiverInfos)) {
                String receiveAddress =(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getProvince()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getProvince());//省份
                receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getCity()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getCity());//城市
                receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getRegion()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getRegion());//区县
                receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getDetail()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getDetail());//详细地址

                buyerInfo +="    "+(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName());//收货人姓名
                buyerInfo +="   "+(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getMobile()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getMobile());//收货人电话
                buyerInfo +="   "+receiveAddress;//收货人地址
                buyerInfo +="   "+(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getPostcode()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getPostcode());//收货人邮编

            }
            buyerInfo +="\r\n";//换行
            String buyerNote=Arguments.isNull(shopOrder.getBuyerNote()) ? "" : shopOrder.getBuyerNote();
            buyerInfo +="买家留言: "+(Arguments.isEmpty(buyerNote)?"无":buyerNote);

            ExportHelper.setContentByRowAndColumn(Sheet, 4, 2, buyerInfo);
        }catch (Exception e) {
            log.error("build buyer info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * SkuOrder信息组装
     * Created by lujm on 2017/02/15
     *
     * @param xssfSheet     xssfSheet
     * @param supplierIsSeller 供应商作为卖家登录
     * @param skuOrders SkuOrder
     */
    private void buildSkuOrdersInfo(XSSFSheet xssfSheet, List<SkuOrder> skuOrders, Boolean supplierIsSeller) {
        try {
            if (skuOrders != null && skuOrders.size() > 0) {
                for (int i = 0; i < skuOrders.size(); i++) {
                    SkuOrder skuOrder = skuOrders.get(i);
                    //根据shopId skuId获取商品原价
                    Long skuId=Arguments.isNull(skuOrder.getSkuId()) ? -1 : skuOrder.getSkuId();
                    //获取平台店铺的销售价
                    Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId((long)VegaShopType.PLATFORM.value(),skuId);
                    if (!findShopSku.isSuccess()) {
                        log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                                (long)VegaShopType.PLATFORM.value(), skuId, findShopSku.getError());
                        throw new ServiceException(findShopSku.getError());
                    }
                    if (findShopSku.getResult().isPresent()) {
                        //产品原价
                        double itemPrice = findShopSku.getResult().get().getPrice()  / 100.00;//分转化为(元)
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 5,df1.format(itemPrice));//产品原价
                    }

                    //根据itemId获取商品"单位"
                    Long itemId=Arguments.isNull(skuOrder.getItemId()) ? -1 : skuOrder.getItemId();
                    Response<Item> itemRes = itemReadService.findById(itemId);
                    if (!itemRes.isSuccess()) {
                        log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
                        throw new JsonResponseException(itemRes.getError());
                    }
                    Map<String,String> extraMap=itemRes.getResult().getExtra();
                    ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 8,Arguments.isNull(extraMap.get("unit")) ? "" :
                            extraMap.get("unit"));//单位

                    ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 1, Arguments.isNull(skuOrder.getItemName()) ? "" :
                            skuOrder.getItemName());//产品名称
                    String attrs = "";
                    List<SkuAttribute> skuAttributes = skuOrder.getSkuAttrs();
                    if (!CollectionUtils.isEmpty(skuAttributes)) {
                        //规格
                        for (SkuAttribute skuAttribute : skuAttributes) {
                            String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                            attrs += attr;
                        }
                        ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 2, attrs);
                    }
                    ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 3, Arguments.isNull(skuOrder.getSkuId()) ? "" :
                            skuOrder.getSkuId().toString());//SKUID

                    ParanaUser user = UserUtil.getCurrentUser();
                    OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);
                    Map<String, String> tags = skuOrder.getTags();
                    String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE);

                    //二级经销商作为卖家登录 显示二级成本价
                    if(userType.equals(OrderUserType.DEALER_SECOND) && supplierIsSeller){
                        orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE);
                    }
                    if (!Arguments.isNull(orderSkuSellerPrice) && !Objects.equals(orderSkuSellerPrice, "0")) {
                        ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 4, NumberUtils.formatPrice(Integer.valueOf(orderSkuSellerPrice))); //服务商成本价
                    }

                    if (!Arguments.isNull(skuOrder.getOriginFee()) && !Arguments.isNull(skuOrder.getQuantity())) {
                        //单价
                        double price = (skuOrder.getOriginFee() / skuOrder.getQuantity()) / 100.00;//分转化为(元)

                        //供应商作为卖家登录 显示供货价
                        if(userType.equals(OrderUserType.SUPPLIER) && supplierIsSeller){
                            price = 0.0;
                            orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
                            if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                                price = Long.valueOf(orderSkuSellerPrice) / 100.00;
                                skuOrder.setFee(skuOrder.getQuantity() * Long.valueOf(orderSkuSellerPrice));
                            }
                        }
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 6, df1.format(price));
                    }
                    ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 7, Arguments.isNull(skuOrder.getQuantity()) ? "" :
                            skuOrder.getQuantity().toString());//数量
                    ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 9, Arguments.isNull(skuOrder.getDiscount()) ? "0.00" :
                            skuOrder.getDiscount().toString());//优惠
                    if (!Arguments.isNull(skuOrder.getFee())) {
                        //实付款
                        DecimalFormat df2 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(xssfSheet, i + 10, 10, df2.format(skuOrder.getFee() / 100.00));
                    }
                    //"备注栏"暂不填充,预留以后扩展
                }
            }
        }catch (Exception e) {
            log.error("build sku order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 发票信息组装
     * Created by lujm on 2017/02/15
     *
     * @param Sheet     Sheet
     * @param invoices invoices
     */
    private void buildInvoices(XSSFSheet Sheet, List<Invoice> invoices) {
        try {
            if (invoices != null && invoices.size() > 0) {
                String invoiceInfo = "";
                for (int i = 0; i < invoices.size(); i++) {
                    Invoice invoice = invoices.get(i);
                    Map<String,String> invoiceTypeMap = invoice.getDetail();
                    String invoiceType=Arguments.isNull(invoiceTypeMap.get("type"))?"":invoiceTypeMap.get("type");//普通发票&增值发票
                    String titleType=Arguments.isNull(invoiceTypeMap.get("titleType"))?"":invoiceTypeMap.get("titleType");//个人&公司
                    if(invoiceType.equals("2")){
                        invoiceInfo +="发票类型: 增值发票"+"\r\n";
                        invoiceInfo +="发票抬头: "+invoice.getTitle()+"\r\n";

                        invoiceInfo +="公司名称: "+(Arguments.isNull(invoiceTypeMap.get("companyName"))?"":invoiceTypeMap.get("companyName"))+"\r\n";
                        invoiceInfo +="企业税号: "+(Arguments.isNull(invoiceTypeMap.get("taxRegisterNo"))?"":invoiceTypeMap.get("taxRegisterNo"))+"\r\n";
                        invoiceInfo +="注册地址: "+(Arguments.isNull(invoiceTypeMap.get("registerAddress"))?"":invoiceTypeMap.get("registerAddress"))+"\r\n";
                        invoiceInfo +="注册电话: "+(Arguments.isNull(invoiceTypeMap.get("registerPhone"))?"":invoiceTypeMap.get("registerPhone"))+"\r\n";
                        invoiceInfo +="开户行: "+(Arguments.isNull(invoiceTypeMap.get("registerBank"))?"":invoiceTypeMap.get("registerBank"))+"\r\n";
                        invoiceInfo +="开户账号: "+(Arguments.isNull(invoiceTypeMap.get("bankAccount"))?"":invoiceTypeMap.get("bankAccount"));
                    }else{
                        invoiceInfo +="发票类型: 普通发票"+"\r\n";
                        invoiceInfo +="发票抬头: "+invoice.getTitle()+"\r\n";
                        if(titleType.equals("2")){
                            if (!Arguments.isNull(invoiceTypeMap.get("taxIdentityNo"))) {
                                invoiceInfo +="企业税号: "+invoiceTypeMap.get("taxIdentityNo")+"\r\n";
                            }
                            invoiceInfo +="内容: 公司";
                        }else{
                            invoiceInfo +="内容: 个人";
                        }
                    }
                }
                ExportHelper.setContentByRowAndColumn(Sheet, 6, 2, invoiceInfo);
            }
        }catch (Exception e) {
            log.error("build order detail info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }
    private List<Long> findAuthCategoryIdsByShopId(Long shopId) {
        Response<Optional<CategoryAuthe>> autheResp =
                categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!autheResp.isSuccess()) {
            log.error("find category auth by shopId:{} fail, cause:{}",
                    shopId, autheResp.getError());
            throw new JsonResponseException(autheResp.getError());
        }
        if (!autheResp.getResult().isPresent()) {
            log.error("find category auth by shopId:{} empty", shopId);
            throw new JsonResponseException("find.category.auth.empty");
        }
        List<Long> authCategoryIds = Lists.newArrayList();
        List<VegaCategoryDiscountDto> discountList = autheResp.getResult().get().getDiscountList();
        discountList.forEach(vegaCategoryDiscountDto -> {
            if (vegaCategoryDiscountDto.getIsUse()) {
                authCategoryIds.add(vegaCategoryDiscountDto.getCategoryId());
            }
        });
        List<Long> categoryIds = Lists.newArrayList();
        for (Long categoryId : authCategoryIds) {
            Response<BackCategory> bcResp = backCategoryReadService.findById(categoryId);
            if (!bcResp.isSuccess()) {
                log.error("find backCategory by id:{} fial, cause:{}", categoryId, bcResp.getError());
                throw new JsonResponseException(bcResp.getError());
            }
            if (!bcResp.getResult().getHasChildren()) {
                categoryIds.add(categoryId);
            } else {
                categoryIds.addAll(getChildCategoryIds(categoryId));
            }
        }
        return categoryIds;
    }


    //转换订单信息
    private List<String> getOrderContent(Sku sku) {
        List<String> contents = Lists.newArrayList();
        contents.add(Arguments.isNull(sku.getItemId()) ? "" : sku.getItemId().toString());
        contents.add(Strings.isNullOrEmpty(sku.getName()) ? "" : sku.getName());
        contents.add(Arguments.isNull(sku.getId()) ? "" : sku.getId().toString());
        contents.add(Strings.isNullOrEmpty(sku.getOuterSkuId()) ? "" : sku.getOuterSkuId());

        String attrs = "";
        List<SkuAttribute> skuAttributes = sku.getAttrs();
        if (!CollectionUtils.isEmpty(skuAttributes)) {
            for (SkuAttribute skuAttribute : skuAttributes) {
                String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                attrs += attr;
            }
        }
        contents.add(attrs);


        return contents;
    }


    private List<Long> getChildCategoryIds(Long categoryId) {
        Response<List<BackCategory>> bcResp = backCategoryReadService.findChildrenByPid(categoryId);
        if (!bcResp.isSuccess()) {
            log.error("find back categories by pid:{} fail, cause:{}", categoryId, bcResp.getError());
            throw new JsonResponseException(bcResp.getError());
        }
        List<Long> categoryIds = Lists.newArrayList();
        for (BackCategory bc : bcResp.getResult()) {
            if (!bc.getHasChildren()) {
                categoryIds.add(bc.getId());
            } else {
                categoryIds.addAll(getChildCategoryIds(bc.getId()));
            }
        }
        return categoryIds;
    }

    public static List<List<Long>> createList(List<Long> targe, int size) {
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

    private List<Item> findItemsByCategoryIds(List<Long> categoryIds) {
        Response<List<Item>> itemsResp =
                vegaItemReadService.findItemsByCategoryIds(categoryIds);
        if (!itemsResp.isSuccess()) {
            log.error("find items by categoryIds:{} fail, cause:{}",
                    categoryIds, itemsResp.getError());
            throw new JsonResponseException(itemsResp.getError());
        }
        return itemsResp.getResult();
    }
}
