package com.sanlux.item.impl.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sanlux.item.dto.excel.UploadRaw;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Params;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.common.utils.Strs;
import lombok.val;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Effet
 */
public class UploadHelper {

    protected Map<String, String> meta;

    protected Map<String, Integer> header;

    protected List<UploadRaw.Line> lines;

    public UploadHelper(UploadRaw rawData, Set<String> requiredFields) {
        this.meta = Iters.nullToEmpty(rawData.getMeta());
        this.header = Iters.nullToEmpty(rawData.getHeader());
        this.lines = Iters.nullToEmpty(rawData.getLines());
        for (String requiredField : requiredFields) {
            if (!header.containsKey(requiredField)) {
                throw new ServiceException("Excel 非法, 缺少必要字段");
            }
        }
    }

    public MetaHelper getMeta() {
        return new MetaHelper(meta);
    }

    public static class MetaHelper {
        private Map<String, String> meta;
        public MetaHelper(Map<String, String> meta) {
            this.meta = meta;
        }

        public <T> T getValue(String key, boolean required, ValueProcessor<T> processor) {
            return getValue(key, required, processor, null);
        }
        public <T> T getValue(String key, boolean required, ValueProcessor<T> processor, T defaultValue) {
            String value = Params.trimToNull(meta.get(key));
            if (value == null) {
                if (required) {
                    throw err("缺少元信息" + key);
                }
                return defaultValue;
            }
            try {
                T parsedValue = processor.process(value);
                if (parsedValue == null) {
                    if (required && defaultValue == null) {
                        throw err("元信息解析为空" + key);
                    }
                    return defaultValue;
                }
                return parsedValue;
            } catch (ValueProcessException e) {
                throw err("元信息解析失败" + key + e.getMessage());
            } catch (Exception e) {
                Throwables.propagateIfInstanceOf(e, ServiceException.class);
                throw err("元信息读取失败" + key);
            }
        }
    }

    public HeaderHelper getHeader() {
        return new HeaderHelper(this);
    }

    public class HeaderHelper {
        private UploadHelper upper;
        private HeaderHelper(UploadHelper upper) {
            this.upper = upper;
        }
        public Set<String> getFieldsFromPrefix(String prefix) {
            Set<String> fields = Sets.newHashSet();
            for (String key : upper.header.keySet()) {
                if (key.startsWith(prefix)) {
                    fields.add(key);
                }
            }
            return fields;
        }
        public void checkFieldExists(Set<String> fields) throws ServiceException {
            val missing = Sets.difference(fields, upper.header.keySet()).immutableCopy();
            if (!missing.isEmpty()) {
                throw new ServiceException(Joiner.on(",").skipNulls().join(missing) + " 列不存在");
            }
        }
    }

    public int lineCount() {
        return lines.size();
    }

    public LineHelper getLine(int row) {
        if (row > lines.size()) {
            throw new ServiceException("行数非法");
        }
        return new LineHelper(lines.get(row), this);
    }

    public static class LineHelper {

        private UploadRaw.Line line;

        private Map<Integer, UploadRaw.Column> columnMap;

        private UploadHelper upper;

        public LineHelper(UploadRaw.Line line, UploadHelper upper) {
            this.line = line;
            this.upper = upper;
            // TODO: build columnMap
            columnMap = Maps.newHashMap();
            for (UploadRaw.Column column : line.getColumns()) {
                columnMap.put(column.getCol(), column);
            }
        }

        public ServiceException error(String message) {
            return err(line, message);
        }

        public <T> T getValue(String field, boolean required, ValueProcessor<T> processor) {
            return getValue(field, required, processor, null);
        }

        public <T> T getValue(String field, boolean required, ValueProcessor<T> processor, T defaultValue) {
            UploadRaw.Column column = getColumn(field);
            if (column == null) {
                if (required) {
                    throw err("缺少字段" + field);
                }
                return defaultValue;
            }
            String value = Params.trimToNull(column.getValue());
            if (value == null) {
                if (required) {
                    throw err(column, field, "不能为空");
                }
                return defaultValue;
            }
            try {
                T parsedValue = processor.process(value);
                if (parsedValue == null) {
                    if (required && defaultValue == null) {
                        throw err(column, field, "解析为空");
                    }
                    return defaultValue;
                }
                return parsedValue;
            } catch (ValueProcessException e) {
                throw err(column, field, e.getMessage());
            } catch (Exception e) {
                Throwables.propagateIfInstanceOf(e, ServiceException.class);
                throw err(column, field, "读取失败");
            }
        }

