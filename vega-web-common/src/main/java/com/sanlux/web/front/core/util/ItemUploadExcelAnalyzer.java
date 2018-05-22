package com.sanlux.web.front.core.util;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.sanlux.item.dto.excel.UploadRaw;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Params;
import io.terminus.parana.attribute.dto.AttributeMetaKey;
import io.terminus.parana.category.dto.GroupedCategoryAttribute;
import io.terminus.parana.category.model.CategoryAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by cuiwentao
 * on 16/10/14
 */
@Slf4j
@Service
public class ItemUploadExcelAnalyzer {

    public static final String TEMPLATE_SHEET_NAME = "Template";


    public static void buildTemplateExcel(String[] categories,
                                   List<GroupedCategoryAttribute> attributeList,
                                   ServletOutputStream outputStream) throws IOException {
        try (InputStream inputStream = Resources.asByteSource(Resources.getResource("excel/intro-and-sample.xlsx")).openStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.createSheet(TEMPLATE_SHEET_NAME);

            ItemUploadHeader header = buildHeader(categories, attributeList);

            // Normal font
            Font normalFont = wb.createFont();
            normalFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);

            // Bold font
            Font boldFont = wb.createFont();
            boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

            Row bannerRow = sheet.createRow(0);
            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(20);
            Row columnIdRow = sheet.createRow(2);
            columnIdRow.setHeightInPoints(20);
            int offset = 0;
            for (ItemUploadHeader.Group group : header.getArray()) {
                // Banner cell style
                CellStyle bannerCellStyle = createHeaderCellStyle(wb, boldFont, group.getColorIndex());
                bannerCellStyle.setAlignment(CellStyle.ALIGN_LEFT);

                // Title cell style (required column)
                CellStyle titleRequiredCellStyle = createHeaderCellStyle(wb, boldFont, group.getColorIndex());

                // Title cell style (not required column)
                CellStyle titleNotRequiredCellStyle = createHeaderCellStyle(wb, normalFont, group.getColorIndex());

                if (group.getFields() == null || group.getFields().length == 0) {
                    continue;
                }
                int length = group.getFields().length;
                for (int i = offset; i < offset + length; ++i) {
                    // fill empty string cell
                    Cell cell = bannerRow.createCell(i, Cell.CELL_TYPE_STRING);
                    cell.setCellValue("");
                    cell.setCellStyle(bannerCellStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(0, 0, offset, offset + length - 1));
                String value = Joiner.on(" - ").skipNulls().join(
                        Strings.emptyToNull(group.getName()),
                        Strings.emptyToNull(group.getDescription())
                );
                bannerRow.getCell(offset).setCellValue(value);

                for (int i = offset; i < offset + length; ++i) {
                    ItemUploadField field = group.getFields()[i - offset];
                    Cell titleCell = titleRow.createCell(i, Cell.CELL_TYPE_STRING);
                    titleCell.setCellStyle(field.isRequired() ? titleRequiredCellStyle : titleNotRequiredCellStyle);
                    titleCell.setCellValue(field.getTitle());

                    Cell columnIdCell = columnIdRow.createCell(i, Cell.CELL_TYPE_STRING);
                    columnIdCell.setCellStyle(field.isRequired() ? titleRequiredCellStyle : titleNotRequiredCellStyle);
                    columnIdCell.setCellValue(field.getKey());
                }
                offset += length;
            }

            for (int i = 0; i < offset; ++i) {
                sheet.autoSizeColumn(i, true);
            }

            sheet.createFreezePane(0, 2);

            wb.write(outputStream);
        }
    }

    public static UploadRaw analyzeItemExcel (InputStream inputStream) throws IOException {
        Workbook wb = new XSSFWorkbook(inputStream);
        Sheet sheet = wb.getSheet("商品");
        if (sheet == null) {
            log.error("stock manager excel is format has error");
            throw new JsonResponseException("stock.manager,excel.format.error");
        }
        return analyzeTitileAndLine(sheet, 2);
    }

    public static UploadRaw analyzeStockExcel (InputStream inputStream) throws IOException, ServiceException {
        Workbook wb = new XSSFWorkbook(inputStream);
        Sheet sheet = wb.getSheet("库存管理表");
        if (sheet == null) {
            log.error("stock manager excel is format has error");
            throw new JsonResponseException("stock.manager,excel.format.error");
        }
        return analyzeTitileAndLine(sheet, 2);
    }

    public static UploadRaw analyzeSellerPriceExcel (InputStream inputStream) throws IOException, ServiceException {
        Workbook wb = new XSSFWorkbook(inputStream);
        Sheet sheet = wb.getSheet("商品管理");
        if (sheet == null) {
            log.error("set seller price excel is format has error");
            throw new JsonResponseException("set.seller.price.excel.format.error");
        }
        return analyzeTitileAndLine(sheet, 2);
    }

