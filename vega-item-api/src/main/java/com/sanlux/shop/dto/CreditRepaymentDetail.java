/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.CreditRepaymentResume;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditRepaymentDetail implements Serializable{

    private static final long serialVersionUID = 8195577736474223415L;

    private CreditAlterResume alterResume;

    private List<CreditRepaymentResume> repaymentResumeList;

}
