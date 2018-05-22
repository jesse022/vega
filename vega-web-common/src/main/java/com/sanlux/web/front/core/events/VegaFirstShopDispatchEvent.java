package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 一级经销商派单二级时,修改订单二级成本价事件
 *
 * Created by lujm on 2017/12/6.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class VegaFirstShopDispatchEvent implements Serializable {
    private static final long serialVersionUID = -511122840485577692L;

    private Long orderId;

    private Long shopId;

    public static VegaFirstShopDispatchEvent from(Long orderId, Long shopId) {
        VegaFirstShopDispatchEvent event = new VegaFirstShopDispatchEvent();
        event.setOrderId(orderId);
        event.setShopId(shopId);
        return event;
    }
}
