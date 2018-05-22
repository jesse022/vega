package com.sanlux.trade.dto;

import io.terminus.common.model.Paging;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.SkuOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by liangfujie on 16/8/26
 */
@Data
public class VegaOrderDetail extends OrderDetail implements Serializable {

    private static final long serialVersionUID = 3715257722032160406L;

    Paging<SkuOrderAndOperation> skuOrderPaging;//SKU订单分页信息



    @Data
    public static class SkuOrderAndOperation implements Serializable {

        private static final long serialVersionUID = -7022678909886805049L;

        private SkuOrder skuOrder;

        private Set<OrderOperation> skuOrderOperations;
    }
}
