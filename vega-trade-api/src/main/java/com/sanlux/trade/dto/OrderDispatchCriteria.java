package com.sanlux.trade.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by liangfujie on 16/8/23
 */
@Data
public class OrderDispatchCriteria  extends PagingCriteria implements Serializable {
    private static final long serialVersionUID = -1213069903543877082L;
    /**
     * 接单店铺ID,这个字段后端处理,前端不需要处理
     */
    private Long receiveShopId;

    /**
     * 订单号
     */
    private Long orderId;

    /**
     * 派单商家ID
     */
    private Long dispatchShopId;

    /**
     * 派单商家名称,后端处理塞入ID
     */
    private String dispatchShopName;


    /**
     *查询起始时间
     */
    private Date startAt;

    /**
     * 查询终止时间
     */

    private Date endAt;


}
