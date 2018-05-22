package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/9/20
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class FirstShopCreateScopeOrAuthEvent implements Serializable {


    private static final long serialVersionUID = 5536715536902377927L;

    private Long shopId;

    public static FirstShopCreateScopeOrAuthEvent from(Long shopId) {
        FirstShopCreateScopeOrAuthEvent event = new FirstShopCreateScopeOrAuthEvent();
        event.setShopId(shopId);
        return event;
    }
}