        public <T> Map<String, T> getKeyValue(String fieldPrefix, ValueProcessor<T> processor) {
            Map<String, UploadRaw.Column> columns = getColumnsAsPrefix(fieldPrefix);
            if (columns.isEmpty()) {
                return Maps.newHashMap();
            }
            Map<String, T> result = Maps.newHashMap();
            for (Map.Entry<String, UploadRaw.Column> entry : columns.entrySet()) {
                String key = entry.getKey();
                UploadRaw.Column column = entry.getValue();
                String value = Params.trimToNull(column.getValue());
                if (value != null) {
                    try {
                        T parsedValue = processor.process(value);
                        if (parsedValue != null) {
                            result.put(key, parsedValue);
                        }
                    } catch (ValueProcessException e) {
                        throw err(column, key, e.getMessage());
                    } catch (Exception e) {
                        Throwables.propagateIfInstanceOf(e, ServiceException.class);
                        throw err(column, key, "读取失败");
                    }
                }
            }
            return result;
        }

        private Map<String, UploadRaw.Column> getColumnsAsPrefix(String prefix) {
            Map<String, UploadRaw.Column> columns = Maps.newHashMap();
            for (Map.Entry<String, Integer> entry : upper.header.entrySet()) {
                if (entry.getKey() != null && entry.getKey().startsWith(prefix)) {
                    Integer col = entry.getValue();
                    if (col != null) {
                        UploadRaw.Column column = columnMap.get(col);
                        if (column != null) {
                            columns.put(entry.getKey().substring(prefix.length()), column);
                        }
                    }
                }
            }
            return columns;
        }

        private UploadRaw.Column getColumn(String field) {
            Integer col = upper.header.get(field);
            if (col == null) {
                return null;
            }
            UploadRaw.Column column = columnMap.get(col);
            if (column == null) {
                column = new UploadRaw.Column();
                column.setRow(line.getRow());
                column.setCol(col);
                column.setValue(null);
            }
            return column;
        }
    }

    public static ServiceException err(String message) {
        return new ServiceException(message);
    }

    public static ServiceException err(UploadRaw.Line line, String message) {
        return new ServiceException("第" + (line.getRow() + 1) + "行"
                + " " + message);
    }

    public static ServiceException err(UploadRaw.Column column, String field, String message) {
        return new ServiceException("第" + (column.getRow() + 1) + "行"
                + "第" + (column.getCol() + 1) + "列, "
                + field + " " + message);
    }

    public static class ValueProcessException extends RuntimeException {
        private static final long serialVersionUID = -2831849773007520639L;

        public ValueProcessException() {
            super();
        }

        public ValueProcessException(String message) {
            super(message);
        }

        public ValueProcessException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValueProcessException(Throwable cause) {
            super(cause);
        }

        protected ValueProcessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public interface ValueProcessor<T> {
        T process(String rawValue) throws ValueProcessException;
    }

    public static final ValueProcessor<String> STRING_VALUE_PROCESSOR =
            new ValueProcessor<String>() {
                @Override
                public String process(String rawValue) throws ServiceException {
                    return Params.trimToNull(rawValue);
                }
            };

    public static final ValueProcessor<Long> LONG_VALUE_PROCESSOR =
            new ValueProcessor<Long>() {
                @Override
                public Long process(String rawValue) throws ServiceException {
                    return Strs.parseLong(rawValue).orNull();
                }
            };

    public static final ValueProcessor<Integer> INT_VALUE_PROCESSOR =
            new ValueProcessor<Integer>() {
                @Override
                public Integer process(String rawValue) throws ServiceException {
                    return Strs.parseInt(rawValue).orNull();
                }
            };

    public static final ValueProcessor<Long> PRICE_VALUE_PROCESSOR =
            new ValueProcessor<Long>() {
                @Override
                public Long process(String rawValue) throws ServiceException {
                    String value = Params.trimToNull(rawValue);
                    if (value == null) {
                        return null;
                    }
                    return (long) (Double.parseDouble(value) * 100 + 0.001);
                }
            };
}