    public static UploadRaw analyze(InputStream inputStream) throws IOException , ServiceException {
        try {
            Workbook wb = new XSSFWorkbook(inputStream);
            Sheet sheet = wb.getSheet(TEMPLATE_SHEET_NAME);
            if (sheet != null) {
                return analyze2(sheet);
            }
            for (int i = 0; i < wb.getNumberOfSheets(); ++i) {
                Sheet s = wb.getSheetAt(i);
                if (s != null && s.getSheetName() != null
                        && (s.getSheetName().startsWith("templ")
                        || s.getSheetName().startsWith("Templ"))) {
                    return analyze2(s);
                }
            }
        } catch (IOException e) {
            log.error("io exception:{}", e.getMessage());
            throw new IOException(e);
        } catch (ServiceException e) {
            log.error("service exception:{}", e.getMessage());
            throw new ServiceException(e.getMessage());
        } catch (Exception e) {
            log.error("product upload excel invalid,sheet not found");
            throw new ServiceException("product.upload.excel.invalid.sheet.not.found");
        }
        return null;
    }

    public static UploadRaw analyze2(Sheet sheet) throws ServiceException {
        Row bannerRow = sheet.getRow(0);
        if (bannerRow == null) {
            throw new ServiceException("template.format.error");
        }
        String scopeStr = readString(bannerRow, 0);
        if (Strings.isNullOrEmpty(scopeStr)) {
            throw new ServiceException("template.format.error.scope.missing");
        }
        String pathStr;
        if (scopeStr.startsWith("Scope=")) {
            pathStr = scopeStr.substring("Scope=".length());
        } else if (scopeStr.startsWith("S=")) {
            pathStr = scopeStr.substring("S=".length());
        } else {
            throw new ServiceException("template.format.error.scope.invalid");
        }

        Map<String, String> meta = Maps.newHashMap();
        meta.put("Scope", pathStr);

        UploadRaw result = analyzeTitileAndLine(sheet, 3);
        result.setMeta(meta);
        return result;
    }

