package com.sanlux.youyuncai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/2/1.
 */
@Data
public class SkuAttributeDto implements Serializable {

    private static final long serialVersionUID = -268356873593594843L;

    private String key;

    private String value;
}
