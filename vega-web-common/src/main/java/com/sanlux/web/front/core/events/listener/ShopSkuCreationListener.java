package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.ShopSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.events.trade.OrderConfirmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 监听支付成功回调事件,创建店铺sku
 * Author:cp
 * Created on 8/9/16.
 */
@Component
@Slf4j
public class ShopSkuCreationListener {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onConfirm(OrderConfirmEvent orderConfirmEvent) throws Exception {
        OrderLevel orderLevel = OrderLevel.fromInt(orderConfirmEvent.getOrderType());
        final Long orderId = orderConfirmEvent.getOrderId();

        List<SkuOrder> skuOrders = findSkuOrders(orderLevel, orderId);
        final Long buyerId = skuOrders.get(0).getBuyerId();

        //如果不是经销商下的单,则直接略过
        if (!isDealer(buyerId)) {
            return;
        }

        Response<Shop> findShop = shopReadService.findByUserId(buyerId);
        if (!findShop.isSuccess()) {
            log.error("fail to find shop by userId={},cause:{},then create shopSku skipped for order(id={}),level={}",
                    buyerId, findShop.getError(), orderId, orderLevel);
            return;
        }
        final Long shopId = findShop.getResult().getId();

        for (SkuOrder skuOrder : skuOrders) {
            if (shopSkuExisted(shopId, skuOrder.getSkuId())) {
                updateShopSku(shopId, skuOrder);
            } else {
                createShopSku(shopId, skuOrder);
            }
        }
    }

    private List<SkuOrder> findSkuOrders(OrderLevel orderLevel, Long orderId) {
        switch (orderLevel) {
            case SHOP:
                return findSkuOrdersByShopOrderId(orderId);
            case SKU:
                return Arrays.asList(findSkuOrderById(orderId));
            default:
                log.error("unknown order level:{}", orderLevel);
                throw new IllegalArgumentException("unknown.order.level");
        }
    }

    private boolean isDealer(Long buyerId) {
        Response<User> findUser = userReadService.findById(buyerId);
        if (!findUser.isSuccess()) {
            log.error("fail to find user by id={},cause:{}",
                    buyerId, findUser.getError());
            throw new ServiceException(findUser.getError());
        }
        User user = findUser.getResult();

        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByRoles(user.getRoles());
        return orderUserType == OrderUserType.DEALER_FIRST || orderUserType == OrderUserType.DEALER_SECOND;
    }

    private List<SkuOrder> findSkuOrdersByShopOrderId(Long shopOrderId) {
        Response<List<SkuOrder>> findResp = skuOrderReadService.findByShopOrderId(shopOrderId);
        if (!findResp.isSuccess()) {
            log.error("fail to find sku orders by shopOrderId={},cause:{}",
                    shopOrderId, findResp.getError());
            throw new ServiceException(findResp.getError());
        }
        return findResp.getResult();
    }

    private SkuOrder findSkuOrderById(Long skuOrderId) {
        Response<SkuOrder> findResp = skuOrderReadService.findById(skuOrderId);
        if (!findResp.isSuccess()) {
            log.error("fail to find sku order by id={},cause:{}",
                    skuOrderId, findResp.getError());
            throw new ServiceException(findResp.getError());
        }
        return findResp.getResult();
    }

    private boolean shopSkuExisted(Long shopId, Long skuId) {
        Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId(shopId, skuId);
        if (!findShopSku.isSuccess()) {
            log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                    shopId, skuId, findShopSku.getError());
            throw new ServiceException(findShopSku.getError());
        }
        Optional<ShopSku> shopSkuOptional = findShopSku.getResult();
        return shopSkuOptional.isPresent();
    }

    private void updateShopSku(Long shopId, SkuOrder skuOrder) {
        Response<Boolean> updateResp = shopSkuWriteService.updateStockQuantity(shopId, skuOrder.getSkuId(), skuOrder.getQuantity());
        if (!updateResp.isSuccess()) {
            log.error("fail to update stock quantity,shopId={},skuId={},delta={},cause:{}",
                    shopId, skuOrder.getSkuId(), skuOrder.getQuantity(), updateResp.getError());
        }
    }

    private void createShopSku(Long shopId, SkuOrder skuOrder) throws Exception {
        Response<Sku> findSku = skuReadService.findSkuById(skuOrder.getSkuId());
        if (!findSku.isSuccess()) {
            log.error("fail to find sku by id={},cause:{},then create shopSku skipped for skuOrder(id={})",
                    skuOrder.getSkuId(), findSku.getError(), skuOrder.getId());
            return;
        }
        Sku sku = findSku.getResult();

        ShopSku shopSku = new ShopSku();
        shopSku.setShopId(shopId);
        shopSku.setItemId(sku.getItemId());
        shopSku.setSkuId(sku.getId());
        shopSku.setStatus(sku.getStatus());
        shopSku.setPrice(sku.getPrice());
        shopSku.setExtraPrice(sku.getExtraPrice());
        shopSku.setStockType(sku.getStockType());
        shopSku.setStockQuantity(skuOrder.getQuantity());

        Response<Long> createResp = shopSkuWriteService.create(shopSku);
        if (!createResp.isSuccess()) {
            log.error("fail to create shopSku:{} for skuOrder(id={}),cause:{}",
                    shopSku, skuOrder.getId(), createResp.getError());
        }
    }

}
