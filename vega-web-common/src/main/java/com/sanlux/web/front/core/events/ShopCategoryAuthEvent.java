package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 一级经销商授权类目更改事件
 *
 * Created by lujm on 2017/5/8.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class ShopCategoryAuthEvent implements Serializable {

    private static final long serialVersionUID = 5963897960482301714L;

    private Long userId;

    private Long shopId;

    public static ShopCategoryAuthEvent from(Long shopId) {
        ShopCategoryAuthEvent event = new ShopCategoryAuthEvent();
        event.setShopId(shopId);
        return event;
    }
}
