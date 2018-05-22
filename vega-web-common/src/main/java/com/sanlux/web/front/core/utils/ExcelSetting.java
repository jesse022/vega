/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author : panxin
 */
public class ExcelSetting {

    public ExcelSetting() {
    }

    public static void setTitleAndColumnWidth(XSSFSheet s, Map<String, Integer> columnMap) {
        XSSFRow titleRow = s.createRow(0);
        int i = 0;

        for(Iterator var4 = columnMap.keySet().iterator(); var4.hasNext(); ++i) {
            String name = (String)var4.next();
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(name);
            s.setColumnWidth(i, columnMap.get(name));
        }

    }

    public static void setContent(Row row, List<String> datas) {
        int i = 0;

        for(Iterator var3 = datas.iterator(); var3.hasNext(); ++i) {
            String data = (String)var3.next();
            Cell cell = row.createCell(i);
            cell.setCellValue(data);
        }

    }

}
