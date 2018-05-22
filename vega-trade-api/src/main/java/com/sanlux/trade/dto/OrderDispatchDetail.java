package com.sanlux.trade.dto;

import io.terminus.parana.order.dto.OrderGroup;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by liangfujie on 16/8/26
 */
@Data
public class OrderDispatchDetail implements Serializable {
    private static final long serialVersionUID = -8300799003817587719L;
    /**
     * 订单详细信息
     */
    private OrderGroup orderGroup;

    /**
     * 派送订单创建时间
     */
    private Date createdAt;


}