    private static UploadRaw analyzeTitileAndLine (Sheet sheet, Integer lineStartNum) {
        int lastRowNum = sheet.getLastRowNum();
        // error as no data
        if (lastRowNum <= lineStartNum - 1) {
            throw new ServiceException("template.format.error");
        }


        Row titleRow = sheet.getRow(lineStartNum - 1);
        if (titleRow == null) {
            throw new ServiceException("template.format.error");
        }

        Map<String, Integer> header = Maps.newHashMap();
        for (Cell cell : titleRow) {
            if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                continue;
            }
            if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
                throw new ServiceException("template.format.error");
            }
            String key = Params.trimToNull(cell.getStringCellValue());
            if (key != null) {
                header.put(key, cell.getColumnIndex());
            }
        }

        List<UploadRaw.Line> lines = Lists.newArrayList();
        for (int i = lineStartNum; i <= lastRowNum; ++i) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            boolean isEmptyRow = true;
            for (Cell cell : row) {
                if (cell == null) {
                    continue;
                }
                if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    continue;
                }
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    if (Params.trimToNull(cell.getStringCellValue()) != null) {
                        isEmptyRow = false;
                        break;
                    }
                }
            }
            if (isEmptyRow) {
                continue;
            }

            UploadRaw.Line line = new UploadRaw.Line();
            lines.add(line);
            line.setRow(i);
            List<UploadRaw.Column> columns = Lists.newArrayList();
            line.setColumns(columns);
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    continue;
                }
                if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
                    // TODO: 暂时强制设置
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                }
                UploadRaw.Column column = new UploadRaw.Column();
                column.setRow(i);
                column.setCol(cell.getColumnIndex());
                column.setValue(Params.trimToNull(cell.getStringCellValue()));
                columns.add(column);
            }
        }


        UploadRaw result = new UploadRaw();
        result.setHeader(header);
        result.setLines(lines);
        return result;
    }


    private static ItemUploadHeader buildHeader(String[] categories, List<GroupedCategoryAttribute> attributeList) {
        ItemUploadHeader header = new ItemUploadHeader();
        header.setArray(
                new ItemUploadHeader.Group[]{
                        new ItemUploadHeader.Group("", "Scope=" + Joiner.on('\\').join(categories), IndexedColors.LEMON_CHIFFON.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("item_name", "商品名", Cell.CELL_TYPE_STRING, true),
                        }),
                        new ItemUploadHeader.Group("", "version = 1.0", IndexedColors.LEMON_CHIFFON.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("brand", "品牌", Cell.CELL_TYPE_STRING, true),
                        }),
                        new ItemUploadHeader.Group("", "请勿修改或删除前三行", IndexedColors.LEMON_CHIFFON.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("code", "商品代码(货号)", Cell.CELL_TYPE_STRING, true),
                        }),
                        new ItemUploadHeader.Group("关联关系", "多规格商品/规格/单规格商品", IndexedColors.CORAL.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("parent_child", "商品规格", Cell.CELL_TYPE_STRING, true),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.BLUE_GREY.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("item_outer_id", "商品外部ID", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.BLUE_GREY.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("sku_outer_id", "SKU外部ID", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.BLUE_GREY.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("brand_code", "商标代号", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.BLUE_GREY.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("other_no", "对方货号", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.BLUE_GREY.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("delivery_fee_template_id", "运费模板ID", Cell.CELL_TYPE_NUMERIC, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("image", "商品图片", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("价格信息", "商品价格 (分)", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("price", "供货价", Cell.CELL_TYPE_NUMERIC, true),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("stock_quantity", "库存数量", Cell.CELL_TYPE_NUMERIC, true),
                        }),
                        new ItemUploadHeader.Group("计量信息", "", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("default_unit_of_measure", "计量单位", Cell.CELL_TYPE_STRING, false),
                                new ItemUploadField("default_per_unit_amount", "计量值", Cell.CELL_TYPE_NUMERIC, false),

                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("assistant_unit", "副单位", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("", "", IndexedColors.ROSE.getIndex(), new ItemUploadField[]{
                                new ItemUploadField("trademark_name", "商标名称", Cell.CELL_TYPE_STRING, false),
                        }),
                        new ItemUploadHeader.Group("销售属性", "最多支持两种", IndexedColors.LIGHT_ORANGE.getIndex(),
                                buildCustomFieldsForVariant(attributeList)
                        ),
                        new ItemUploadHeader.Group("非销售属性", "", IndexedColors.ORANGE.getIndex(),
                                buildCustomFields(attributeList)
                        ),
                }
        );
        return header;
    }

    private static ItemUploadField[] buildCustomFields(List<GroupedCategoryAttribute> attributeList) {
        List<ItemUploadField> fields = Lists.newArrayList();
        Map<String, GroupedCategoryAttribute> attributeMap =
                Maps.uniqueIndex(attributeList, GroupedCategoryAttribute::getGroup);
        GroupedCategoryAttribute groupedCategoryAttribute = attributeMap.get("DEFAULT");
        if (groupedCategoryAttribute == null) {
            return fields.toArray(new ItemUploadField[fields.size()]);
        }
        for (CategoryAttribute categoryAttribute : groupedCategoryAttribute.getCategoryAttributes()) {
            if (categoryAttribute.getStatus() == 1
                    && Objects.equals(categoryAttribute.getAttrMetas().get(AttributeMetaKey.SKU_CANDIDATE), "false")) {
                fields.add(new ItemUploadField("custom_" + categoryAttribute.getAttrKey(), categoryAttribute.getAttrKey(), Cell.CELL_TYPE_STRING, true));
            }
        }
        return fields.toArray(new ItemUploadField[fields.size()]);
    }

    private static ItemUploadField[] buildCustomFieldsForVariant(List<GroupedCategoryAttribute> attributeList) {
        List<ItemUploadField> fields = Lists.newArrayList();
        Map<String, GroupedCategoryAttribute> attributeMap =
                Maps.uniqueIndex(attributeList, GroupedCategoryAttribute::getGroup);
        GroupedCategoryAttribute groupedCategoryAttribute = attributeMap.get("DEFAULT");
        if (groupedCategoryAttribute == null) {
            return fields.toArray(new ItemUploadField[fields.size()]);
        }
        for (CategoryAttribute categoryAttribute : groupedCategoryAttribute.getCategoryAttributes()) {
            if (categoryAttribute.getStatus() == 1
                    && Objects.equals(categoryAttribute.getAttrMetas().get(AttributeMetaKey.SKU_CANDIDATE), "true")) {
                fields.add(new ItemUploadField("custom_" + categoryAttribute.getAttrKey(), categoryAttribute.getAttrKey(), Cell.CELL_TYPE_STRING, true));
            }
        }
        return fields.toArray(new ItemUploadField[fields.size()]);
    }

    private static CellStyle createHeaderCellStyle(Workbook workbook, Font font, short color) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(color);
        cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setFont(font);
        return cellStyle;
    }

    private static String readString(Row row, Integer columnIndex) {
        Cell cell = readCell(row, columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
        }
        return cell.getStringCellValue();
    }

    private static Cell readCell(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        return row.getCell(columnIndex, Row.RETURN_BLANK_AS_NULL);
    }

}
