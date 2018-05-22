package com.sanlux.youyuncai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/1/30.
 */
@Data
public class CategoryDto implements Serializable {

    private static final long serialVersionUID = 7521047711164252169L;

    /**
     * 商品分类编码,必须
     */
    private String code;

    /**
     * 商品分类名称,必须
     */
    private String name;

    /**
     * 商品分类上级编码,顶级分类可为空
     */
    private String parentCode;
}
