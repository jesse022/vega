package com.sanlux.item.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/11/14
 */
@Data
public class ShopSkuCriteria  extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -5612769455063319646L;

    private Long categoryId;

    private Long shopId;
}
