/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import com.sanlux.shop.model.CreditAlterResume;
import io.terminus.common.model.Paging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditAlterResumeDto implements Serializable{

    private static final long serialVersionUID = 4487394680892482397L;

    /**
     * 每个月的额度履历
     */
    private Paging<CreditAlterResume> resumePaging;

    /**
     * 每个月的总欠款金额
     */
    private Long totalDebt;

    /**
     * 罚息比率
     */
    private Integer fineRate;

    /**
     * 信用额度利息
     */
    private Integer creditInterest;

}
