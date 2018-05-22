package com.sanlux.web.front.core.events.trade;

import com.sanlux.trade.enums.TradeSmsNodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/13/16
 * Time: 1:36 PM
 */
@Data
public class TradeSmsEvent implements Serializable {

    private static final long serialVersionUID = 1692069757102216302L;

    private Long orderId;
    private String expressCompanyName;
    private String shipmentSerialNo;
    private TradeSmsNodeEnum nodeEnum;


    public TradeSmsEvent(Long orderId,TradeSmsNodeEnum nodeEnum){
        this.orderId=orderId;
        this.nodeEnum=nodeEnum;
    }

    public TradeSmsEvent(Long orderId, String expressCompanyName, String shipmentSerialNo, TradeSmsNodeEnum nodeEnum){
        this.orderId = orderId;
        this.expressCompanyName = expressCompanyName;
        this.shipmentSerialNo = shipmentSerialNo;
        this.nodeEnum=nodeEnum;
    }
}
