package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.ShopSkuWriteService;
import com.sanlux.item.service.VegaItemWriteService;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.web.front.core.events.trade.VegaOrderShipmentEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 发货减库存 总单级别的发货
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class VegaStorageDecreaseListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private VegaItemWriteService vegaItemWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;
    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void decrStorageAndIncrSale(VegaOrderShipmentEvent orderShipmentEvent) {

        Long shipmentId = orderShipmentEvent.getShipmentId();
        Response<List<OrderShipment>> rOrderShipments = shipmentReadService.findOrderIdsByShipmentId(shipmentId);
        if (!rOrderShipments.isSuccess()) {
            log.error("failed to find orderIds for shipment(id={}), error code:{}", shipmentId, rOrderShipments.getError());
            return;
        }
        List<OrderShipment> orderShipments = rOrderShipments.getResult();
        //总单级别的发货 所以这里只会有一条发货单
        OrderShipment orderShipment = orderShipments.get(0);

        Long shopOrderId = orderShipment.getOrderId();

        Response<List<SkuOrder>> findSkuOrders = skuOrderReadService.findByShopOrderId(shopOrderId);
        if (!findSkuOrders.isSuccess()) {
            log.error("fail to find sku orders by shopId={},cause:{}",
                    shopOrderId, findSkuOrders.getError());
            return;
        }
        List<SkuOrder> skuOrders = findSkuOrders.getResult();

        for (SkuOrder skuOrder : skuOrders) {
            // 待发货节点,其他节点不减库存
            List<Integer> waitShippList = ImmutableList.of(
                    VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                    VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue(),
                    VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue()
            );
            if (!waitShippList.contains(skuOrder.getStatus())) {
                continue;
            }

            Shop shop = findShopById(skuOrder.getShopId());

            //供应商发货
            if(ShopTypeHelper.isSupplierShop(shop.getType())){
                Response<Boolean> deltaR = vegaItemWriteService.updateStockQuantity(skuOrder.getSkuId(), skuOrder.getQuantity());
                if (!deltaR.isSuccess()) {
                    log.error("fail to decrease sku(id={})'s storage, delta={}, error code:{}",
                            skuOrder.getSkuId(), skuOrder.getQuantity(), deltaR.getError());
                }
            }else {
                //经销商发货
                if (shopSkuExisted(skuOrder.getShopId(), skuOrder.getSkuId())) {
                    updateShopSku(skuOrder.getShopId(), skuOrder);
                } else {
                    log.warn("shop id:{} not has sku id:{},so skip to decrease stock :{}",skuOrder.getShopId(),skuOrder.getSkuId(),skuOrder.getQuantity());
                }

            }

            //普通用户增加商品销量
            if(isNormalUser(skuOrder.getBuyerId())){
                Response<Boolean> deltaR = vegaItemWriteService.updateSaleQuantity(skuOrder.getSkuId(), skuOrder.getQuantity());
                if (!deltaR.isSuccess()) {
                    log.error("fail to increase sku(id={})'s sales, delta={}, error code:{}",
                            skuOrder.getSkuId(), skuOrder.getQuantity(), deltaR.getError());
                }
            }
        }


    }

    private boolean isDealerFirst(Long buyerId) {
        Response<User> findUser = userReadService.findById(buyerId);
        if (!findUser.isSuccess()) {
            log.error("fail to find user by id={},cause:{}",
                    buyerId, findUser.getError());
            throw new ServiceException(findUser.getError());
        }
        User user = findUser.getResult();

        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByRoles(user.getRoles());
        return orderUserType == OrderUserType.DEALER_FIRST;
    }

    private boolean isNormalUser(Long buyerId) {
        Response<User> findUser = userReadService.findById(buyerId);
        if (!findUser.isSuccess()) {
            log.error("fail to find user by id={},cause:{}",
                    buyerId, findUser.getError());
            throw new ServiceException(findUser.getError());
        }
        User user = findUser.getResult();

        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByRoles(user.getRoles());
        if(Objects.equals(orderUserType, OrderUserType.SUPPLIER) ||
                Objects.equals(orderUserType, OrderUserType.SERVICE_MANAGER)){
            orderUserType = OrderUserType.NORMAL_USER;
        }
        return orderUserType == OrderUserType.NORMAL_USER;
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
        Response<Boolean> updateResp = shopSkuWriteService.updateStockQuantity(shopId, skuOrder.getSkuId(), getNegateValue(skuOrder.getQuantity()));
        if (!updateResp.isSuccess()) {
            log.error("fail to update stock quantity,shopId={},skuId={},delta={},cause:{}",
                    shopId, skuOrder.getSkuId(), skuOrder.getQuantity(), updateResp.getError());
        }
    }



    /**
     * 查找店铺
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        Response<Shop> resp = shopReadService.findById(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    //取相反数
    private Integer getNegateValue(Integer value){
       return BigDecimal.valueOf(value).negate().intValue();
    }
}
