/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.criteria;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

/**
 * @author : panxin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VegaSellerTradeDailySummaryCriteria extends PagingCriteria implements Serializable {
    private static final long serialVersionUID = -7726021258959795738L;

    /**
     * 汇总起始时间
     */
    private Date sumStartAt;

    /**
     * 汇总截止时间
     */
    private Date sumEndAt;

    /**
     * 商家名称
     */
    private Long sellerId;

    /**
     * 商家类型
     */
    private Integer sellerType;

    /**
     * 商家名称
     */
    private String sellerName;

    /**
     * 汇总类型
     * @see io.terminus.parana.settle.enums.SummaryType
     */
    private Integer summaryType;

    /**
     * 打款状态
     */
    private Integer transStatus;

    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    @Override
    public void formatDate(){
        if(sumStartAt != null && sumEndAt != null){
            if(sumStartAt.equals(sumEndAt)){
                sumEndAt=new DateTime(sumEndAt.getTime()).plusDays(1).minusSeconds(1).toDate();
            }
        }
    }

}
