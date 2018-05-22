package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 店铺库存手工同步,解决大数据时前端等待超时问题
 *
 * Created by lujm on 2017/5/16.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class VegaOrderStorageSyncEvent implements Serializable {

    private static final long serialVersionUID = -1082865893936740158L;

    private Long shopId;

    private Long userId;

    public static VegaOrderStorageSyncEvent from(Long shopId, Long userId) {
        VegaOrderStorageSyncEvent event = new VegaOrderStorageSyncEvent();
        event.setShopId(shopId);
        event.setUserId(userId);
        return event;
    }
}
