package com.sanlux.trade.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 查询条件
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 4:03 PM
 */
@Data
public class PurchaseOrderCriteria extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -1036264162515549757L;

    /**
     * 用户ID
     */
    private Long buyerId;

    /**
     * 商品种类,用于过滤友云采专属订单(sku_quantity = 1 and is_temp =0 代表友云采专属采购单,正常不可见)
     */
    private Integer skuQuantity;

    /**
     * 名称
     */
    private String name;

    /**
     * 是否为临时采购单
     */
    private Boolean isTemp;

    /**
     * 时间上限
     */
    private Date startAt;
    /**
     * 时间下限
     */
    private Date endAt;
}
