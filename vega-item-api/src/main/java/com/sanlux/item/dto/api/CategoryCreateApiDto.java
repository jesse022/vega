package com.sanlux.item.dto.api;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * Created by lujm on 2018/3/27.
 */
@Data
public class CategoryCreateApiDto implements Serializable {

    private static final long serialVersionUID = -6378623268321720793L;

    /**
     * 类目编号
     */
    @NotEmpty(message = "类目Id不能为空")
    private String code;

    /**
     * 类目名称
     */
    @NotEmpty(message = "类目名称不能为空")
    private String name;

    /**
     * 上级ID
     */
    private String parentCode;
}
