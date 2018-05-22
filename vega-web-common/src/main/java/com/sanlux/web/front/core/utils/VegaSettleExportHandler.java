/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.utils;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.model.VegaSettleOrderDetail;
import com.sanlux.trade.settle.model.VegaSettleRefundOrderDetail;
import io.terminus.parana.settle.model.PayChannelDailySummary;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import io.terminus.parana.settle.model.Settlement;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author : panxin
 */
@Slf4j
public class VegaSettleExportHandler {// extends SettleExportHandler {

    private static final long serialVersionUID = -948673894767170588L;

    public static void exportPayChannelDetail(List<PayChannelDetail> dataList, OutputStream outputStream) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("支付渠道明细报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();
            columnMap.put("交易成功日期", 1800);
            columnMap.put("渠道名称", 7200);
            columnMap.put("交易金额", 1800);
            columnMap.put("支付平台佣金", 10008);
            columnMap.put("支付平台费率", 4500);
            columnMap.put("实际入账", 4608);
            columnMap.put("类型", 4608);
            columnMap.put("系统内部流水号", 4608);
            columnMap.put("支付平台交易流水号", 4608);
            columnMap.put("对账状态", 4608);
            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder.buildPayChannelDetailContent(dataList.get(i)));
                }
            }

            wb.write(outputStream);
        } catch (Exception var7) {
            log.error("export pay channel detail template fail,cause:{}", Throwables.getStackTraceAsString(var7));
        }

    }

    public static void exportSettlement(List<Settlement> dataList, OutputStream outputStream) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("账务明细报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();
            columnMap.put("创建日期", 1800);
            columnMap.put("完成时间", 1800);
            columnMap.put("渠道名称", 1800);
            columnMap.put("应收退货款", 1800);
            columnMap.put("商家优惠", 1800);
            columnMap.put("平台优惠", 1800);
            columnMap.put("运费", 1800);
            columnMap.put("运费优惠", 1800);
            columnMap.put("实收/退货款", 1800);
            columnMap.put("支付平台佣金", 1800);
            columnMap.put("平台佣金", 1800);
            columnMap.put("交易类型", 1800);
            columnMap.put("平台交易流水号", 7200);
            columnMap.put("支付平台交易流水号", 7200);
            columnMap.put("订单号", 7200);
            columnMap.put("退款单号", 1800);
            columnMap.put("对账状态", 1800);
            columnMap.put("对账完成时间", 1800);
            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder.buildSettlementContent(dataList.get(i)));
                }
            }

            wb.write(outputStream);
        } catch (Exception var7) {
            log.error("export settlement detail template fail,cause:{}", Throwables.getStackTraceAsString(var7));
        }

    }

    public static void exportPayChannelDailySummary(List<PayChannelDailySummary> dataList, OutputStream outputStream) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("支付渠道汇总报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();
            columnMap.put("创建日期", 1800);
            columnMap.put("支付渠道", 1800);
            columnMap.put("总交易额", 1800);
            columnMap.put("支付平台佣金", 1800);
            columnMap.put("渠道净收", 1800);
            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder.buildPayChannelDailySummary(dataList.get(i)));
                }
            }

            wb.write(outputStream);
        } catch (Exception var7) {
            log.error("export settlement detail template fail,cause:{}", Throwables.getStackTraceAsString(var7));
        }

    }

    public static void exportSettleOrderDetail(List<VegaSettleOrderDetail> dataList,
                                               OutputStream outputStream,
                                               Boolean exportIsAdmin) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("订单明细报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();
            columnMap.put("支付时间", 1800);
            columnMap.put("对账时间", 1800);
            columnMap.put("订单ID", 1800);
            if (exportIsAdmin) {
                columnMap.put("公司类型", 1800);
                columnMap.put("公司名", 1800);
            }
            columnMap.put("应收货款", 1800);
            columnMap.put("运费", 1800);
            columnMap.put("实收货款", 1800);
            columnMap.put("支付渠道", 1800);
            columnMap.put("支付平台佣金", 1800);
            columnMap.put("经销商佣金", 1800);
            columnMap.put("平台佣金", 1800);
            if (exportIsAdmin) {
                columnMap.put("应打款", 1800);
            }else {
                columnMap.put("应收款", 1800);
            }

            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder
                            .buildSettleOrderDetail(dataList.get(i), exportIsAdmin));
                }
            }

            wb.write(outputStream);
        } catch (Exception e) {
            log.error("export settlement detail template fail,cause:{}", Throwables.getStackTraceAsString(e));
        }

    }

    public static void exportSettleRefundOrderDetail(List<VegaSettleRefundOrderDetail> dataList,
                                                     OutputStream outputStream,
                                                     Boolean exporterIsAdmin) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("退款单明细报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();

            columnMap.put("退款完成时间", 1800);
            if (exporterIsAdmin) {
                columnMap.put("公司类型", 1800);
                columnMap.put("公司名", 1800);
            }
            columnMap.put("订单号", 1800);
            columnMap.put("退款单号", 1800);
            columnMap.put("应退货款", 1800);
            columnMap.put("平台优惠", 1800);
            columnMap.put("应退运费", 1800);
            columnMap.put("实退货款", 1800);
            columnMap.put("应退经销商佣金", 1800);
            columnMap.put("应退平台佣金", 1800);
            if (exporterIsAdmin) {
                columnMap.put("应扣款", 1800);
            }else {
                columnMap.put("应收款", 1800);
            }
            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                     ExcelSetting.setContent(row, VegaExcelContentBuilder
                            .buildSettleRefundOrderDetail(dataList.get(i), exporterIsAdmin));
                }
            }

            wb.write(outputStream);
        } catch (Exception e) {
            log.error("export settlement detail template fail,cause:{}", Throwables.getStackTraceAsString(e));
        }

    }

    public static void exportSellerTradeDailySummary(List<VegaSellerTradeDailySummary> dataList,
                                                     OutputStream outputStream,
                                                     Boolean isNeedTransStatus) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet e = wb.createSheet("商家日汇总报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();
            columnMap.put("汇总日期", 1800);
            if (isNeedTransStatus) {
                columnMap.put("公司名称", 1800);
            }
            columnMap.put("订单数量", 1800);
            columnMap.put("退款笔数", 1800);
            columnMap.put("应收货款", 1800);
            columnMap.put("平台优惠", 1800);
            columnMap.put("交易退款", 1800);
            columnMap.put("运费", 1800);
            columnMap.put("实收货款", 1800);
            columnMap.put("支付平台佣金", 1800);
            columnMap.put("经销商佣金", 1800);
            columnMap.put("平台佣金", 1800);
            if (isNeedTransStatus) {
                columnMap.put("应打款", 1800);
            }else {
                columnMap.put("应收款", 1800);
            }
            if (isNeedTransStatus) {
                columnMap.put("打款状态", 1800);
            }
            ExcelSetting.setTitleAndColumnWidth(e, columnMap);
            if(dataList != null && dataList.size() > 0) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = e.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder
                            .buildSellerTradeDailySummary(dataList.get(i), isNeedTransStatus));
                }
            }

            wb.write(outputStream);
        } catch (Exception e) {
            log.error("export seller trade daily summary template fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    public static void exportPlatformTradeDailySummary(List<PlatformTradeDailySummary> dataList, OutputStream outputStream) {
        XSSFWorkbook wb = new XSSFWorkbook();

        try {
            XSSFSheet sheet = wb.createSheet("平台日汇总报表");
            Map<String, Integer> columnMap = Maps.newLinkedHashMap();

            columnMap.put("汇总日期", 1800);
            columnMap.put("订单数量", 1800);
            columnMap.put("退款笔数", 1800);
            columnMap.put("应收货款", 1800);
            columnMap.put("交易退款", 1800);
            columnMap.put("运费", 1800);
            columnMap.put("实收货款", 1800);
            columnMap.put("支付平台佣金", 1800);
            columnMap.put("经销商佣金", 1800);
            columnMap.put("平台佣金", 1800);
            columnMap.put("应打款", 1800);

            ExcelSetting.setTitleAndColumnWidth(sheet, columnMap);

            if(!dataList.isEmpty()) {
                for(int i = 0; i < dataList.size(); ++i) {
                    XSSFRow row = sheet.createRow(i + 1);
                    ExcelSetting.setContent(row, VegaExcelContentBuilder.buildPlatformTradeDailySummary(dataList.get(i)));
                }
            }

            wb.write(outputStream);
        } catch (Exception e) {
            log.error("export platform trade daily summary template fail,cause:{}", Throwables.getStackTraceAsString(e));
        }

    }

}
