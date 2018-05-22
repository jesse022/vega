package com.sanlux.item.dto.excel;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/10/17
 */
@Data
public class UploadRaw implements Serializable {


    private static final long serialVersionUID = 6194070982574388671L;

    private Map<String, String> meta;

    private Map<String, Integer> header;

    private List<Line> lines;

    @Data
    public static class Line implements Serializable {
        private static final long serialVersionUID = -3062326487750006832L;
        private int row;
        private List<Column> columns;
    }

    @Data
    public static class Column implements Serializable {
        private static final long serialVersionUID = 3993543143451790976L;
        private int row;
        private int col;
        private String value;
    }
}
