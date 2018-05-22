package com.sanlux.item.dto.api;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/3/16.
 */
@Data
public class ItemAttributesDto implements Serializable {

    private static final long serialVersionUID = 8520890978058855022L;

    private String attrsKey;

    private String attrsValue;
}
