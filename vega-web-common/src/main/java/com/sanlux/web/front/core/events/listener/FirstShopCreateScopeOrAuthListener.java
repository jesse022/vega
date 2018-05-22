package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.user.model.DeliveryScope;
import com.sanlux.user.service.DeliveryScopeReadService;
import com.sanlux.web.front.core.events.FirstShopCreateScopeOrAuthEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by cuiwentao
 * on 16/9/20
 */
@Component
@Slf4j
public class FirstShopCreateScopeOrAuthListener {

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private DeliveryScopeReadService deliveryScopeReadService;

    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onFirstShopCreateScopeOrAuthEvent (FirstShopCreateScopeOrAuthEvent event) {

        Long shopId = event.getShopId();

        if (hasCreateCategoryAuth(shopId) && hasCreateDeliveryScope(shopId)) {
            vegaShopWriteService.changeShopStatusById(shopId, VegaShopStatus.NORMAL.value(), null);
        }
    }


    private Boolean hasCreateDeliveryScope (Long shopId) {

        Response<Optional<DeliveryScope>> response = deliveryScopeReadService.findDeliveryScopeByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find delivery scope by shopId:{} fail, cause:{}", shopId, response.getError());
            return Boolean.FALSE;
        }
        if (response.getResult().isPresent() && Arguments.notNull(response.getResult().get())
                && !Strings.isNullOrEmpty(response.getResult().get().getScope())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    private Boolean hasCreateCategoryAuth (Long shopId) {

        Response<Optional<CategoryAuthe>> response = categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find category auth by shopId:{} fail, cause:{}", shopId, response.getError());
            return Boolean.FALSE;
        }
        if (response.getResult().isPresent() && Arguments.notNull(response.getResult().get())
                && !Strings.isNullOrEmpty(response.getResult().get().getCategoryDiscountList())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
