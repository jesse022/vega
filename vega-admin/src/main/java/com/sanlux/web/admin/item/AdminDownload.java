package com.sanlux.web.admin.item;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.DateHelper;
import com.sanlux.common.helper.VegaOrderStatusHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.trade.VegaOrderExportExcel;
import com.sanlux.web.front.core.util.ArithUtil;
import com.sanlux.web.front.core.utils.ExportHelper;
import com.sanlux.web.front.core.utils.ExportWordHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.OrderReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 运营订单详情导出到Excel功能
 * Created by lujm on 2017/2/14.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminDownload {
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private VegaCategoryAuthByShopIdCacherService vegaCategoryAuthByShopIdCacherService;

    @RpcConsumer
    private VegaCategoryByItemIdCacherService vegaCategoryByItemIdCacherService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @Autowired
    private VegaOrderExportExcel vegaOrderExportExcel;

    /**
     * 运营后台导出订单详情到excel
     * Created by lujm on 2017/02/14
     *
     * @param orderId  订单号
     * @param response http
     */
    @RequestMapping(value = "/download/{orderId}/order/excel", method = RequestMethod.GET)
    public void downLoadOrderDetailToExcel(@PathVariable("orderId") Long orderId,
                                           HttpServletResponse response) {
        try {
            Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderId);
            if (!vegaOrderDetailRS.isSuccess()) {
                log.error("find OrderDetail by orderId:{} fail, cause:{}",
                        orderId, vegaOrderDetailRS.getError());
                throw new JsonResponseException(vegaOrderDetailRS.getError());
            }
            String xlsFileName = URLEncoder.encode("订单详情", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            getOrderDetailTemplateFile(vegaOrderDetailRS.getResult(), response.getOutputStream());
        } catch (Exception e) {
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
    public void downLoadOrderDetailToExcel(@RequestParam(value = "orderIds", required = true) List<Long> orderIds,
                                           HttpServletResponse response) {
        try {
            String xlsFileName = URLEncoder.encode("订单详情", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            batchGetOrderDetailTemplateFile(orderIds, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, orderId:{} ", orderIds);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
    }

    /**
     * 发货单导出
     * @param orderIds 订单Ids
     * @param response response
     */
    @RequestMapping(value = "/download/order-shipment", method = RequestMethod.GET)
    public void ExportOrderShipmentInfo(@RequestParam(value = "orderIds", required = true) List<Long> orderIds,
                                           HttpServletResponse response) {
        try {
            String xlsFileName = URLEncoder.encode("集乘发货单"+ DateHelper.formatDate(new Date()), "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);

            Response<List<VegaOrderDetail>> vegaOrderDetailRS = vegaOrderReadService.findShopOrderAndReceiverInfoByOrderIds(orderIds);
            if (!vegaOrderDetailRS.isSuccess()) {
                log.error("find OrderDetail by orderId:{} fail, cause:{}",
                        orderIds, vegaOrderDetailRS.getError());
                throw new JsonResponseException(vegaOrderDetailRS.getError());
            }

            buildVegaOrderDetailTemplateFile(response.getOutputStream(), vegaOrderDetailRS.getResult());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, orderId:{} ", orderIds);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
    }


    /**
     * 导出服务商接单订单的成本价接口(临时使用)
     *
     * @param pageNo   起始值
     * @param pageSize 每页显示数量
     */
    @RequestMapping(value = "/export-shop-order/{shopId}", method = RequestMethod.GET)
    public void ExportShopOrderToExcel(HttpServletResponse httpServletResponse,
                                       @RequestParam(required = true) Integer pageNo,
                                       @RequestParam(required = true) Integer pageSize,
                                       @PathVariable("shopId") Long shopId) {
        try {
            List<ShopOrder> shopOrders = getFirstShopReceiveOrder(pageNo, pageSize, shopId);
            String xlsFileName = URLEncoder.encode("服务商接单订单信息", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildShopOrderTemplateFile(httpServletResponse.getOutputStream(), shopOrders);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.shop.order.to.excel.fail");
            throw new JsonResponseException("export.shop.order.to.excel.fail");
        }
    }


    // // TODO: 2018/1/17 待修改模板... 
    /**
     * 订单导出合同
     * @param orderId 订单号
     * @param httpServletResponse httpServletResponse
     */
    @RequestMapping(value = "/export-order-contract/{orderId}", method = RequestMethod.GET)
    public void ExportOrderContractToWord(@PathVariable("orderId") Long orderId,
                                          HttpServletResponse httpServletResponse) {
        try {
            String xlsFileName = URLEncoder.encode("订单合同", "UTF-8") + ".doc";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);

            Map<String, Object> dataMap = vegaOrderExportExcel.getOrderToContractDateMap(orderId);

            ExportWordHelper.createWord(dataMap, "order-contract-template.ftl", httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.shop.order.to.contract.fail, orderId:{}", orderId);
            throw new JsonResponseException("export.shop.order.to.contract.fail");
        }
    }



    /**
     * 组装订单详情到excel
     * Created by lujm on 2017/02/14
     *
     * @param vegaOrderDetail 订单号
     * @param outputStream    OutputStream
     */
    private void getOrderDetailTemplateFile(VegaOrderDetail vegaOrderDetail, OutputStream outputStream) {
        try {
            InputStream inStream = Resources.asByteSource(Resources.getResource("excel/order-detail-template.xlsx")).openStream();
            XSSFWorkbook Workbook = new XSSFWorkbook(inStream);
            XSSFSheet Sheet = Workbook.getSheetAt(1);
            Workbook.removeSheetAt(0);//删除订单汇总sheet

            /**  1.订单详情   */
            ShopOrder shopOrder = vegaOrderDetail.getShopOrder();
            buildOrderDetail(Sheet, shopOrder);

            /**  2.买家信息   */
            List<OrderReceiverInfo> orderReceiverInfos = vegaOrderDetail.getOrderReceiverInfos();
            buildBuyerInfo(Sheet, shopOrder, orderReceiverInfos);

            /**  3.SKU信息   */
            List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();
            buildSkuOrderInfo(Sheet, skuOrders);

            /**  4.发票信息   */
            List<Invoice> invoices = vegaOrderDetail.getInvoices();
            buildInvoices(Sheet, invoices);

            Workbook.write(outputStream);
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

            for (int i = 0; i < orderIds.size(); i++) {
                Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderIds.get(i));
                if (!vegaOrderDetailRS.isSuccess()) {
                    log.error("find OrderDetail by orderId:{} fail, cause:{}",
                            orderIds.get(i), vegaOrderDetailRS.getError());
                    throw new JsonResponseException(vegaOrderDetailRS.getError());
                }
                vegaOrderDetails.add(vegaOrderDetailRS.getResult());
            }

            //订单汇总
            setSheetOrderDetailCollect(Workbook, vegaOrderDetails);

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
                    VegaOrderDetail vegaOrderDetail0 = (VegaOrderDetail) o1;
                    VegaOrderDetail vegaOrderDetail1 = (VegaOrderDetail) o2;
                    int i = vegaOrderDetail0.getShopOrder().getCreatedAt().compareTo(vegaOrderDetail1.getShopOrder().getCreatedAt());
                    if (i == 0) {
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
            for (VegaOrderDetail vegaOrderDetail : vegaOrderDetails) {
                List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();

                if (index2 > 0) {
                    //复制的sheet
                    sheet = Workbook.cloneSheet(1);
                    Workbook.setSheetName(index2 + 1, "订单号(" + vegaOrderDetail.getShopOrder().getId() + ")");
                    //单个订单订单详情页组装
                    setSheetOrderDetail(sheet, vegaOrderDetail);
                }

                for (SkuOrder skuOrder : skuOrders) {
                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 6, Arguments.isNull(skuOrder.getItemName()) ? "" :
                            skuOrder.getItemName());//产品名称

                    String attrs = "";
                    List<SkuAttribute> skuAttributes = skuOrder.getSkuAttrs();
                    if (!CollectionUtils.isEmpty(skuAttributes)) {
                        for (SkuAttribute skuAttribute : skuAttributes) {
                            String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                            attrs += attr;
                        }
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 7, attrs);//规格
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

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 13, VegaOrderStatusHelper.getOrderStatusName(skuOrder.getStatus()));//交易状态

                    index1 ++;
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
                        if (Objects.equals(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetail.getShopOrder().getCreatedAt())),
                                DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt())))) {
                            //日期和前一行相同
                            rowEnd = rowEnd + vegaOrderDetail.getSkuOrders().size();
                        } else {
                            //日期和前一行不相同
                            ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, Arguments.isNull(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt()) ? "" :
                                    DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt())));//订单日期
                            rowStart = rowEnd + 1;
                            rowEnd = rowStart + vegaOrderDetail.getSkuOrders().size() - 1;
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

                String operationNote = "";
                if (!Arguments.isNull(vegaOrderDetail.getShopOrder().getExtra()) && vegaOrderDetail.getShopOrder().getExtra().containsKey(SystemConstant.OPERATION_NOTE)) {
                    operationNote = vegaOrderDetail.getShopOrder().getExtra().get(SystemConstant.OPERATION_NOTE);
                }
                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 4, 4, operationNote);//订单备注

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 5, 5, Arguments.isNull(vegaOrderDetail.getShopOrder().getBuyerName()) ? "" :
                        vegaOrderDetail.getShopOrder().getBuyerName());//买家

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 14, 14, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getShipFee()));//运费

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 15, 15, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getDiffFee()));//改价

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 16, 16, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getFee()));//实付款(订单)


                index0 = index0 + skuOrders.size();
                index1 = 0;//重新赋值
                index2++;
            }
            //最后进行模板页订单详情组装
            if (vegaOrderDetails.size() > 0) {
                Workbook.setSheetName(1, "订单号(" + vegaOrderDetails.get(0).getShopOrder().getId() + ")");
                setSheetOrderDetail(sourceSheet, vegaOrderDetails.get(0));
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
            buildSkuOrderInfo(sheet, skuOrders);

            /**  4.发票信息   */
            List<Invoice> invoices = vegaOrderDetail.getInvoices();
            buildInvoices(sheet, invoices);

        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 订单详情组装
     * Created by lujm on 2017/02/14
     *
     * @param Sheet     Sheet
     * @param shopOrder shopOrder
     */
    private void buildOrderDetail(XSSFSheet Sheet, ShopOrder shopOrder) {
        try {
            ExportHelper.setContentByRowAndColumn(Sheet, 2, 2, Arguments.isNull(shopOrder.getId()) ? "" :
                    shopOrder.getId().toString());//订单号
            if (!Arguments.isNull(shopOrder.getExtra()) && shopOrder.getExtra().containsKey(SystemConstant.OPERATION_NOTE)) {
                ExportHelper.setContentByRowAndColumn(Sheet, 2, 4, Arguments.isNull(shopOrder.getExtra().get(SystemConstant.OPERATION_NOTE)) ? "" :
                        shopOrder.getExtra().get(SystemConstant.OPERATION_NOTE));//订单备注
            } else {
                ExportHelper.setContentByRowAndColumn(Sheet, 2, 4, "");//订单备注
            }

            ExportHelper.setContentByRowAndColumn(Sheet, 2, 9, Arguments.isNull(shopOrder.getCreatedAt()) ? "" :
                    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime(shopOrder.getCreatedAt())));//创建时间

            ExportHelper.setContentByRowAndColumn(Sheet, 8, 2, NumberUtils.formatPrice(shopOrder.getShipFee()));//运费

            ExportHelper.setContentByRowAndColumn(Sheet, 9, 2, NumberUtils.formatPrice(shopOrder.getDiffFee()));//改价

        } catch (Exception e) {
            log.error("build order detail info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 买家信息组装
     * Created by lujm on 2017/02/14
     *
     * @param Sheet              Sheet
     * @param shopOrder          shopOrder
     * @param orderReceiverInfos orderReceiverInfos
     */
    private void buildBuyerInfo(XSSFSheet Sheet, ShopOrder shopOrder, List<OrderReceiverInfo> orderReceiverInfos) {
        try {
            String info = "收货人地址:";
            String receiveAddress = "";
            if (!CollectionUtils.isEmpty(orderReceiverInfos)) {
                receiveAddress += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getProvince()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getProvince());//省份
                receiveAddress += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getCity()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getCity());//城市
                receiveAddress += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getRegion()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getRegion());//区县
                receiveAddress += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getDetail()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getDetail());//详细地址

                info += "    " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName());//收货人姓名
                info += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getMobile()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getMobile());//收货人电话
                info += "   " + receiveAddress;//收货人地址
                info += "   " + (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getPostcode()) ? "" :
                        orderReceiverInfos.get(0).getReceiverInfo().getPostcode());//收货人邮编
            }
            info += "\r\n";//换行
            String buyerNote = Arguments.isNull(shopOrder.getBuyerNote()) ? "" : shopOrder.getBuyerNote();
            info += "买家留言: " + (Arguments.isEmpty(buyerNote) ? "无" : buyerNote);

            ExportHelper.setContentByRowAndColumn(Sheet, 4, 2, info);
        } catch (Exception e) {
            log.error("build buyer info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * SkuOrder信息组装
     * Created by lujm on 2017/02/14
     *
     * @param Sheet     Sheet
     * @param skuOrders SkuOrder
     */
    private void buildSkuOrderInfo(XSSFSheet Sheet, List<SkuOrder> skuOrders) {
        try {
            if (skuOrders != null && skuOrders.size() > 0) {
                for (int i = 0; i < skuOrders.size(); i++) {
                    SkuOrder skuOrder = skuOrders.get(i);

                    //根据shopId skuId获取商品原价
                    Long skuId = Arguments.isNull(skuOrder.getSkuId()) ? -1 : skuOrder.getSkuId();
                    //取平台店铺的销售价
                    Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId((long) VegaShopType.PLATFORM.value(), skuId);
                    if (!findShopSku.isSuccess()) {
                        log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                                VegaShopType.PLATFORM.value(), skuId, findShopSku.getError());
                        throw new ServiceException(findShopSku.getError());
                    }

                    Map<String, String> tags = skuOrder.getTags();
                    String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 4, NumberUtils.formatPrice(Long.valueOf(orderSkuSellerPrice)));//供货价


                    if (findShopSku.getResult().isPresent()) {
                        //产品原价
                        double itemPrice = findShopSku.getResult().get().getPrice() / 100.00;//分转化为(元)
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 5, df1.format(itemPrice));//产品原价
                    }

                    //根据itemId获取商品"单位"
                    Long itemId = Arguments.isNull(skuOrder.getItemId()) ? -1 : skuOrder.getItemId();
                    Response<Item> itemRes = itemReadService.findById(itemId);
                    if (!itemRes.isSuccess()) {
                        log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
                        throw new JsonResponseException(itemRes.getError());
                    }
                    Map<String, String> extraMap = itemRes.getResult().getExtra();
                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 8, Arguments.isNull(extraMap.get("unit")) ? "" :
                            extraMap.get("unit"));//单位

                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 1, Arguments.isNull(skuOrder.getItemName()) ? "" :
                            skuOrder.getItemName());//产品名称
                    String attrs = "";
                    List<SkuAttribute> skuAttributes = skuOrder.getSkuAttrs();
                    if (!CollectionUtils.isEmpty(skuAttributes)) {
                        //规格
                        for (SkuAttribute skuAttribute : skuAttributes) {
                            String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                            attrs += attr;
                        }
                        ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 2, attrs);
                    }
                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 3, Arguments.isNull(skuOrder.getSkuId()) ? "" :
                            skuOrder.getSkuId().toString());//SKUID
                    if (!Arguments.isNull(skuOrder.getOriginFee()) && !Arguments.isNull(skuOrder.getQuantity())) {
                        //单价
                        double price = (skuOrder.getOriginFee() / skuOrder.getQuantity()) / 100.00;//分转化为(元)
                        DecimalFormat df1 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 6, df1.format(price));
                    }
                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 7, Arguments.isNull(skuOrder.getQuantity()) ? "" :
                            skuOrder.getQuantity().toString());//数量
                    ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 9, Arguments.isNull(skuOrder.getDiscount()) ? "0.00" :
                            skuOrder.getDiscount().toString());//优惠
                    if (!Arguments.isNull(skuOrder.getFee())) {
                        //实付款
                        DecimalFormat df2 = new DecimalFormat("###.00");//保留2位小数
                        ExportHelper.setContentByRowAndColumn(Sheet, i + 11, 10, df2.format(skuOrder.getFee() / 100.00));
                    }
                    //"备注栏"暂不填充,预留以后扩展
                }
            }
        } catch (Exception e) {
            log.error("build sku order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 发票信息组装
     * Created by lujm on 2017/02/14
     *
     * @param Sheet    Sheet
     * @param invoices invoices
     */
    private void buildInvoices(XSSFSheet Sheet, List<Invoice> invoices) {
        try {
            if (!CollectionUtils.isEmpty(invoices)) {
                String invoiceInfo = "";
                for (Invoice invoice : invoices) {
                    Map<String, String> invoiceTypeMap = invoice.getDetail();
                    String invoiceType = Arguments.isNull(invoiceTypeMap.get("type")) ? "" : invoiceTypeMap.get("type");//普通发票&增值发票
                    String titleType = Arguments.isNull(invoiceTypeMap.get("titleType")) ? "" : invoiceTypeMap.get("titleType");//个人&公司
                    if (invoiceType.equals("1")) {
                        //普通发票
                        invoiceInfo = "发票类型: 普通发票" + "\r\n";
                        invoiceInfo += "发票抬头: " + invoice.getTitle() + "\r\n";
                        if (Objects.equals(titleType, String.valueOf(2))) {
                            if (!Arguments.isNull(invoiceTypeMap.get("taxIdentityNo"))) {
                                invoiceInfo += "企业税号: " + invoiceTypeMap.get("taxIdentityNo") + "\r\n";
                            }
                            invoiceInfo += "内容: 公司";
                        } else {
                            invoiceInfo += "内容: 个人";
                        }
                    } else {
                        //增值发票
                        invoiceInfo = "发票类型: 增值发票" + "\r\n";
                        invoiceInfo += "发票抬头: " + invoice.getTitle() + "\r\n";
                        invoiceInfo += "公司名称: " + (Arguments.isNull(invoiceTypeMap.get("companyName")) ? "" : invoiceTypeMap.get("companyName")) + "\r\n";
                        invoiceInfo += "企业税号: " + (Arguments.isNull(invoiceTypeMap.get("taxRegisterNo")) ? "" : invoiceTypeMap.get("taxRegisterNo")) + "\r\n";
                        invoiceInfo += "注册地址: " + (Arguments.isNull(invoiceTypeMap.get("registerAddress")) ? "" : invoiceTypeMap.get("registerAddress")) + "\r\n";
                        invoiceInfo += "注册电话: " + (Arguments.isNull(invoiceTypeMap.get("registerPhone")) ? "" : invoiceTypeMap.get("registerPhone")) + "\r\n";
                        invoiceInfo += "开户行: " + (Arguments.isNull(invoiceTypeMap.get("registerBank")) ? "" : invoiceTypeMap.get("registerBank")) + "\r\n";
                        invoiceInfo += "开户账号: " + (Arguments.isNull(invoiceTypeMap.get("bankAccount")) ? "" : invoiceTypeMap.get("bankAccount"));
                    }
                }
                ExportHelper.setContentByRowAndColumn(Sheet, 6, 2, invoiceInfo);
            }
        } catch (Exception e) {
            log.error("build order detail info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    /**
     * 获取服务商接单的订单信息(用于服务商导出订单成本价)
     *
     * @param pageNo   起始值
     * @param pageSize 每页显示数量
     * @param shopId   店铺Id
     * @return 店铺订单
     */
    private List<ShopOrder> getFirstShopReceiveOrder(Integer pageNo, Integer pageSize, Long shopId) {
        Map<String, Object> orderCriteria = Maps.newHashMap();
        orderCriteria.put("shopId", shopId);
        //只导出"已发货","已确认收货"状态的订单
        orderCriteria.put("status", ImmutableList.of(
                VegaOrderStatus.SHIPPED.getValue(),
                VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(), //已付款待一级审核
                VegaOrderStatus.CONFIRMED.getValue()));

        Response<Paging<ShopOrder>> response = vegaOrderReadService.pagingShopOrder(pageNo, pageSize, orderCriteria);
        if (!response.isSuccess()) {
            log.error("paging shop order fail,criteria:{},error:{}", orderCriteria, response.getError());
            throw new JsonResponseException("paging.shop.order.fail");
        }
        List<ShopOrder> shopOrders = response.getResult().getData();
        shopOrders.forEach(shopOrder -> {
            Integer sellerPrice = 0; // 服务商成本价
            Response<List<SkuOrder>> skuOrderResp = skuOrderReadService.findByShopOrderId(shopOrder.getId());
            if (!skuOrderResp.isSuccess()) {
                log.error("fail to find skuOrders by shopOrderId:{},cause:{}",
                        shopOrder.getId(), skuOrderResp.getError());
                throw new JsonResponseException("export.shop.order.to.excel.fail");
            }
            for (SkuOrder skuOrder : skuOrderResp.getResult()) {
                sellerPrice = sellerPrice + findFirstShopSkuPrice(skuOrder.getSkuId()).getResult() * skuOrder.getQuantity();
            }
            shopOrder.setOriginFee(sellerPrice.longValue()); // 塞入服务商成本价
        });
        return shopOrders;
    }

    /**
     * 根据skuId获取一级经销商折扣价
     *
     * @param skuId skuId
     * @return 一级折扣价
     */
    public Response<Integer> findFirstShopSkuPrice(Long skuId) {
        try {
            Sku sku = findSkuById(skuId);
            ShopSku shopSku = findShopSkuByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, skuId);
            if (Arguments.isNull(shopSku)) {
                return Response.fail("platform.shop.sku.not.set");
            }

            Integer shopSkuPrice = shopSku.getPrice();
            Float discount = findFirstShopDiscount(sku.getItemId());

            Integer price =
                    (int) Math.round(
                            ArithUtil.div(ArithUtil.mul(shopSkuPrice.doubleValue(), discount.doubleValue()),
                                    DefaultDiscount.COUNT_PRICE_DIVISOR.doubleValue())
                    );
            return Response.ok(price > shopSkuPrice ? shopSkuPrice : price);
        } catch (Exception e) {
            log.error("find sku price fail,skuId:{}, cause:{}", skuId, e.getMessage());
            return Response.fail(e.getMessage());
        }

    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
    }

    private ShopSku findShopSkuByShopIdAndSkuId(Long shopId, Long skuId) {
        Response<Optional<ShopSku>> shopSkuResponse = shopSkuReadService.findByShopIdAndSkuId(shopId, skuId);
        if (!shopSkuResponse.isSuccess()) {
            log.error("find shop sku fail, shopId:{}, skuId:{}, cause:{}", shopId, skuId, shopSkuResponse.getError());
            throw new ServiceException(shopSkuResponse.getError());
        }
        if (shopSkuResponse.getResult().isPresent()) {
            return shopSkuResponse.getResult().get();
        }
        return null;
    }

    /**
     * 根据商品ID获取一级经销商折扣
     *
     * @param itemId 商品ID
     * @return 折扣值
     */
    private Float findFirstShopDiscount(Long itemId) {
        try {
            List<VegaCategoryDiscountDto> discountDtoList = findCategoryDiscountList(DefaultId.PLATFROM_SHOP_ID);
            Long categoryId = findCategoryIdsByItemId(itemId);
            Map<Long, VegaCategoryDiscountDto> discountsMap =
                    Maps.uniqueIndex(discountDtoList, VegaCategoryDiscountDto::getCategoryId);
            Float discount = discountsMap.get(categoryId).getCategoryDiscount();
            if (!Arguments.isNull(discount)) {
                return discount;
            }
            return DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
        } catch (Exception e) {
            log.error("find category discount for first shop fail, itemId:{}, cause:{}", itemId, e.getMessage());
            throw new ServiceException("find.category.discount.for.first.shop.fail");
        }
    }

    /**
     * 根据店铺获取类目折扣信息
     *
     * @param shopId 店铺Id
     * @return 类目折扣信息
     */
    private List<VegaCategoryDiscountDto> findCategoryDiscountList(Long shopId) {

        Response<Optional<CategoryAuthe>> categoryAuthResp = vegaCategoryAuthByShopIdCacherService.findByShopId(shopId);
        if (!categoryAuthResp.isSuccess()) {
            log.error("find category discount list fail, shopId:{}, cause:{}",
                    shopId, categoryAuthResp.getError());
            throw new JsonResponseException(categoryAuthResp.getError());
        }
        if (categoryAuthResp.getResult().isPresent() && Arguments.notNull(categoryAuthResp.getResult().get())
                && !CollectionUtils.isEmpty(categoryAuthResp.getResult().get().getDiscountList())) {
            return categoryAuthResp.getResult().get().getDiscountList();
        }
        return Collections.<VegaCategoryDiscountDto>emptyList();
    }

    /**
     * 根据商品Id获取类目信息
     *
     * @param itemId 商品Id
     * @return 类目Id
     */
    private Long findCategoryIdsByItemId(Long itemId) {
        Response<Long> categoryIdDtoResponse =
                vegaCategoryByItemIdCacherService.findByItemId(itemId);
        if (!categoryIdDtoResponse.isSuccess()) {
            log.error("find categoryIds by itemId fail, itemId:{}, cause:{}",
                    itemId, categoryIdDtoResponse.getError());
            throw new JsonResponseException(categoryIdDtoResponse.getError());
        }
        return categoryIdDtoResponse.getResult();
    }

    /**
     * 服务商接单订单信息导出封装
     *
     * @param outputStream outputStream
     * @param shopOrders   shopOrders
     */
    private void buildShopOrderTemplateFile(OutputStream outputStream, List<ShopOrder> shopOrders) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("订单号", 18 * 150);
            columnMaps.put("下单时间", 18 * 250);
            columnMaps.put("买家", 18 * 200);
            columnMaps.put("总价(元)", 18 * 250);
            columnMaps.put("成本价(元)", 18 * 250);

            XSSFSheet xssfSheet = xssfWorkbook.createSheet("订单信息");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            int index = 0;
            for (ShopOrder shopOrder : shopOrders) {
                Row rowData = xssfSheet.createRow(index + 1);


                ExportHelper.setContent(rowData, ImmutableList.of(shopOrder.getId().toString(),
                        Arguments.isNull(shopOrder.getCreatedAt()) ? "" :
                                DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(shopOrder.getCreatedAt())),
                        shopOrder.getBuyerName(),
                        NumberUtils.formatPrice(shopOrder.getFee()),
                        NumberUtils.formatPrice(shopOrder.getOriginFee())));
                index++;
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("build.shop.order.to.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 发货单下载信息组装
     * @param outputStream outputStream
     * @param vegaOrderDetails vegaOrderDetails
     */
    private void buildVegaOrderDetailTemplateFile (OutputStream outputStream, List<VegaOrderDetail> vegaOrderDetails) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("订单号", 18 * 150);
            columnMaps.put("下单时间", 18 * 200);
            columnMaps.put("收货人姓名", 18 * 200);
            columnMaps.put("收货人电话", 18 * 200);
            columnMaps.put("收货地址", 18 * 800);
            columnMaps.put("邮编", 18 * 200);
            columnMaps.put("备注", 18 * 500);

            XSSFSheet xssfSheet = xssfWorkbook.createSheet("集乘发货单");
            XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
            ExportHelper.setTitleAndColumnWidth(xssfSheet, ExportHelper.setCellStyle(cellStyle), 1, 35, columnMaps);


            int index = 0;
            for (VegaOrderDetail vegaOrderDetail : vegaOrderDetails) {
                Row rowData = xssfSheet.createRow(index + 1);
                ShopOrder  shopOrder = vegaOrderDetail.getShopOrder();
                List<OrderReceiverInfo> orderReceiverInfos = vegaOrderDetail.getOrderReceiverInfos();

                String receiveAddress = ""; //收货地址
                String receiveUserName = ""; //收货人姓名
                String receiveMobile = ""; //收货人手机号码
                String receivePostcode = ""; //收货人邮编
                String operationNode = ""; //备注
                if (!CollectionUtils.isEmpty(orderReceiverInfos)) {
                    receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getProvince()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getProvince());//省份
                    receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getCity()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getCity());//城市
                    receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getRegion()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getRegion());//区县
                    receiveAddress +=(Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getDetail()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getDetail());//详细地址

                    receiveUserName = (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getReceiveUserName());//收货人姓名
                    receiveMobile = (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getMobile()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getMobile());//收货人电话
                    receivePostcode = (Arguments.isNull(orderReceiverInfos.get(0).getReceiverInfo().getPostcode()) ? "" :
                            orderReceiverInfos.get(0).getReceiverInfo().getPostcode());//收货人邮编
                }

                if (!Arguments.isNull(shopOrder.getExtra()) && shopOrder.getExtra().containsKey(SystemConstant.OPERATION_NOTE)) {
                    operationNode = Arguments.isNull(shopOrder.getExtra().get(SystemConstant.OPERATION_NOTE)) ? "" :
                            shopOrder.getExtra().get(SystemConstant.OPERATION_NOTE);
                }


                ExportHelper.setContent(rowData, cellStyle, 25,
                        ImmutableList.of(
                                vegaOrderDetail.getShopOrder().getId().toString(),
                                Arguments.isNull(shopOrder.getCreatedAt()) ? "" :
                                        DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(shopOrder.getCreatedAt())),
                                receiveUserName,
                                receiveMobile,
                                receiveAddress,
                                receivePostcode,
                                operationNode
                        )
                );
                index++;
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("build.order.detail.to.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

}
