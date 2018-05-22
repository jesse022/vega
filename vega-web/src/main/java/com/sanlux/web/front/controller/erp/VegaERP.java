package com.sanlux.web.front.controller.erp;

import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.service.VegaSkuReadService;
import com.sanlux.item.service.VegaSkuWriteService;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiEvent;
import com.sanlux.web.front.core.util.ItemUploadExcelAnalyzer;
import com.sanlux.web.front.core.utils.ExportHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by cuiwentao
 * on 16/10/26
 */
@RestController
@Slf4j
@RequestMapping("/api/erp")
public class VegaERP {

    @RpcConsumer
    private VegaSkuWriteService vegaSkuWriteService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private VegaSkuReadService vegaSkuReadService;

    @Autowired
    private EventBus eventBus;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd");


    @RequestMapping(value = "/download/stock-quantity/excel", method = RequestMethod.GET)
    public void downLoadStockExcel (HttpServletResponse httpServletResponse) {

        try {

            String xlsFileName = URLEncoder.encode("库存管理表", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildStockQuantityTemplateFile(httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of stock quantity failed ");
            throw new JsonResponseException("download.stock.manager.excel.fail");

        }
    }


    @RequestMapping(value = "/upload/stock-quantity/excel", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public Boolean uploadStockQuantity (MultipartFile file) throws IOException {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            UploadRaw rawData = ItemUploadExcelAnalyzer.analyzeStockExcel(file.getInputStream());
            Response<Map<String, Object>> response = vegaSkuWriteService.uploadToImportRaw(user.getShopId(), rawData);
            if (!response.isSuccess()) {
                log.error("upload excel batch set shop sku stock quantity fail, shopId:{}, cause:{}",
                        user.getShopId(), response.getError());
                throw new JsonResponseException(response.getError());
            }
            Map<String, Object> returnMap = response.getResult();
            Boolean isSuccess = Arguments.isNull(returnMap.get("status")) ? Boolean.FALSE : (Boolean) returnMap.get("status");

            if (isSuccess && !Arguments.isNull(returnMap.get("items"))) if (returnMap.get("items") instanceof List) {
                List itemList = (List) returnMap.get("items");
                if (!Arguments.isNullOrEmpty(itemList)) {
                    List<Long> itemIds = Lists.newArrayList();
                    for (int i = 0; i < itemList.size(); i++) {
                        Object o = itemList.get(i);
                        if (o instanceof Item) {
                            itemIds.add(((Item) o).getId());
                        }
                    }

                    // 供应商批量更新库存成功,同步友云采
                    eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIds));
                }
            }

            return isSuccess;
        } catch (Exception e) {
            log.error("upload stock quantity manage excel fail, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    @RequestMapping(value = "/download/{orderId}/item/excel", method = RequestMethod.GET)
    public void downLoadOrderItemExcel (@PathVariable("orderId")Long orderId,
                                        HttpServletResponse httpServletResponse) {

        try {

            String xlsFileName = URLEncoder.encode("订单商品详情表", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);

            Response<List<SkuOrder>> skuOrders = skuOrderReadService.findByShopOrderId(orderId);
            if (!skuOrders.isSuccess()) {
                log.error("find sku order by orderId:{} fail, cause:{}", orderId, skuOrders.getError());
                throw new JsonResponseException(skuOrders.getError());
            }
            List<SkuOrder> skuOrderList = skuOrders.getResult();

            buildOrderItemTemplateFile(skuOrderList, httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of stock quantity failed ");
            throw new JsonResponseException("download.order.item.excel.fail");

        }
    }

    @RequestMapping(value = "/fix-outer-sku-id", method = RequestMethod.GET)
    public Boolean fixOuterSkuId() {
        Integer pageNo = 1;
        Integer pageSize = 200;
        while(true) {
            Response<Paging<Sku>> resp = vegaSkuReadService.paging(pageNo, pageSize);
            if (!resp.isSuccess()) {
                log.error("failed to find sku  cause : {}", resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<Sku> paging = resp.getResult();
            List<Sku> skuList = paging.getData();
            if (!skuList.isEmpty()) {
                skuList.forEach(sku -> {
                    Map<String, String> extra = sku.getExtra();
                    sku.setOuterSkuId(extra.get("brandCode") + extra.get("otherNo"));
                });
                Response<Boolean> updateResp = vegaSkuWriteService.batchUpdateOuterSkuId(skuList);
                if (!updateResp.isSuccess()) {
                    log.error("fail to batch update outerSkuId, skuList:{}, cause:{}",
                            skuList, updateResp.getError());
                }
            }

            Long total = paging.getTotal();
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;

        }
        return Boolean.TRUE;
    }

    /**
     * 供应商下载批量设置供货价模板,导出当前店铺冻结状态下的所有商品
     *
     * @param httpServletResponse
     */
    @RequestMapping(value = "/download/seller-price/excel", method = RequestMethod.GET)
    public void downLoadSellerPriceExcel (HttpServletResponse httpServletResponse) {
        try {
            String xlsFileName = URLEncoder.encode("供应商批量设置供货价模板", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildSellerPricTemplateFile(httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download.batch.update.seller.price.excel.fail");
            throw new JsonResponseException("download.batch.update.seller.price.excel.fail");
        }
    }

    /**
     * 供应商批量导入商品供货价
     * @param file
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/upload/seller-price/excel", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public Response<Boolean> uploadSellerPrice (MultipartFile file) throws IOException {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            UploadRaw rawData = ItemUploadExcelAnalyzer.analyzeSellerPriceExcel(file.getInputStream());
            Response<Boolean> response = vegaSkuWriteService.batchUpdateSellerPriceByExcel(user.getShopId(), rawData);

            if (!response.isSuccess()) {
                log.error("upload batch update seller price excel failed, shopId:{}, cause:{}",
                        user.getShopId(), response.getError());
                throw new JsonResponseException(response.getError());
            }
            return Response.ok(response.getResult());
        } catch (Exception e) {
            log.error("upload batch update seller price excel failed, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    private void buildOrderItemTemplateFile(List<SkuOrder> skuOrders, OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("单号", 18 * 100);
            columnMaps.put("客户代号", 18 * 400);
            columnMaps.put("受购/订单日期", 18 * 400);

            columnMaps.put("货品名称", 18 * 400);
            columnMaps.put("对方货号", 18 * 400);
            columnMaps.put("数量", 18 * 400);
            columnMaps.put("批号代号", 18 * 400);
            columnMaps.put("商标", 18 * 400);
            columnMaps.put("预交日", 18 * 400);

            XSSFSheet xssfSheet = xssfWorkbook.createSheet("订单商品表");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            if (skuOrders != null && skuOrders.size() > 0) {
                List<Long> skuIds = Lists.transform(skuOrders, SkuOrder::getSkuId);
                List<Long> itemIds = Lists.transform(skuOrders, SkuOrder::getItemId);

                Response<List<Sku>> skusResp = skuReadService.findSkusByIds(skuIds);
                if (!skusResp.isSuccess()) {
                    log.error("find sku by ids:{} fail, cause:{}", skuIds, skusResp.getError());
                    throw new JsonResponseException(skusResp.getError());
                }
                Map<Long, Sku> skuIndexById = Maps.uniqueIndex(skusResp.getResult(), Sku::getId);

                Response<List<Item>> itemsResp = itemReadService.findByIds(itemIds);
                if (!itemsResp.isSuccess()) {
                    log.error("find item by ids:{}, cause:{}", itemIds, itemsResp.getError());
                    throw new JsonResponseException(itemsResp.getError());
                }
                Map<Long, Item> itemIndexById = Maps.uniqueIndex(itemsResp.getResult(), Item::getId);

                for (int skuOrderLoop = 0; skuOrderLoop < skuOrders.size(); skuOrderLoop++) {
                    Row row = xssfSheet.createRow(skuOrderLoop + 1);
                    ExportHelper.setContent(row, getOrderContent(skuOrders.get(skuOrderLoop),
                            skuIndexById.get(skuOrders.get(skuOrderLoop).getSkuId()),
                            itemIndexById.get(skuOrders.get(skuOrderLoop).getItemId()).getName()));

                }
            }

            xssfWorkbook.write(outputStream);


        } catch (Exception e) {
            log.error("export order item info fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("download.order.item.excel.fail");

        }
    }

    private List<String> getOrderContent(SkuOrder skuOrder, Sku sku, String name) {
        List<String> contents = Lists.newArrayList();
        contents.add(Arguments.isNull(skuOrder.getOrderId()) ? "" : String.valueOf(skuOrder.getOrderId()));
        contents.add(Arguments.isNull(skuOrder.getBuyerName()) ? "" : skuOrder.getBuyerName());
        contents.add(DFT.print(new DateTime(skuOrder.getCreatedAt())));

        Map<String, String> extra = sku.getExtra();

        contents.add(Arguments.isNull(name) ? "" : name);
        contents.add(Arguments.isNull(extra.get("otherNo")) ? "" : extra.get("otherNo"));
        contents.add(Arguments.isNull(skuOrder.getQuantity()) ? "" : skuOrder.getQuantity().toString());
        contents.add(extra.get("brandCode") + extra.get("otherNo"));
        contents.add(Arguments.isNull(extra.get("brandCode")) ? "" : extra.get("brandCode"));
        contents.add(DFT.print(new DateTime(skuOrder.getCreatedAt())));


        return contents;
    }

    private void buildStockQuantityTemplateFile (OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("商品外部ID", 18 * 100);
            columnMaps.put("SKU外部ID", 18 * 400);
            columnMaps.put("数量", 18 * 100);
            XSSFSheet xssfSheet = xssfWorkbook.createSheet("库存管理表");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            Row row = xssfSheet.createRow(1);
            List<String> contents = Lists.newArrayList();
            contents.add("outer_item_id");
            contents.add("outer_sku_id");
            contents.add("stock_quantity");

            ExportHelper.setContent(row, contents);

            xssfWorkbook.write(outputStream);


        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }

    }

    /**
     * 供应商价格批量修改模板数据封装
     *
     * @param outputStream
     */
    private void buildSellerPricTemplateFile (OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("商品编号", 18 * 150);
            columnMaps.put("商品名称", 18 * 800);
            columnMaps.put("SKU编号", 18 * 150);
            columnMaps.put("商品规格", 18 * 800);
            columnMaps.put("现供货价(分)", 18 * 180);
            columnMaps.put("新供货价(精确到分)", 18 * 180);
            XSSFSheet xssfSheet = xssfWorkbook.createSheet("商品管理");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);
            Row row = xssfSheet.createRow(1);
            List<String> contents = Lists.newArrayList();
            contents.add("item_id");
            contents.add("item_name");
            contents.add("sku_id");
            contents.add("sku_name");
            contents.add("sku_price");
            contents.add("sku_new_price");
            ExportHelper.setContent(row, contents);


            final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
            Response<List<Sku>> skusResp = vegaSkuReadService.findFrozenItemsByShopId(shopId);
            if (!skusResp.isSuccess()) {
                log.error("fail to find frozen skus by shopId={},status={},cause:{}",
                        shopId, DefaultItemStatus.ITEM_FREEZE, skusResp.getError());
                throw new JsonResponseException("sku.find.fail");
            }
            List<Long> itemIds = Lists.transform(skusResp.getResult(), Sku::getItemId);
            Set<Long> linkedHashSet = new LinkedHashSet<>(itemIds);
            itemIds = new ArrayList<>(linkedHashSet);
            Response<List<Item>> itemsResp = itemReadService.findByIds(itemIds);
            if (!itemsResp.isSuccess()) {
                log.error("find item by ids:{}, cause:{}", itemIds, itemsResp.getError());
                throw new JsonResponseException(itemsResp.getError());
            }
            Map<Long, Item> itemIndexById = Maps.uniqueIndex(itemsResp.getResult(), Item::getId);
            int index = 0;
            for (Sku sku : skusResp.getResult()) {
                Row rowData = xssfSheet.createRow(index + 2);
                String attrs = "";
                List<SkuAttribute> skuAttributes = sku.getAttrs();
                if (!Arguments.isNullOrEmpty(skuAttributes)) {
                    //规格
                    for (SkuAttribute skuAttribute : skuAttributes) {
                        String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                        attrs += attr;
                    }
                }
                ExportHelper.setContent(rowData, ImmutableList.of(sku.getItemId().toString(),
                        itemIndexById.get(sku.getItemId()).getName(),
                        sku.getId().toString(),
                        attrs,
                        sku.getPrice().toString(),
                        ""));
                index++;
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("download.batch.update.seller.price.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }
}
