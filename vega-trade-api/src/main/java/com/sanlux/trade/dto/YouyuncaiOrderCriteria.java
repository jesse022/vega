package com.sanlux.trade.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by lujm on 2018/3/14.
 */
@Data
public class YouyuncaiOrderCriteria extends PagingCriteria implements Serializable {

    private static final long serialVersionUID = 4822678668794941630L;

    /**
     * 集乘网订单Id
     */
    private Long orderId;

    /**
     * 买家用户Id
     */
    private Long userId;

    /**
     * 买家用户Ids
     */
    private List<Long> userIds;

    /**
     * 按名称查询类型 1:企业 2:机构
     */
    private Integer youyuncaiNameType = 1;

    /**
     * 友云采公司名称
     */
    private String companyName;

    /**
     * 友云采订单Id
     */
    private String orderCode;

    /**
     * 开票类型 0为不开票，1为随货开票，2为集中开票
     */
    private String invoiceState;

    /**
     * 是否已开具过发票 0:未开 1：部分已开 2：全部已开
     */
    private Integer hasInvoiced;

    /**
     * 时间上限
     */
    private Date startAt;
    /**
     * 时间下限
     */
    private Date endAt;

    /**
     * 查询子订单限制
     */
    private Integer skuOrderLimit = 5;

}
