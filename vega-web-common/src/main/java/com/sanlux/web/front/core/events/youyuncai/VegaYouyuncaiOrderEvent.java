package com.sanlux.web.front.core.events.youyuncai;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 友云采订单
 * Created by lujm on 2018/3/12.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class VegaYouyuncaiOrderEvent implements Serializable {

    private static final long serialVersionUID = -1748818410903186591L;

    private Long orderId;

    private String shipmentCompanyName;

    private String shipmentSerialNo;


    /**
     * 友云采出货通知接口
     * @param orderId  集乘网订单号
     * @param shipmentCompanyName     物流公司名称
     * @param shipmentSerialNo        物流单号
     * @return
     */
    public static VegaYouyuncaiOrderEvent formOrderShopInfo(Long orderId, String shipmentCompanyName, String shipmentSerialNo){
        VegaYouyuncaiOrderEvent event = new VegaYouyuncaiOrderEvent();
        event.setOrderId(orderId);
        event.setShipmentCompanyName(shipmentCompanyName);
        event.setShipmentSerialNo(shipmentSerialNo);
        return event;
    }
}
