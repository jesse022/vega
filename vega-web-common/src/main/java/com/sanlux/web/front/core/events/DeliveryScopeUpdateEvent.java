package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/8/22
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class DeliveryScopeUpdateEvent implements Serializable{


    private static final long serialVersionUID = 5688160339816026673L;

    private Long shopId;

    public static DeliveryScopeUpdateEvent from(Long shopId) {
        DeliveryScopeUpdateEvent event = new DeliveryScopeUpdateEvent();
        event.setShopId(shopId);
        return event;
    }
}
