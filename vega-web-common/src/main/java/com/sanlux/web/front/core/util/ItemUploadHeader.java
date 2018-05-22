package com.sanlux.web.front.core.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/10/14
 */
@Data
public class ItemUploadHeader implements Serializable {


    private static final long serialVersionUID = -3949217670932563470L;
    private Group[] array;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group implements Serializable {
        private static final long serialVersionUID = -5291915514976398819L;
        private String name;
        private String description;
        private short colorIndex;
        private ItemUploadField[] fields;
    }
}
