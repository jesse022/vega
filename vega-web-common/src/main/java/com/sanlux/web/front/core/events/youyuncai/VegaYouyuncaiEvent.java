package com.sanlux.web.front.core.events.youyuncai;

import com.sanlux.youyuncai.enums.YouyuncaiApiType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/2/6.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class VegaYouyuncaiEvent implements Serializable {

    private static final long serialVersionUID = 4469648062719977710L;

    private Long itemId;

    private Integer apiType;

    private List<Long> itemIds;

    public static VegaYouyuncaiEvent formItemByitemId(Long itemId, Integer apiType){
        VegaYouyuncaiEvent event = new VegaYouyuncaiEvent();
        event.setItemId(itemId);
        event.setApiType(apiType);
        return event;
    }

    public static VegaYouyuncaiEvent formItemByitemIds(List<Long> itemIds){
        VegaYouyuncaiEvent event = new VegaYouyuncaiEvent();
        event.setApiType(YouyuncaiApiType.ITEM_ALL.value());
        event.setItemIds(itemIds);
        return event;
    }
}
