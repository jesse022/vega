package com.sanlux.web.front.core.utils;

import com.sanlux.common.helper.DateHelper;
import io.terminus.common.utils.Arguments;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/10/12
 */
public class ExportHelper {
    private static FormulaEvaluator evaluator;
//设置表单头信息
    public static void setTitleAndColumnWidth(XSSFSheet s, Map<String, Integer> columnMap) {

        Row titleRow = s.createRow(0);
        int i = 0;
        for (String name : columnMap.keySet()) {
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(name);
            s.setColumnWidth(i, columnMap.get(name));
            i++;
        }
    }

    /**
     * 指定行创建标题内容
     * @param s  Sheet
     * @param row 行,基于1
     * @param rowHeight 行高
     * @param cellStyle 单元格样式
     * @param columnMap 列内容
     */
    public static void setTitleAndColumnWidth(XSSFSheet s, XSSFCellStyle cellStyle, int row, int rowHeight,
                                              Map<String, Integer> columnMap) {

        Row titleRow = s.createRow(row-1);
        titleRow.setHeightInPoints((short)rowHeight);
        int i = 0;
        for (String name : columnMap.keySet()) {
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(name);
            cell.setCellStyle(cellStyle);
            s.setColumnWidth(i, columnMap.get(name));
            i++;
        }
    }


    //设置表单行内容
    public static void setContent(Row row, List<String> datas) {
        int i = 0;
        for (String data : datas) {
            Cell cell = row.createCell(i);
            cell.setCellValue(data);
            i++;
        }

    }

    public static void setContent(Row row, XSSFCellStyle cellStyle, int rowHeight, List<String> datas) {
        int i = 0;
        row.setHeightInPoints((short)rowHeight);
        for (String data : datas) {
            Cell cell = row.createCell(i);
            cell.setCellValue(data);
            cell.setCellStyle(cellStyle);
            i++;
        }
    }

    public static void setContentByRowAndColumn(XSSFSheet sheet,int row,int column,String content){
        setContentByRowAndColumn(sheet, null, null, row, column, content);
    }

    /**
     * 指定行、列书写值。
     * @param sheet sheet
     * @param cellStyle 单元格样式
     * @param rowHeight 行高
     * @param row  行,基于1
     * @param column 列,基于1
     * @param content  将要书写的内容
     */
    public static void setContentByRowAndColumn(XSSFSheet sheet, XSSFCellStyle cellStyle, Integer rowHeight, int row,int column,String content){
        Row rows = sheet.getRow(row - 1);
        if (rows==null) {
            rows = sheet.createRow(row - 1);
        }
        Cell cell = rows.getCell(column-1);
        if(cell==null){
            cell = rows.createCell(column-1);
        }
        //获取当前行和列的表格样式
        XSSFRow rowCellStyle = sheet.getRow(row - 1);
        XSSFCellStyle columnOne = rowCellStyle.getCell(column-1).getCellStyle();
        columnOne.setWrapText(true);//支持换行显示样式,需传入换行符"/r/n"
        if (Arguments.notNull(rowHeight)) {
            rows.setHeightInPoints(rowHeight);
        }
        cell.setCellValue(content);
        cell.setCellStyle(Arguments.isNull(cellStyle) ? columnOne : cellStyle);// 填充样式
    }

    /**
     * 指定行、列合并单元格书写值。
     * Created by lujm on 2017/05/11
     * @param sheet sheet
     * @param rowStart 起始行 基于1
     * @param rowEnd 截止行 基于1
     * @param columnStart 起始列 基于1
     * @param columnEnd 截止列 基于1
     * @param content 将要被书写的内容。
     */
    public static void setContentAddMergedRegionByRowAndColumn(XSSFSheet sheet,int rowStart,int rowEnd,int columnStart,int columnEnd,String content){
        Row nowRow = sheet.getRow(rowStart - 1);
        if (nowRow==null) {
            nowRow = sheet.createRow(rowStart - 1);
        }
        Cell nowCell = nowRow.getCell(columnStart-1);
        if(nowCell==null){
            nowCell = nowRow.createCell(columnStart-1);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowStart-1, rowEnd-1, columnStart-1, columnEnd-1));//合并单元格  参数：起始行号，终止行号， 起始列号，终止列号

