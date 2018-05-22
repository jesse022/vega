package com.sanlux.web.front.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 经销商批量导入库存事件
 * Created by cuiwentao
 * on 16/12/14
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class ShopAddItemEvent implements Serializable {


    private static final long serialVersionUID = 7292057727366992466L;

    private Long userId;

    private Long shopId;

    private String url;

    public static ShopAddItemEvent from(Long userId, Long shopId, String url) {
        ShopAddItemEvent event = new ShopAddItemEvent();
        event.setUserId(userId);
        event.setShopId(shopId);
        event.setUrl(url);
        return event;
    }
}
