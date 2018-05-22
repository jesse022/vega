package com.sanlux.web.admin.report;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.trade.dto.*;
import com.sanlux.trade.enums.VegaOrderChannelEnum;
import com.sanlux.trade.model.VegaReportMonthSale;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.trade.service.VegaReportMonthSaleReadService;
import com.sanlux.trade.service.VegaReportMonthSaleWriteService;
import com.sanlux.web.front.core.report.VegaReportComponent;
import com.sanlux.web.front.core.utils.ExportHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 运营报表control类
 *
 * Created by lujm on 2017/9/21.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/vega/sale/report")
public class VegaOperatingReport {
    @RpcConsumer
    VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    VegaReportMonthSaleReadService vegaReportMonthSaleReadService;
    @RpcConsumer
    VegaReportMonthSaleWriteService vegaReportMonthSaleWriteService;
    @Autowired
    VegaReportComponent vegaReportComponent;


    /**
     * 手工生成(重新生成)销售报表数据接口
     * @param year   年
     * @param month  月
     * @param isUpdate 是否重新生成
     * @return 是否成功
     */
    @RequestMapping(value = "", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> addOrUpdateSalesReport(@RequestParam(value = "year") Integer year,
                                                   @RequestParam(value = "month") Integer month,
                                                   @RequestParam(value = "isUpdate" ,defaultValue = "0") Boolean isUpdate) {
        VegaReportMonthSale isExist = getSalesReportByYearAndMonth(year, month);
        if (!isUpdate) {
            if (Arguments.notNull(isExist)) {
                log.error("create.month.sale.report.fail,because year: {} month: {} report is exist", year, month);
                throw new JsonResponseException("month.sale.report.exist");
            }
        } else {
            if (Arguments.isNull(isExist)) {
                log.error("update.month.sale.report.fail,because year: {} month: {} report is not exist", year, month);
                throw new JsonResponseException("month.sale.report.not.exist");
            }
        }

        List<ReportSalesDto> reportSales = getSalesReportListByDate(getBeginTime(year, month), getEndTime(year, month));
        if (Arguments.isNullOrEmpty(reportSales)) {
            return Response.ok(Boolean.FALSE);
        }
        ReportSalesDto reportSalesDto = getSalesReporSum(reportSales);
        VegaReportMonthSale vegaReportMonthSale = new VegaReportMonthSale();

        vegaReportMonthSale.setYear(year);
        vegaReportMonthSale.setMonth(month);
        vegaReportMonthSale.setStatus(Boolean.TRUE);
        vegaReportMonthSale.setOrderCount(reportSalesDto.getOrderSum());
        vegaReportMonthSale.setOrderFee(reportSalesDto.getSalesVolumeSum());
        vegaReportMonthSale.setOrderMember(reportSalesDto.getMemberSum());
        vegaReportMonthSale.setCategorys(reportSalesDto.getReportCategoryDtos());
        vegaReportMonthSale.setVisitors(reportSalesDto.getReportTerminalDtos());

        if (!isUpdate) {
            Response<Boolean> rspCreate = vegaReportMonthSaleWriteService.create(vegaReportMonthSale);
            if (!rspCreate.isSuccess()) {
                log.error("failed to create month sale report, year={}, month={}, cause:{}", year, month, rspCreate.getError());
                throw new JsonResponseException("create.month.sale.report.fail");
            }
            return Response.ok(rspCreate.getResult());
        }

        Response<Boolean> rspUpdate = vegaReportMonthSaleWriteService.updateByYearAndMonth(vegaReportMonthSale);
        if (!rspUpdate.isSuccess()) {
            log.error("failed to update month sale report by year={}, month={}, cause:{}", year, month, rspUpdate.getError());
            throw new JsonResponseException("update.month.sale.report.fail");
        }
        return Response.ok(rspUpdate.getResult());
    }

    /**
     * 根据年度获取销售报表数据接口
     * @param year 年度
     * @return
     */
    @RequestMapping(value = "/find-by-year", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<ReportSalesDto>> findSalesReportByYearAndMonth(@RequestParam(value = "year", required = false) Integer year) {
        if (Arguments.isNull(year)) {
            year = DateTime.now().year().get();
        }
        Response<List<VegaReportMonthSale>> rspReport = vegaReportMonthSaleReadService.findByYear(year);
        if (!rspReport.isSuccess()) {
            log.error("failed to find month sale report by year={}, cause:{}", year, rspReport.getError());
            throw new JsonResponseException("month.sale.report.find.fail");
        }
        Map<Integer, VegaReportMonthSale> reportMonthSaleMap = Maps.uniqueIndex(rspReport.getResult(), VegaReportMonthSale::getMonth);

        List<ReportSalesDto> reportMonthSaleList = Lists.newArrayListWithCapacity(11);
        for (int i = 1; i<=12; i++) {
            ReportSalesDto reportSalesDto = new ReportSalesDto();
            reportSalesDto.setYear(year);
            reportSalesDto.setMonth(i);
            reportSalesDto.setStatus(Boolean.FALSE);
            reportMonthSaleList.add(reportSalesDto);
        }
        final Long[] totalOrderFee = {0L};
        reportMonthSaleList.stream().forEach(reportSalesDto -> {
            if (!Arguments.isNull(reportMonthSaleMap.get(reportSalesDto.getMonth()))) {
                totalOrderFee[0] = totalOrderFee[0] + reportMonthSaleMap.get(reportSalesDto.getMonth()).getOrderFee();
                reportSalesDto.setSummaryDate(reportMonthSaleMap.get(reportSalesDto.getMonth()).getId().toString());//主键Id放入汇总时间字段
                reportSalesDto.setStatus(Boolean.TRUE);
                reportSalesDto.setOrderSum(reportMonthSaleMap.get(reportSalesDto.getMonth()).getOrderCount());
                reportSalesDto.setSalesVolumeSum(reportMonthSaleMap.get(reportSalesDto.getMonth()).getOrderFee());
                reportSalesDto.setTotalOrderFee(totalOrderFee[0]);
            }
        });

        return Response.ok(reportMonthSaleList);
    }

    /**
     * 按月导出销售报表数据(根据选中月份id)
     * @return
     */
    @RequestMapping(value = "/export-by-month", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportSalesReportWithMonth(HttpServletResponse httpServletResponse,
                                           @RequestParam(value = "ids", required = true) List<Long> ids) {
        try {
            List<VegaReportMonthSale> ReportMonthSales = getSalesReportByIds(ids);
            List<ReportSalesDto> reportSales = Lists.newArrayList();
            ReportMonthSales.stream().forEach(reportMonthSale -> {
                ReportSalesDto reportSalesDto = new ReportSalesDto();
                reportSalesDto.setSummaryDate(reportMonthSale.getMonth()+"月份");
                reportSalesDto.setYear(reportMonthSale.getYear());
                reportSalesDto.setMonth(reportMonthSale.getMonth());
                reportSalesDto.setOrderSum(reportMonthSale.getOrderCount());
                reportSalesDto.setSalesVolumeSum(reportMonthSale.getOrderFee());
                reportSalesDto.setMemberSum(reportMonthSale.getOrderMember());
                reportSalesDto.setReportCategoryDtos(reportMonthSale.getCategorys());
                reportSalesDto.setReportTerminalDtos(reportMonthSale.getVisitors());
                reportSales.add(reportSalesDto);
            });

            String xlsFileName = URLEncoder.encode("集乘平台销售情况汇报", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);

            buildMonthSalesReportTemplateFile(httpServletResponse.getOutputStream(), reportSales);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.month.sale.report.to.excel.fail");
            throw new JsonResponseException("export.month.sale.report.to.excel.fail");
        }
    }

    /**
     * 按日导出报表数据(截止当前时间5天的数据)
     * @return
     */
    @RequestMapping(value = "/export-by-day", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportSalesReportWithDay(HttpServletResponse httpServletResponse) {
        try {
            Date startAt = DateTime.now().plusDays(-4).withTimeAtStartOfDay().toDate();
            Date endAt = DateTime.now().plusDays(1).withTimeAtStartOfDay().toDate();

            List<ReportSalesDto> reportSales = getSalesReportListByDate(startAt, endAt);
            reportSales = getSalesReportListByGroup(reportSales);


            String xlsFileName = URLEncoder.encode("集乘平台销售情况汇报", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildMonthSalesReportTemplateFile(httpServletResponse.getOutputStream(), reportSales);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.month.sale.report.to.excel.fail");
            throw new JsonResponseException("export.month.sale.report.to.excel.fail");
        }
    }

    /**
     * 按日导出服务商销售报表数据
     * @return
     */
    @RequestMapping(value = "/export-by-dealer", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportDealerSalesReportWithDay(@RequestParam(value = "startAt") String startAt,
                                               @RequestParam(value = "endAt") String endAt,
                                               @RequestParam(value = "shopIds") List<Long> shopIds,
                                               HttpServletResponse httpServletResponse) {
        try {
            List<ReportDealerSalesDto> reportDealerSalesDtoList = getDealerSalesReportListByDate(startDate(startAt), endDate(endAt), shopIds);
            reportDealerSalesDtoList = getDealerSalesReportListByGroup(reportDealerSalesDtoList);

            String xlsFileName = URLEncoder.encode("集乘平台每日销售情况汇报", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);

            buildDealerSalesReportTemplateFile(httpServletResponse.getOutputStream(), reportDealerSalesDtoList);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.month.sale.report.to.excel.fail");
            throw new JsonResponseException("export.month.sale.report.to.excel.fail");
        }
    }


    /**
     * 销售报表数据组装
     * @param outputStream outputStream
     * @param reportSalesDtos 报表数据
     */
    private void buildMonthSalesReportTemplateFile(OutputStream outputStream, List<ReportSalesDto> reportSalesDtos) {
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            VegaReportMonthSale categoryRelationship = getSalesReportByYearAndMonth(0, 0); //获取报表类目和实际后台类目对应关系,对应关系手工维护
            if (Arguments.isNull(categoryRelationship) || Arguments.isNull(categoryRelationship.getExtra())) {
                return;
            }

            columnMaps.put("时间", 18 * 160);
            columnMaps.put("成交订单数", 18 * 150);
            columnMaps.put("电脑端", 18 * 150);
            columnMaps.put("移动端", 18 * 150);
            columnMaps.put("微信", 18 * 150);
            // 动态设置类目标题
            Map<String, String> extraMap = categoryRelationship.getExtra();

            for (String key : extraMap.keySet()) {
                columnMaps.put(key, 18 * 200);
            }
            columnMaps.put("累计总金额", 18 * 200);
            columnMaps.put("会员数", 18 * 200);


            XSSFSheet xssfSheet = xssfWorkbook.createSheet("集乘平台销售情况汇报");
            XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
            Row row2 = xssfSheet.createRow(1);
            List<String> row2List = Lists.newArrayList("时间","成交订单数","访客数","","");

            row2List.addAll(extraMap.keySet().stream().collect(Collectors.toList()));
            row2List.add("累计总金额");
            row2List.add("会员数");


            ExportHelper.setTitleAndColumnWidth(xssfSheet, ExportHelper.setCellStyle(cellStyle), 3, 25, columnMaps);// 第三行
            ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, cellStyle, 50, 1, 1, 1, 7 + extraMap.size() , "集乘平台销售情况汇报"); //第一行
            ExportHelper.setContent(row2, cellStyle, 25, row2List);

            //合并标题行
            xssfSheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
            xssfSheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1));
            xssfSheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 4));

            for (int i=0;i< extraMap.keySet().size() + 2;i++) {
                xssfSheet.addMergedRegion(new CellRangeAddress(1, 2, 5 + i, 5 + i));
            }


            if (!Arguments.isNull(reportSalesDtos)) {
                int index = 0;
                //合计行初始化
                Map<String, Long> sumMap = Maps.newHashMap();
                sumMap.put("orderSum", 0L);
                sumMap.put("terminalPC", 0L);
                sumMap.put("terminalMobile", 0L);
                sumMap.put("terminalWeChat", 0L);
                for (String key : extraMap.keySet()) {
                    sumMap.put(key, 0L);
                }
                sumMap.put("salesVolumeSum", 0L);
                sumMap.put("memberSum", 0L);


                for (ReportSalesDto reportSalesDto : reportSalesDtos) {
                    Row row = xssfSheet.createRow(3 + index);
                    Map<Integer, ReportTerminalDto> terminalMap = Maps.uniqueIndex(reportSalesDto.getReportTerminalDtos(), ReportTerminalDto::getTerminalId);

                    String orderSum = Arguments.isNull(reportSalesDto.getOrderSum()) ? "" : reportSalesDto.getOrderSum().toString();
                    String terminalPC = Arguments.isNull(terminalMap.get(VegaOrderChannelEnum.PC.value())) ? "" :
                            terminalMap.get(VegaOrderChannelEnum.PC.value()).getSum().toString();
                    String terminalMobile = Arguments.isNull(terminalMap.get(VegaOrderChannelEnum.MOBILE.value())) ? "" :
                            terminalMap.get(VegaOrderChannelEnum.MOBILE.value()).getSum().toString();
                    String terminalWeChat = Arguments.isNull(terminalMap.get(VegaOrderChannelEnum.WE_CHAT.value())) ? "" :
                            terminalMap.get(VegaOrderChannelEnum.WE_CHAT.value()).getSum().toString();
                    sumMap.put("orderSum", sumMap.get("orderSum") + Long.valueOf(orderSum));
                    sumMap.put("terminalPC", sumMap.get("terminalPC") + Long.valueOf(terminalPC));
                    sumMap.put("terminalMobile", sumMap.get("terminalMobile") + Long.valueOf(terminalMobile));
                    sumMap.put("terminalWeChat", sumMap.get("terminalWeChat") + Long.valueOf(terminalWeChat));


                    List<String> rowList = Lists.newArrayList(
                            Arguments.isNull(reportSalesDto.getSummaryDate()) ? "" : reportSalesDto.getSummaryDate(),
                            orderSum, terminalPC, terminalMobile, terminalWeChat);

                    //动态填充各类目数量
                    Map<Long, ReportCategoryDto> categoryMap = Maps.uniqueIndex(reportSalesDto.getReportCategoryDtos(), ReportCategoryDto::getCategoryId);
                    for (String key : extraMap.keySet()) {
                        String category = extraMap.get(key);
                        List<String> categoryList = Splitters.COMMA.splitToList(category);
                        Long totalCategoryFee = 0L;
                        for (String categoryId : categoryList) {
                            Long fee = Arguments.isNull(categoryMap.get(Long.valueOf(categoryId))) ? 0L : categoryMap.get(Long.valueOf(categoryId)).getCategoryFee();
                            totalCategoryFee += fee;
                        }
                        rowList.add(Objects.equal(totalCategoryFee, 0L) ? "" : NumberUtils.formatPrice(totalCategoryFee));

                        sumMap.put(key, sumMap.get(key) + totalCategoryFee);
                    }

                    rowList.add(NumberUtils.formatPrice(reportSalesDto.getSalesVolumeSum()));
                    rowList.add(Arguments.isNull(reportSalesDto.getMemberSum()) ? "" : reportSalesDto.getMemberSum() + "");

                    String salesVolumeSum = Arguments.isNull(reportSalesDto.getSalesVolumeSum()) ? "" : reportSalesDto.getSalesVolumeSum().toString();
                    String memberSum = Arguments.isNull(reportSalesDto.getMemberSum()) ? "" : reportSalesDto.getMemberSum().toString();
                    sumMap.put("salesVolumeSum", sumMap.get("salesVolumeSum") + Long.valueOf(salesVolumeSum));
                    sumMap.put("memberSum", sumMap.get("memberSum") + Long.valueOf(memberSum));


                    ExportHelper.setContent(row, cellStyle, 25, rowList);
                    index++;
                }
                Row sumRow = xssfSheet.createRow(3 + index);
                List<String> sumList = Lists.newArrayList("合计",
                        sumMap.get("orderSum").toString(),
                        sumMap.get("terminalPC").toString(),
                        sumMap.get("terminalMobile").toString(),
                        sumMap.get("terminalWeChat").toString());
                sumList.addAll(extraMap.keySet().stream().map(key -> NumberUtils.formatPrice(sumMap.get(key))).collect(Collectors.toList()));
                sumList.add(NumberUtils.formatPrice(sumMap.get("salesVolumeSum")));
                sumList.add(sumMap.get("memberSum").toString());
                ExportHelper.setContent(sumRow, cellStyle, 25, sumList);
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export.month.sale.report.to.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }


    /**
     * 服务商每日销售报表数据组装
     * @param outputStream outputStream
     * @param reportDealerSalesDtoList 报表数据
     */
    private void buildDealerSalesReportTemplateFile(OutputStream outputStream, List<ReportDealerSalesDto> reportDealerSalesDtoList) {
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();

            columnMaps.put("客户名称", 18 * 500);
            columnMaps.put("时间", 18 * 250);
            columnMaps.put("成交订单数", 18 * 200);
            columnMaps.put("累计总金额", 18 * 200);


            XSSFSheet xssfSheet = xssfWorkbook.createSheet("集乘平台每日销售情况汇报");
            XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();

            ExportHelper.setTitleAndColumnWidth(xssfSheet, ExportHelper.setCellStyle(cellStyle), 2, 25, columnMaps);// 第二行
            ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, cellStyle, 60, 1, 1, 1, 4 , "集乘平台每日销售情况汇报"); //第一行

            if (!Arguments.isNull(reportDealerSalesDtoList)) {
                //按照日期从大到小排序
                Collections.sort(reportDealerSalesDtoList, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        ReportDealerSalesDto r0=(ReportDealerSalesDto)o1;
                        ReportDealerSalesDto r1=(ReportDealerSalesDto)o2;
                        return r0.getSummaryDate().compareTo(r1.getSummaryDate());
                    }
                });
                Collections.reverse(reportDealerSalesDtoList);

                int rowStart = 2; //订单日期合并默认起始行
                int rowEnd = 2; //订单日期合并默认截止行
                int index = 0;
                for (ReportDealerSalesDto reportDealerSalesDto : reportDealerSalesDtoList) {
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 3 + index, 1, Arguments.isNull(reportDealerSalesDto.getShopName()) ? "" : reportDealerSalesDto.getShopName());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 3 + index, 3, Arguments.isNull(reportDealerSalesDto.getOrderSum()) ? "" : reportDealerSalesDto.getOrderSum().toString());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 3 + index, 4, (NumberUtils.formatPrice(reportDealerSalesDto.getSalesVolumeSum())));


                    /**
                     * 汇总日期合并处理逻辑:
                     *
                     * 1.只有一条记录时,直接对日期进行赋值
                     * 2.有多条记录,循环订单列表
                     *   1)第一条,修改截止行数位置
                     *   2)中间数据,取当前订单日期和上一个订单日期进行对比,如果相同就更新截止行数位置,不同时就对上一订单日期进行赋值,并更新起始和截止位置
                     *   3)最后一条时,直接对当前订单进行日期赋值
                     */
                    if (reportDealerSalesDtoList.size() == 1) {
                        //只有一条记录
                        rowStart = rowStart + 1;
                        rowEnd = rowEnd + 1;
                        ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, cellStyle, 25, rowStart, rowEnd, 2, 2,
                                Arguments.isNull(reportDealerSalesDto.getSummaryDate()) ? "" : reportDealerSalesDto.getSummaryDate());

                    } else {
                        if (index > 0) {
                            if (Objects.equal(reportDealerSalesDtoList.get(index).getSummaryDate(), reportDealerSalesDtoList.get(index - 1).getSummaryDate())) {
                                //日期和前一行相同
                                rowEnd = rowEnd + 1;
                            } else {
                                //日期和前一行不相同
                                ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, cellStyle, 25, rowStart, rowEnd, 2, 2,
                                        Arguments.isNull(reportDealerSalesDtoList.get(index - 1).getSummaryDate()) ? "" : reportDealerSalesDtoList.get(index - 1).getSummaryDate());
                                rowStart = rowEnd + 1;
                                rowEnd = rowStart;
                            }
                        } else {
                            //第一行
                            rowStart = rowStart + 1;
                            rowEnd = rowEnd + 1;
                        }

                        if (index == reportDealerSalesDtoList.size() - 1) {
                            //最后一行
                            ExportHelper.setContentAddMergedRegionByRowAndColumn(xssfSheet, cellStyle, 25, rowStart, rowEnd, 2, 2,
                                    Arguments.isNull(reportDealerSalesDto.getSummaryDate()) ? "" : reportDealerSalesDto.getSummaryDate());
                        }
                    }

                    index++;
                }
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export.dealer.sale.report.to.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }
    /**
     * 根据起止时间获取报表数据
     *
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 报表数据
     */
    private List<ReportSalesDto> getSalesReportListByDate(Date startAt, Date endAt) {
        Response<List<VegaOrderDetail>> response = vegaOrderReadService.findShopOrderByShopIds(startAt, endAt, null);
        if (!response.isSuccess()) {
            log.error("failed to find shop order, startAt={}, endAt={}, cause:{}", startAt, endAt, response.getError());
            throw new JsonResponseException("shop.order.find.fail");
        }

        List<ReportSalesDto> reportSalesDtos = vegaReportComponent.getOperateReportByDate(response.getResult());
        if (Arguments.isNullOrEmpty(reportSalesDtos)) {
            return Collections.emptyList();
        }

        return reportSalesDtos;
    }

    /**
     * 根据起止时间获取服务商每日销售报表数据
     * @param startAt  开始时间
     * @param endAt    结束时间
     * @param shopIds  店铺Ids
     * @return 报表数据
     */
    private List<ReportDealerSalesDto> getDealerSalesReportListByDate(Date startAt, Date endAt, List<Long> shopIds) {
        Response<List<VegaOrderDetail>> response = vegaOrderReadService.findShopOrderByShopIds(startAt, endAt, shopIds);
        if (!response.isSuccess()) {
            log.error("failed to find shop order, startAt={}, endAt={}, cause:{}", startAt, endAt, response.getError());
            throw new JsonResponseException("shop.order.find.fail");
        }

        List<ReportDealerSalesDto> reportDealerSalesDtoList = vegaReportComponent.getDealerSalesReportByDate(response.getResult());
        if (Arguments.isNullOrEmpty(reportDealerSalesDtoList)) {
            return Collections.emptyList();
        }

        return reportDealerSalesDtoList;
    }

    /**
     * List数据合并求和(用于生成月度报表)
     * @param list 分组之前List
     * @return 求和结果
     */
    private ReportSalesDto getSalesReporSum(List<ReportSalesDto> list) {
        ReportSalesDto reportSalesDto = new ReportSalesDto();
        if(list.size() == 1) {
            return list.get(0);
        }
        List<ReportTerminalDto> terminals = Lists.newArrayList();
        Map<Integer, ReportTerminalDto> terminalNewMap = Maps.newHashMap();

        List<ReportCategoryDto> categorys = Lists.newArrayList();
        Map<Long, ReportCategoryDto> categoryMap = Maps.newHashMap();

        for (ReportSalesDto bean : list) {
            reportSalesDto.setOrderSum(numberAdd(reportSalesDto.getOrderSum(), bean.getOrderSum()).intValue()); // 订单数
            reportSalesDto.setSalesVolumeSum(numberAdd(reportSalesDto.getSalesVolumeSum(), bean.getSalesVolumeSum()).longValue()); // 订单总金额
            reportSalesDto.setMemberSum(numberAdd(reportSalesDto.getMemberSum(), bean.getMemberSum()).intValue()); //会员数

            // 访客客户端
            for (ReportTerminalDto r : bean.getReportTerminalDtos()) {
                terminals.removeIf(reportTerminalDto1 -> Objects.equal(reportTerminalDto1.getTerminalId(), r.getTerminalId()));

                Integer sum = 0;
                if (!Arguments.isNull(terminalNewMap.get(r.getTerminalId()))
                        && terminalNewMap.get(r.getTerminalId()).getSum() > 0) {
                    sum = terminalNewMap.get(r.getTerminalId()).getSum();
                }
                ReportTerminalDto reportTerminalDto = new ReportTerminalDto();
                reportTerminalDto.setTerminalId(r.getTerminalId());
                reportTerminalDto.setTerminalType(r.getTerminalType());
                reportTerminalDto.setSum(numberAdd(sum, r.getSum()).intValue());

                terminals.add(reportTerminalDto);
                terminalNewMap = Maps.uniqueIndex(terminals, ReportTerminalDto::getTerminalId);
            }


            // 类目金额统计
            for (ReportCategoryDto r : bean.getReportCategoryDtos()) {
                categorys.removeIf(reportCategoryDto1 -> Objects.equal(reportCategoryDto1.getCategoryId(), r.getCategoryId()));

                Long sum = 0L;
                if (!Arguments.isNull(categoryMap.get(r.getCategoryId()))
                        && categoryMap.get(r.getCategoryId()).getCategoryFee() > 0) {
                    sum = categoryMap.get(r.getCategoryId()).getCategoryFee();
                }

                ReportCategoryDto reportCategoryDto = new ReportCategoryDto();

                reportCategoryDto.setCategoryId(r.getCategoryId());
                reportCategoryDto.setCategoryName(r.getCategoryName());
                reportCategoryDto.setCategoryFee(numberAdd(sum, r.getCategoryFee()).longValue());

                categorys.add(reportCategoryDto);
                categoryMap = Maps.uniqueIndex(categorys, ReportCategoryDto::getCategoryId);
            }

        }
        reportSalesDto.setReportTerminalDtos(terminals);
        reportSalesDto.setReportCategoryDtos(categorys);

        return reportSalesDto;
    }

    /**
     * 根据主键进行分组求和,返回日期列表数据(用于获取按日统计报表)
     * @param list 分组之前List
     * @return 分组之后List
     */
    private List<ReportSalesDto> getSalesReportListByGroup(List<ReportSalesDto> list) {

        // 如果需要sum多个字段，可以定义 key value(object) Map<String, object> map
        Map<String, ReportSalesDto> map = Maps.newHashMap();
        for (ReportSalesDto bean : list) {
            // 如果需要group by 多个字段，对应key=字段a+字段b...
            String key = bean.getSummaryDate();
            ReportSalesDto mapValue = new ReportSalesDto();

            if (map.containsKey(key)) {
                mapValue.setSummaryDate(key); //汇总时间,主键
                mapValue.setOrderSum(numberAdd(map.get(key).getOrderSum(), bean.getOrderSum()).intValue()); // 订单数
                mapValue.setSalesVolumeSum(numberAdd(map.get(key).getSalesVolumeSum(), bean.getSalesVolumeSum()).longValue()); // 订单总金额
                mapValue.setMemberSum(numberAdd(map.get(key).getMemberSum(), bean.getMemberSum()).intValue()); //会员数

                // 访客客户端
                for (ReportTerminalDto r : bean.getReportTerminalDtos()) {
                    List<ReportTerminalDto> rList = map.get(key).getReportTerminalDtos();
                    List<Integer> rLisTerminalId = Lists.transform(rList, ReportTerminalDto::getTerminalId);
                    rList.replaceAll(reportTerminalDto -> {
                        if (Objects.equal(r.getTerminalId(), reportTerminalDto.getTerminalId())) {
                            reportTerminalDto.setSum(numberAdd(reportTerminalDto.getSum(), r.getSum()).intValue());
                        }
                        return reportTerminalDto;
                    });
                    if (!rLisTerminalId.contains(r.getTerminalId())) {
                        rList.add(r);
                    }
                    mapValue.setReportTerminalDtos(rList);
                }

                // 类目金额统计
                for (ReportCategoryDto r : bean.getReportCategoryDtos()) {
                    List<ReportCategoryDto> rList = map.get(key).getReportCategoryDtos();
                    List<Long> rLisCategoryId = Lists.transform(rList, ReportCategoryDto::getCategoryId);
                    rList.replaceAll(reportCategoryDto -> {
                        if (Objects.equal(r.getCategoryId(), reportCategoryDto.getCategoryId())) {
                            reportCategoryDto.setCategoryFee(numberAdd(reportCategoryDto.getCategoryFee(), r.getCategoryFee()).longValue());
                        }
                        return reportCategoryDto;
                    });
                    if (!rLisCategoryId.contains(r.getCategoryId())) {
                        rList.add(r);
                    }
                    mapValue.setReportCategoryDtos(rList);
                }
            } else {
                mapValue.setSummaryDate(key); //汇总时间,主键
                mapValue.setOrderSum(bean.getOrderSum()); // 订单数
                mapValue.setSalesVolumeSum(bean.getSalesVolumeSum()); // 订单总金额
                mapValue.setMemberSum(bean.getMemberSum()); // 会员数
                mapValue.setReportTerminalDtos(bean.getReportTerminalDtos());// 访客客户端
                mapValue.setReportCategoryDtos(bean.getReportCategoryDtos());// 类目金额统计
            }
            map.put(bean.getSummaryDate(), mapValue);
        }

        // 放入list
        List<ReportSalesDto> result = Lists.newArrayList();
        for (Map.Entry<String, ReportSalesDto> entry : map.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
    }


    /**
     * 根据主键进行分组求和,返回日期列表数据(用于获取按日统计报表)
     * @param list 分组之前List
     * @return 分组之后List
     */
    private List<ReportDealerSalesDto> getDealerSalesReportListByGroup(List<ReportDealerSalesDto> list) {

        // 如果需要sum多个字段，可以定义 key value(object) Map<String, object> map
        Map<String, ReportDealerSalesDto> map = Maps.newHashMap();
        for (ReportDealerSalesDto bean : list) {
            // 如果需要group by 多个字段，对应key=字段a+字段b...
            String key = bean.getSummaryDate() + bean.getShopId();
            ReportDealerSalesDto mapValue = new ReportDealerSalesDto();

            if (map.containsKey(key)) {
                mapValue.setKey(key); //主键
                mapValue.setShopId(bean.getShopId());
                mapValue.setShopName(bean.getShopName());
                mapValue.setSummaryDate(bean.getSummaryDate());
                mapValue.setOrderSum(numberAdd(map.get(key).getOrderSum(), bean.getOrderSum()).intValue()); // 订单数
                mapValue.setSalesVolumeSum(numberAdd(map.get(key).getSalesVolumeSum(), bean.getSalesVolumeSum()).longValue()); // 订单总金额
            } else {
                mapValue.setKey(key); //主键
                mapValue.setShopId(bean.getShopId());
                mapValue.setShopName(bean.getShopName());
                mapValue.setSummaryDate(bean.getSummaryDate());
                mapValue.setOrderSum(bean.getOrderSum()); // 订单数
                mapValue.setSalesVolumeSum(bean.getSalesVolumeSum()); // 订单总金额
            }
            map.put(key, mapValue);
        }

        // 放入list
        List<ReportDealerSalesDto> result = Lists.newArrayList();
        for (Map.Entry<String, ReportDealerSalesDto> entry : map.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
    }




    /**
     * 根据年和月判断报表是否已经存在
     * @param year   年
     * @param month  月
     * @return 是否存在
     */
    private VegaReportMonthSale getSalesReportByYearAndMonth(Integer year, Integer month) {
        Response<VegaReportMonthSale> rspReport = vegaReportMonthSaleReadService.findByYearAndMonth(year, month);
        if (!rspReport.isSuccess()) {
            log.error("failed to find month sale report, year={}, month={}, cause:{}", year, month, rspReport.getError());
            throw new JsonResponseException("month.sale.report.find.fail");
        }
        return rspReport.getResult();
    }

    /**
     * 根据Ids获取报表数据,并按月份从小到大排序
     * @param ids
     * @return
     */
    private List<VegaReportMonthSale> getSalesReportByIds(List<Long> ids) {
        Response<List<VegaReportMonthSale>> rspReport = vegaReportMonthSaleReadService.findByIds(ids);
        if (!rspReport.isSuccess()) {
            log.error("failed to find month sale report by ids={}, cause:{}", ids, rspReport.getError());
            throw new JsonResponseException("month.sale.report.find.fail");
        }
        List<VegaReportMonthSale> ReportMonthSales = rspReport.getResult();

        //按照月份从小到大排序
        Collections.sort(ReportMonthSales, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                VegaReportMonthSale r0=(VegaReportMonthSale)o1;
                VegaReportMonthSale r1=(VegaReportMonthSale)o2;
                return r0.getMonth().compareTo(r1.getMonth());
            }
        });

        return ReportMonthSales;
    }


    /**
     * 根据年/月获取月份开始时间
     *
     * @param year  年
     * @param month 月
     * @return date
     */
    private Date getBeginTime(Integer year, Integer month) {
        return new DateTime(year,month,1,0,0,0).withDayOfMonth(1).toDate();
    }

    /**
     * 根据年/月获取月份截止时间
     *
     * @param year  年
     * @param month 月
     * @return date
     */
    private Date getEndTime(Integer year, Integer month) {
        return new DateTime(year,month,1,23,59,59).dayOfMonth().withMaximumValue().toDate();
    }

    private Number numberAdd(Number a, Number b) {
        return (Arguments.isNull(a) ? 0 : a.longValue() ) + (Arguments.isNull(b) ? 0 : b.longValue());
    }

    /**
     * 截止时间
     *
     * @param endAt 截止时间
     * @return date
     */
    private Date endDate(String endAt) {
        return Strings.isNullOrEmpty(endAt) ? null : DateTime.parse(endAt).plusDays(1).toDate();
    }

    /**
     * 起始时间
     *
     * @param startAt 起始时间
     * @return date
     */
    private Date startDate(String startAt) {
        return Strings.isNullOrEmpty(startAt) ? null : DateTime.parse(startAt).toDate();
    }
}
