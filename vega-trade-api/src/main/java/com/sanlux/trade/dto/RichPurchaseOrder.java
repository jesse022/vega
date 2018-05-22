package com.sanlux.trade.dto;

import com.sanlux.trade.model.PurchaseOrder;
import io.terminus.common.model.Paging;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/15/16
 * Time: 2:04 PM
 */
@Data
public class RichPurchaseOrder implements Serializable{

    private List<PurchaseOrder> purchaseOrders;

    private Long currentPurchaseOrderId;

    private String currentPurchaseOrderName;

    private Paging<RichPurchaseSkuOrder> paging;
}
