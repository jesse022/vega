/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import com.sanlux.shop.model.CreditAlterResume;
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
public class CreditAlterResumeDetail implements Serializable {

    private static final long serialVersionUID = -8267912587885984427L;

    private Long totalCredit;

    private Long availableCredit;

    private Boolean isCreditAvailable;

    private CreditAlterResume alterResume;
}
