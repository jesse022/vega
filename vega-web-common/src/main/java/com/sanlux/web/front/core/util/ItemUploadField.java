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
@AllArgsConstructor
@NoArgsConstructor
public class ItemUploadField implements Serializable{


    private static final long serialVersionUID = -7930295003876892580L;

    private String key;

    private String title;

    private int cellType;

    private boolean required;
}
