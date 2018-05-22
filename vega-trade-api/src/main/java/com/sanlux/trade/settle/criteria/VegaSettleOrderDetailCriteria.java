/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.criteria;

import io.terminus.parana.settle.dto.paging.SettleOrderDetailCriteria;
import lombok.Data;

/**
 * @author : panxin
 */
@Data
public class VegaSettleOrderDetailCriteria extends SettleOrderDetailCriteria {
    private static final long serialVersionUID = 6351284793445871328L;

    private String sellerName;

    private String channel;

}
