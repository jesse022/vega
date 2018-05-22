package com.sanlux.category.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 经销商授权类目表中的经销商授权类目List
 * Created by cuiwentao
 * on 16/8/4
 */
@Data
public class VegaCategoryAuthDto implements Serializable {


    private static final long serialVersionUID = -7074755310097120795L;
    /**
     * 类目ID
     */
    private Long categoryId;

    /**
     * 类目名称
     */
    private String categoryName;


}
