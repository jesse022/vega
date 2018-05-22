package com.sanlux.web.admin.user;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.model.DeliveryScope;
import com.sanlux.user.service.DeliveryScopeReadService;
import com.sanlux.user.service.DeliveryScopeWriteService;
import com.sanlux.web.front.core.events.DeliveryScopeUpdateEvent;
import com.sanlux.web.front.core.events.FirstShopCreateScopeOrAuthEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * Created by cuiwentao
 * on 16/8/8
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/delivery/scope")
public class AdminDeliveryScope {

    @RpcConsumer
    private DeliveryScopeReadService deliveryScopeReadService;

    @RpcConsumer
    private DeliveryScopeWriteService deliveryScopeWriteService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @Autowired
    private EventBus eventBus;


    @RequestMapping(value = "", method = RequestMethod.GET)
    public DeliveryScope readScopeByShopId(@RequestParam Long shopId){

        Response<Optional<DeliveryScope>> readScopeResp = deliveryScopeReadService.findDeliveryScopeByShopId(shopId);
        if (!readScopeResp.isSuccess()) {
            log.error("read scope fail, shopId:{}, cause:{}", shopId, readScopeResp.getError());
            throw new JsonResponseException(readScopeResp.getError());
        }

        if(readScopeResp.getResult().isPresent()) {
            return readScopeResp.getResult().get();
        } else {
            log.debug("delivery scope is empty, shopId:{}", shopId);
            return null;
        }
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long writeScope(@RequestBody DeliveryScope scope){

        Long shopId = scope.getShopId();
        Boolean isCreate = Boolean.FALSE;

        if (scope.getId() != null) {
            eventBus.post(DeliveryScopeUpdateEvent.from(shopId));
        }else {
            isCreate = Boolean.TRUE;
        }
        Response<VegaShop> vegaShopResponse = vegaShopReadService.findByShopId(shopId);
        if (!vegaShopResponse.isSuccess()) {
            log.error("write scope fail, shopId:{}, cause:{}", shopId, vegaShopResponse.getError());
            throw new JsonResponseException(vegaShopResponse.getError());
        }
        scope.setShopName(vegaShopResponse.getResult().getShop().getName());
        Response<Long> resp = deliveryScopeWriteService.createOrUpdateDeliveryScope(scope);
        if (!resp.isSuccess()) {
            log.error("write scope fail, shopId:{}, cause:{}", scope.getShopId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        if (isCreate) {
            eventBus.post(FirstShopCreateScopeOrAuthEvent.from(shopId));
        }
        return resp.getResult();

    }

}
