/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.criteria;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditResumeCriteria extends PagingCriteria {

    private Integer alterType;

}
