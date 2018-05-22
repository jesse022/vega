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
public class PurchaseSkuOrderCriteria extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -4392522061773586338L;

    /**
     * 采购单id
     */
    private Long purchaseId;

    /**
     * 用户ID
     */
    private Long buyerId;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 0 未选中 1 选中
     */
    private Integer status;

    /**
     * 时间上限
     */
    private Date startAt;
    /**
     * 时间下限
     */
    private Date endAt;
}
