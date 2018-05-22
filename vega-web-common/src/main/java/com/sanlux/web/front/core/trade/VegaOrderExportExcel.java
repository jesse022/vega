package com.sanlux.web.front.core.trade;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.trade.dto.ContractOrderInfo;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.web.front.core.util.NumberToCN;
import com.sanlux.web.front.core.utils.ExportHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * 订单导出Excel处理类
 * <p/>
 * Created by lujm on 2017/6/1.
 */
@Component
@Slf4j
public class VegaOrderExportExcel {

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;
    @RpcConsumer
    private ItemReadService itemReadService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    /**
     * 根据订单号IDs批量组装订单详情到excel
     *
     * @param orderIds              订单IDs
     * @param serviceManagerUserMap 买家Id,业务经理键值对
     * @param outputStream          OutputStream
     */
    public void batchGetOrderDetailTemplateFile(List<Long> orderIds, Map<Long, ServiceManagerUser> serviceManagerUserMap, OutputStream outputStream) {
        try {
            List<VegaOrderDetail> vegaOrderDetails = Lists.newArrayList();
            for (Long orderId : orderIds) {
                Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderId);
                if (!vegaOrderDetailRS.isSuccess()) {
                    log.error("find OrderDetail by orderId:{} fail, cause:{}",
                            orderId, vegaOrderDetailRS.getError());
                    throw new JsonResponseException(vegaOrderDetailRS.getError());
                }
                vegaOrderDetails.add(vegaOrderDetailRS.getResult());
            }
            //订单汇总
            setSheetOrderDetailCollect(outputStream, vegaOrderDetails, serviceManagerUserMap);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 组装订单汇总信息到sheet,业务经理业绩(专属会员订单)汇总统计
     *
     * @param outputStream          OutputStream
     * @param vegaOrderDetails      订单信息集合
     * @param serviceManagerUserMap 买家Id,业务经理键值对
     */
    private void setSheetOrderDetailCollect(OutputStream outputStream, List<VegaOrderDetail> vegaOrderDetails, Map<Long, ServiceManagerUser> serviceManagerUserMap) {
        try {
            InputStream inStream = Resources.asByteSource(Resources.getResource("excel/order-collect-template.xlsx")).openStream();
            XSSFWorkbook Workbook = new XSSFWorkbook(inStream);

            XSSFSheet collectSheet = Workbook.getSheetAt(0);//汇总页模板
            //先按照商品订单日期排序,日期相同按照订单号排序
            Collections.sort(vegaOrderDetails, new Comparator() {
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
            for (VegaOrderDetail vegaOrderDetail : vegaOrderDetails) {
                List<SkuOrder> skuOrders = vegaOrderDetail.getSkuOrders();

                for (SkuOrder skuOrder : skuOrders) {
                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 6, getObjectToString(skuOrder.getItemName()));//产品名称

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
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 8, NumberUtils.formatPrice(findShopSku.getResult().get().getPrice()));//产品原价
                    }

                    if (!Arguments.isNull(skuOrder.getOriginFee()) && !Arguments.isNull(skuOrder.getQuantity())) {
                        ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 9, NumberUtils.formatPrice(skuOrder.getOriginFee() / skuOrder.getQuantity()));//产品单价
                    }

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 10, getObjectToString(skuOrder.getQuantity()));//数量


                    //根据itemId获取商品"单位"
                    Long itemId = Arguments.isNull(skuOrder.getItemId()) ? -1 : skuOrder.getItemId();
                    Response<Item> itemRes = itemReadService.findById(itemId);
                    if (!itemRes.isSuccess()) {
                        log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
                        throw new JsonResponseException(itemRes.getError());
                    }
                    Map<String, String> extraMap = itemRes.getResult().getExtra();
                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 11, getObjectToString(extraMap.get("unit")));//单位

                    ExportHelper.setContentByRowAndColumn(collectSheet, 3 + index0 + index1, 12, NumberUtils.formatPrice(skuOrder.getFee()));//实付款

                    index1++;
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
                    ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, getDateToString(vegaOrderDetail.getShopOrder().getCreatedAt()));//订单日期

                } else {
                    if (index2 > 0) {
                        if (Objects.equals(getDateToString(vegaOrderDetail.getShopOrder().getCreatedAt()),
                                getDateToString(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt()))) {
                            //日期和前一行相同
                            rowEnd = rowEnd + vegaOrderDetail.getSkuOrders().size();
                        } else {
                            //日期和前一行不相同
                            ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, getDateToString(vegaOrderDetails.get(index2 - 1).getShopOrder().getCreatedAt()));//订单日期
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
                        ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, rowStart, rowEnd, 1, 1, getDateToString(vegaOrderDetail.getShopOrder().getCreatedAt()));//订单日期
                    }
                }

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 2, 2, getObjectToString(vegaOrderDetail.getShopOrder().getId()));//订单号

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 3, 3, getObjectToString(vegaOrderDetail.getShopOrder().getShopName()));//卖家

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 4, 4, getObjectToString(vegaOrderDetail.getShopOrder().getBuyerName()));//买家

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 5, 5, getObjectToString(serviceManagerUserMap.get(vegaOrderDetail.getShopOrder().getBuyerId()).getServiceManagerName()));//业务经理

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 13, 13, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getShipFee()));//运费

                ExportHelper.setContentAddMergedRegionByRowAndColumn(collectSheet, 3 + index0, 3 + index0 + index1 - 1, 14, 14, NumberUtils.formatPrice(vegaOrderDetail.getShopOrder().getFee()));//实付款(订单)

                index0 = index0 + skuOrders.size();
                index1 = 0;//重新赋值
                index2++;
            }
            Workbook.write(outputStream);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 订单导出合同信息组装
     *
     * @param orderId 订单Id
     * @return Map
     */
    public Map<String, Object> getOrderToContractDateMap(Long orderId) {
        Map<String, Object> dateMap = Maps.newConcurrentMap();
        List<ContractOrderInfo> contractOrderInfoList = Lists.newArrayList();
        Response<VegaOrderDetail> vegaOrderDetailRS = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderId);
        if (!vegaOrderDetailRS.isSuccess()) {
            log.error("find OrderDetail by orderId:{} fail, cause:{}",
                    orderId, vegaOrderDetailRS.getError());
            throw new JsonResponseException(vegaOrderDetailRS.getError());
        }
        VegaOrderDetail vegaOrderDetail = vegaOrderDetailRS.getResult();
        if (Arguments.isNull(vegaOrderDetail)) {
            return dateMap;
        }

        if (!Arguments.isNull(vegaOrderDetail.getSkuOrders())) {
            List<SkuOrder> skuOrders = vegaOrderDetailRS.getResult().getSkuOrders();
            Long sumPrice = getContractOrderInfoList(skuOrders, contractOrderInfoList);

            dateMap.put("orderList", contractOrderInfoList); // 订单详情
            dateMap.put("sumPrice", NumberUtils.formatPrice(sumPrice)); // 合计价格
            dateMap.put("capitalPrice", NumberToCN.number2Capital(Double.valueOf(NumberUtils.formatPrice(sumPrice)))); //金额大写
        }

        if (!Arguments.isNull(vegaOrderDetail.getShopOrder())) {
            dateMap.put("contractDate", DateTimeFormat.forPattern("yyyy年MM月dd日").print(
                    new DateTime(vegaOrderDetail.getShopOrder().getCreatedAt()))
            ); // 签合同日期,下单日期
        }

        if (!Arguments.isNull(vegaOrderDetail.getInvoices())) {
            List<Invoice> invoices = vegaOrderDetail.getInvoices();
            if (!CollectionUtils.isEmpty(invoices)) {
                String companyName = ""; //公司名称
                String taxRegisterNo = ""; //企业税号
                String registerPhone = ""; //注册电话
                String registerBank = ""; //开户行
                String bankAccount = ""; //开户账号
                String registerAddress = ""; //注册地址
                for (Invoice invoice : invoices) {
                    Map<String, String> invoiceTypeMap = invoice.getDetail();
                    String invoiceType = Arguments.isNull(invoiceTypeMap.get("type")) ? "" : invoiceTypeMap.get("type");//普通发票&增值发票
                    String titleType = Arguments.isNull(invoiceTypeMap.get("titleType")) ? "" : invoiceTypeMap.get("titleType");//个人&公司

                    if (invoiceType.equals("1")) {
                        //普通发票
                        companyName = invoice.getTitle();
                        if (Objects.equals(titleType, String.valueOf(2))) {
                            if (!Arguments.isNull(invoiceTypeMap.get("taxIdentityNo"))) {
                                taxRegisterNo = invoiceTypeMap.get("taxIdentityNo");
                            }
                        }
                    } else {
                        //增值发票
                        companyName = Arguments.isNull(invoiceTypeMap.get("companyName")) ? "" : invoiceTypeMap.get("companyName");
                        registerPhone = Arguments.isNull(invoiceTypeMap.get("registerPhone")) ? "" : invoiceTypeMap.get("registerPhone");
                        taxRegisterNo = Arguments.isNull(invoiceTypeMap.get("taxRegisterNo")) ? "" : invoiceTypeMap.get("taxRegisterNo");
                        registerAddress = Arguments.isNull(invoiceTypeMap.get("registerAddress")) ? "" : invoiceTypeMap.get("registerAddress");
                        registerBank = Arguments.isNull(invoiceTypeMap.get("registerBank")) ? "" : invoiceTypeMap.get("registerBank");
                        bankAccount = Arguments.isNull(invoiceTypeMap.get("bankAccount")) ? "" : invoiceTypeMap.get("bankAccount");
                    }
                }

                dateMap.put("companyName", companyName); // 公司名称
                dateMap.put("taxRegisterNo", taxRegisterNo); // 企业税号
                dateMap.put("registerPhone", registerPhone); // 注册电话
                dateMap.put("registerBank", registerBank); // 开户行
                dateMap.put("bankAccount", bankAccount); // 开户账号
                dateMap.put("registerAddress", registerAddress); //注册地址
            }
        }

        return dateMap;
    }

    /**
     * 获取合同订单详情
     *
     * @param skuOrders             skuOrders
     * @param contractOrderInfoList 订单详情list
     * @return 订单合计价格
     */
    private Long getContractOrderInfoList(List<SkuOrder> skuOrders, List<ContractOrderInfo> contractOrderInfoList) {
        Long sumPrice = 0L;
        for (int i = 0; i < skuOrders.size(); i++) {
            SkuOrder skuOrder = skuOrders.get(i);
            ContractOrderInfo contractOrderInfo = new ContractOrderInfo();

            String attrs = "";
            List<SkuAttribute> skuAttributes = skuOrder.getSkuAttrs();
            if (!CollectionUtils.isEmpty(skuAttributes)) {
                //规格
                for (SkuAttribute skuAttribute : skuAttributes) {
                    String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                    attrs += attr;
                }
            }
            Long itemId = Arguments.isNull(skuOrder.getItemId()) ? -1 : skuOrder.getItemId();
            Response<Item> itemRes = itemReadService.findById(itemId);
            if (!itemRes.isSuccess()) {
                log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
                throw new JsonResponseException(itemRes.getError());
            }
            Map<String, String> extraMap = itemRes.getResult().getExtra();


            contractOrderInfo.setItemName(Arguments.isNull(skuOrder.getItemName()) ? "" : skuOrder.getItemName());// 名称
            contractOrderInfo.setItemModel(attrs); // 规格
            contractOrderInfo.setItemUnit(Arguments.isNull(extraMap.get("unit")) ? "" : extraMap.get("unit")); // 单位
            contractOrderInfo.setItemQuantity(Arguments.isNull(skuOrder.getQuantity()) ? "" : skuOrder.getQuantity().toString()); // 数量


            if (!Arguments.isNull(skuOrder.getOriginFee()) && !Arguments.isNull(skuOrder.getQuantity())) {
                //单价
                Long price = (skuOrder.getOriginFee() / skuOrder.getQuantity());
                sumPrice += price;
                contractOrderInfo.setItemPrice(NumberUtils.formatPrice(price)); //单价
            }

            contractOrderInfoList.add(contractOrderInfo);
        }


        for (int i = 0; i < contractOrderInfoList.size(); i++) {
            if (i == 0) {
                contractOrderInfoList.get(i).setItemRemark("价格含17%增值税发票运费");
                contractOrderInfoList.get(i).setItemSumPrice(NumberUtils.formatPrice(sumPrice));
                contractOrderInfoList.get(i).setRemarkMerge("<w:vMerge w:val=\"restart\"/>");
                contractOrderInfoList.get(i).setSumPriceMerge("<w:vMerge w:val=\"restart\"/>");
            } else {
                contractOrderInfoList.get(i).setRemarkMerge("<w:vMerge/>");
                contractOrderInfoList.get(i).setSumPriceMerge("<w:vMerge/>");
            }
        }

        return sumPrice;
    }


    private String getObjectToString(Object object) {
        return Arguments.isNull(object) ? "" : object.toString();
    }

    private String getDateToString(Date date) {
        return Arguments.isNull(date) ? "" : DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date));
    }
}
