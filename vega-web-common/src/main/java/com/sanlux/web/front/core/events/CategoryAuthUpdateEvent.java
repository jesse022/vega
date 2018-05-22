package com.sanlux.web.front.core.events;

import com.sanlux.category.dto.VegaCategoryDiscountDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/22
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class CategoryAuthUpdateEvent implements Serializable {

    private static final long serialVersionUID = 7171591348961012216L;

    private Long shopId;

    /**
     * 是否是授权类目更新
     */
    private Boolean isAuthUpdated;

    private List<VegaCategoryDiscountDto> categoryDiscounts;

    public static CategoryAuthUpdateEvent form(Long shopId, Boolean isAuthUpdated,
                                               List<VegaCategoryDiscountDto> categoryDiscounts ){
        CategoryAuthUpdateEvent event = new CategoryAuthUpdateEvent();
        event.setShopId(shopId);
        event.setIsAuthUpdated(isAuthUpdated);
        event.setCategoryDiscounts(categoryDiscounts);
        return event;
    }
}
