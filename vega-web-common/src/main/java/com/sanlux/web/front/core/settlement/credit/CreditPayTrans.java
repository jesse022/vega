/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.credit;

import io.terminus.pay.api.Trans;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
public class CreditPayTrans implements Trans, Serializable {
    private static final long serialVersionUID = -2890640767293714738L;

    private Long id;

    private Long fee;

    private String account;

    private String tradeNo;

    private String refundNo;

    private String channel;

}
