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
public class CreateSecondShopEvent implements Serializable {


    private static final long serialVersionUID = 8531632826979870041L;

    private Long shopId;

    public static CreateSecondShopEvent from(Long shopId) {
        CreateSecondShopEvent event = new CreateSecondShopEvent();
        event.setShopId(shopId);
        return event;
    }
}