        //获取当前行和列的表格样式
        XSSFRow rowCellStyle = sheet.getRow(rowStart - 1);
        XSSFCellStyle columnOne = rowCellStyle.getCell(columnStart-1).getCellStyle();
        columnOne.setWrapText(true);//支持换行显示样式,需传入换行符"/r/n"

        nowCell.setCellValue(content);
        nowCell.setCellStyle(columnOne);// 填充样式
    }

    /**
     * 指定行、列合并单元格书写值(用于自动生成Excel的模板)。
     *
     * @param sheet sheet
     * @param cellStyle 样式
     * @param rowHeight 行高
     * @param rowStart 起始行 基于1
     * @param rowEnd 截止行 基于1
     * @param columnStart 起始列 基于1
     * @param columnEnd 截止列 基于1
     * @param content 将要被书写的内容。
     */
    public static void setContentAddMergedRegionByRowAndColumn(XSSFSheet sheet, XSSFCellStyle cellStyle, int rowHeight,
                                                               int rowStart,int rowEnd,int columnStart,int columnEnd,String content){
        // 初始化样式
        for (int i = rowStart - 1; i <= rowEnd - 1; i++) {
            Row row = sheet.getRow(i);
            if (row==null) {
                row = sheet.createRow(i);
            }
            for (int j = columnStart - 1; j <= columnEnd - 1; j++) {
                Cell cell = row.getCell(j);
                if(cell==null){
                    cell = row.createCell(j);
                }
                cell.setCellStyle(cellStyle);
            }
        }

        Row nowRow = sheet.getRow(rowStart - 1);
        if (nowRow==null) {
            nowRow = sheet.createRow(rowStart - 1);
        }
        Cell nowCell = nowRow.getCell(columnStart-1);
        if(nowCell==null){
            nowCell = nowRow.createCell(columnStart-1);
        }

        nowRow.setHeightInPoints((short)rowHeight);
        sheet.addMergedRegion(new CellRangeAddress(rowStart-1, rowEnd-1, columnStart-1, columnEnd-1));//合并单元格  参数：起始行号，终止行号， 起始列号，终止列号

        nowCell.setCellValue(content);
        nowCell.setCellStyle(cellStyle);

    }


    /**
     * 获取单元格各类型值，返回字符串类型
     * @param cell 单元格
     * @return 返回值
     */
    public static String getCellValueByCell(Cell cell) {
        //判断是否为null或空串
        if (cell == null || cell.toString().trim().equals("")) {
            return "";
        }
        String cellValue = "";
        int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_FORMULA) { //表达式类型
            cellType = evaluator.evaluate(cell).getCellType();
        }

        switch (cellType) {
            case Cell.CELL_TYPE_STRING: //字符串类型
                cellValue = cell.getStringCellValue().trim();
                cellValue = Arguments.isEmpty(cellValue) ? "" : cellValue;
                break;
            case Cell.CELL_TYPE_BOOLEAN:  //布尔类型
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_NUMERIC: //数值类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {  //判断日期类型
                    cellValue = DateHelper.formatDate(cell.getDateCellValue(), "yyyy-MM-dd");
                } else {  //否
                    cellValue = new DecimalFormat("#.######").format(cell.getNumericCellValue());
                }
                break;
            default: //其它类型，取空串吧
                cellValue = "";
                break;
        }
        return cellValue;
    }

    /**
     * 设置单元格样式
     * @param cellStyle
     * @return
     */
    public static XSSFCellStyle setCellStyle(XSSFCellStyle cellStyle) {
        cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN); //下边框
        cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);//左边框
        cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);//上边框
        cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);//右边框:
        cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER); //左右居中
        cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER); //上下居中
        cellStyle.setWrapText(true);//设置自动换行

        return cellStyle;
    }

}
