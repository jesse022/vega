package com.sanlux.web.front.controller.trade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.trade.dto.KdNiaoLogisticsDto;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.enums.VegaOrderChannelEnum;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.events.trade.VegaOrderShipmentEvent;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiOrderEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import com.sanlux.web.front.core.trade.service.LogisticsService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.express.model.ExpressCompany;
import io.terminus.parana.express.service.ExpressCompanyReadService;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.express.dto.OrderExpressTrack;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Author:songrenfei
 * Created on 5/26/16.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega")
public class VegaShipments {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;

    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @RpcConsumer
    private ExpressCompanyReadService expressCompanyReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @Autowired
    private FlowPicker flowPicker;

    @Autowired
    private EventBus eventBus;
    @Autowired
    private VegaOrderComponent vegaOrderComponent;

    @Autowired
    private LogisticsService logisticsService;

    private final static ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    //发货(先判断库存是否充足,库存不足不可发货)
    @RequestMapping(value = "/seller/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createShipment(@RequestParam("orderId") Long orderId,
                               @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                               @RequestParam("corpCode") String shipmentCorpCode,
                               @RequestParam("serialNo") String shipmentSerialNo,
                               @RequestParam(value = "extra", required = false) String extra,
                               @RequestParam(value = "annexUrl", required = false) String annexUrl) {

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase order = findOrder(orderId, orderLevel);
        checkSellerAuth(order);
        checkSkuStorage(order);
        checkIfCanShip(order, orderLevel);
        addAnnexInfo(order, orderLevel, annexUrl);
        return doCreateShipment(orderId, orderLevel, shipmentCorpCode, shipmentSerialNo, extra);
    }

    /**
     * 修改物流信息
     * add by lujm on 2017/4/1
     * @param shipmentId  发货单号
     * @param orderId  订单号
     * @param orderType 订单类型
     * @param shipmentCorpCode 物流公司编号
     * @param shipmentSerialNo 物流单号
     * @param extra 备注
     * @param annexUrl 附件地址
     * @return 是否成功
     */
    @RequestMapping(value = "/seller/shipment/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateShipment(@RequestParam("shipmentId") Long shipmentId,
                                  @RequestParam("orderId") Long orderId,
                                  @RequestParam(value = "orderType", defaultValue = "1",required = false) Integer orderType,
                                  @RequestParam("corpCode") String shipmentCorpCode,
                                  @RequestParam("serialNo") String shipmentSerialNo,
                                  @RequestParam(value = "extra", required = false) String extra,
                                  @RequestParam(value = "annexUrl", required = false) String annexUrl) {

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase order = findOrder(orderId, orderLevel);
        addAnnexInfo(order, orderLevel, annexUrl);//修改附件
        return doUpdateShipment(shipmentId,shipmentCorpCode, shipmentSerialNo, extra);
    }

    /**
     * 查询物流信息
     * add by lujm on 2017/4/5
     * @param orderId  订单号
     * @param orderType 订单类型
     * @return 发货单信息
     */
    @RequestMapping(value = "/seller/shipment/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> listShipment(@RequestParam("orderId") Long orderId,
                                  @RequestParam(value = "orderType", defaultValue = "1",required = false) Integer orderType) {

        OrderLevel orderLevel = OrderLevel.fromInt(MoreObjects.firstNonNull(orderType, 1));
        Response<List<Shipment>> shipmentResp =
                shipmentReadService.findByOrderIdAndOrderLevel(orderId, orderLevel);
        if (!shipmentResp.isSuccess()) {
            log.error("fail to find shipment by order id={} and order lever={},cause:{}",
                    orderId, orderLevel, shipmentResp.getError());
            throw new JsonResponseException(shipmentResp.getError());
        }
        return shipmentResp.getResult();
    }

    /**
     * 订单添加附件信息
     *
     * @param order    订单
     * @param annexUrl 附件信息
     */
    private void addAnnexInfo(OrderBase order, OrderLevel orderLevel, String annexUrl) {
        if (!Strings.isNullOrEmpty(annexUrl)) {
            Map<String, String> extra = order.getExtra() == null ? Maps.newHashMap() : order.getExtra();
            extra.put("annexUrl", annexUrl);
            Response<Boolean> resp = orderWriteService.updateOrderExtra(order.getId(), orderLevel, extra);
            if (!resp.isSuccess()) {
                log.error("failed to set annexUrl by orderId = {}, orderLevel = {}, annexUrl = {}, cause : {}",
                        order.getId(), orderLevel.getValue(), annexUrl, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
        }
    }

  /*  @RequestMapping(value = "/buyer/confirm", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean confirm(@RequestParam("orderId") Long orderId,
                           @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase order = findOrder(orderId, orderLevel);
        checkBuyerAuth(order);

        Response<Boolean> updateResp = shipmentWriteService.updateStatusByOrderIdAndOrderLevel(orderId, orderLevel, 2);
        if (!updateResp.isSuccess()) {
            log.error("fail to update shipment status by order id:{} and order level:{},cause:{}",
                    orderId, orderLevel, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }

        eventBus.post(new OrderConfirmEvent(orderId, orderType));
        return updateResp.getResult();
    }*/

    /**
     * 发货
     *
     * @param orderId          订单ID
     * @param orderLevel       订单类型
     * @param shipmentCorpCode 物流公司编号
     * @param shipmentSerialNo 物流单号
     * @return 发货信息ID
     */
    private Long doCreateShipment(Long orderId, OrderLevel orderLevel, String shipmentCorpCode, String shipmentSerialNo,String extra) {
        Map<Long, Integer> skuInfos = findSkuInfos(orderId, orderLevel);

        Response<ExpressCompany> expressCompanyResp = expressCompanyReadService.findExpressCompanyByCode(shipmentCorpCode);
        if (!expressCompanyResp.isSuccess()) {
            log.error("fail to find express company by code:{},cause:{}", shipmentCorpCode, expressCompanyResp.getError());
            throw new JsonResponseException(expressCompanyResp.getError());
        }
        ExpressCompany expressCompany = expressCompanyResp.getResult();

        Shipment shipment = new Shipment();
        shipment.setShipmentSerialNo(shipmentSerialNo);
        shipment.setShipmentCorpCode(shipmentCorpCode);
        shipment.setShipmentCorpName(expressCompany.getName());
        if(!Strings.isNullOrEmpty(extra)){
            Map<String, String> extraMap=Maps.newHashMap();
            extraMap.put(SystemConstant.SHIPMENT_EXTRA_COMMENT,extra);
            shipment.setExtra(extraMap);
        }
        shipment.setSkuInfos(skuInfos);
        shipment.setReceiverInfos(findReceiverInfos(orderId, orderLevel));

        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(orderId), orderLevel);
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, orderId, orderLevel.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        Long shipmentId = createResp.getResult();

        eventBus.post(new VegaOrderShipmentEvent(shipmentId));
        Response<Long> orderIdRes = vegaOrderComponent.getShopOrderId(orderId, orderLevel.getValue());
        if (!orderIdRes.isSuccess()) {
            log.error("send sms message fail,because shop order id not found by order id:{} order type:{}", orderId, orderLevel.getValue());
        } else {

            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderIdRes.getResult(), expressCompany.getName(), shipmentSerialNo, TradeSmsNodeEnum.SHIPPED));

            Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(orderIdRes.getResult());
            if (shopOrderRes.isSuccess() && Objects.equal(shopOrderRes.getResult().getChannel(), VegaOrderChannelEnum.YOU_YUN_CAI.value())) {
                // 友云采订单提交出货通知接口
                eventBus.post(VegaYouyuncaiOrderEvent.formOrderShopInfo(orderIdRes.getResult(), expressCompany.getName(), shipmentSerialNo));
            }

        }
        return shipmentId;
    }

    /**
     * 物流信息修改
     *
     * @param shipmentId       发货单ID
     * @param shipmentCorpCode 物流公司编号
     * @param shipmentSerialNo 物流单号
     * @return 发货信息ID
     */
    private Boolean doUpdateShipment(Long shipmentId, String shipmentCorpCode, String shipmentSerialNo, String extra) {
        Response<ExpressCompany> expressCompanyResp = expressCompanyReadService.findExpressCompanyByCode(shipmentCorpCode);
        if (!expressCompanyResp.isSuccess()) {
            log.error("fail to find express company by code:{},cause:{}", shipmentCorpCode, expressCompanyResp.getError());
            throw new JsonResponseException(expressCompanyResp.getError());
        }
        ExpressCompany expressCompany = expressCompanyResp.getResult();

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setShipmentSerialNo(shipmentSerialNo);
        shipment.setShipmentCorpCode(shipmentCorpCode);
        if(!Strings.isNullOrEmpty(extra)){
            Response<Shipment> shipmentResponse = shipmentReadService.findById(shipmentId);
            if (!shipmentResponse.isSuccess()) {
                log.error("shipment find failed , id {}", shipmentId);
                throw new JsonResponseException(shipmentResponse.getError());
            }
            Map<String,String> extraMap = shipmentResponse.getResult().getExtra();
            if(extraMap==null){
                extraMap = Maps.newHashMap();
            }
            extraMap.put(SystemConstant.SHIPMENT_EXTRA_COMMENT,extra);
            shipment.setExtra(extraMap);
        }
        shipment.setShipmentCorpName(expressCompany.getName());
        Response<Boolean> updateResp = shipmentWriteService.update(shipment);
        if (!updateResp.isSuccess()) {
            log.error("fail to update shipment:{} for order(shipment={}),cause:{}",
                    shipment, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }

    private Map<Long, Integer> findSkuInfos(Long orderId, OrderLevel orderLevel) {
        Map<Long, Integer> skuInfos = new HashMap<>();
        switch (orderLevel) {
            case SHOP:
                Response<List<SkuOrder>> skuOrdersResp = skuOrderReadService.findByShopOrderId(orderId);
                if (!skuOrdersResp.isSuccess()) {
                    log.error("fail to find sku orders by shop order id:{},cause:{}", orderId, skuOrdersResp.getError());
                    throw new JsonResponseException(skuOrdersResp.getError());
                }
                List<SkuOrder> skuOrders = skuOrdersResp.getResult();
                for (SkuOrder skuOrder : skuOrders) {
                    skuInfos.put(skuOrder.getSkuId(), skuOrder.getQuantity());
                }
                break;
            case SKU:
                SkuOrder skuOrder = findSkuOrder(orderId);
                skuInfos.put(skuOrder.getSkuId(), skuOrder.getQuantity());
                break;
        }

        return skuInfos;
    }

    private ShopOrder findShopOrder(Long orderId) {
        Response<ShopOrder> shopOrderResp = shopOrderReadService.findById(orderId);
        if (!shopOrderResp.isSuccess()) {
            log.error("fail to find shop order by id:{},cause:{}", orderId, shopOrderResp.getError());
            throw new JsonResponseException(shopOrderResp.getError());
        }
        return shopOrderResp.getResult();
    }

    private SkuOrder findSkuOrder(Long orderId) {
        Response<SkuOrder> skuOrderResp = skuOrderReadService.findById(orderId);
        if (!skuOrderResp.isSuccess()) {
            log.error("fail to find sku order by sku order id:{},cause:{}", orderId, skuOrderResp.getError());
            throw new JsonResponseException(skuOrderResp.getError());
        }
        return skuOrderResp.getResult();
    }

    private OrderBase findOrder(Long orderId, OrderLevel orderLevel) {
        return orderLevel == OrderLevel.SHOP ? findShopOrder(orderId) : findSkuOrder(orderId);
    }

    private void checkSellerAuth(OrderBase order) {
        ParanaUser loginUser = UserUtil.getCurrentUser();
        if (!Objects.equal(loginUser.getShopId(), order.getShopId())) {
            throw new JsonResponseException("order.not.belong.to.seller");
        }
    }

    /**
     * 判断库存
     * 供应商发货 判断商品真实库存
     * 经销商 判断经销商商品库存
     *
     * @param order 订单
     */
    private void checkSkuStorage(OrderBase order) {

        Shop shop = getShopById(order.getShopId());
        List<SkuOrder> skuOrders = getSkuOrdersByOrderId(order.getId());
        // 待发货节点,其他节点不进行库存判断
        List<Integer> waitShippList = ImmutableList.of(
                VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),
                VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),
                VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue(),
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue(),
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue(),
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue()
        );

        //供应商发货
        if (shop.getType().equals(VegaShopType.SUPPLIER.value())) {
            for (SkuOrder skuOrder : skuOrders) {
                if (!waitShippList.contains(skuOrder.getStatus())) {
                    continue;
                }
                Sku sku = findSkuById(skuOrder.getSkuId());
                if (sku.getStockQuantity() < skuOrder.getQuantity()) {
                    Item item = findItemById(sku.getItemId());
                    throw new InvalidException(500, "item.id.{0}.stock.quantity.not.enough", String.valueOf(item.getId()));
                }
            }
        } else {

            for (SkuOrder skuOrder : skuOrders) {
                if (!waitShippList.contains(skuOrder.getStatus())) {
                    continue;
                }
                ShopSku shopSku = findSkuByIdAndShopId(skuOrder.getSkuId(), skuOrder.getShopId());
                if (shopSku.getStockQuantity() < skuOrder.getQuantity()) {
                    Item item = findItemById(shopSku.getItemId());
                    throw new InvalidException(500, "item.id.{0}.stock.quantity.not.enough", String.valueOf(item.getId()));
                }
            }
        }

    }

    private Shop getShopById(Long shopId) {
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }


    private boolean isDealerFirst(Long buyerId) {
        Response<User> findUser = userReadService.findById(buyerId);
        if (!findUser.isSuccess()) {
            log.error("fail to find user by id={},cause:{}",
                    buyerId, findUser.getError());
            throw new JsonResponseException(findUser.getError());
        }
        User user = findUser.getResult();

        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByRoles(user.getRoles());
        return orderUserType == OrderUserType.DEALER_FIRST;
    }

    private void checkBuyerAuth(OrderBase order) {
        ParanaUser loginUser = UserUtil.getCurrentUser();
        if (!Objects.equal(loginUser.getId(), order.getBuyerId())) {
            throw new JsonResponseException("order.not.belong.to.buyer");
        }
    }

    private void checkIfCanShip(OrderBase order, OrderLevel orderLevel) {
        Flow flow = flowPicker.pick(order, orderLevel);
        if (!flow.operationAllowed(order.getStatus(), VegaOrderEvent.SHIP.toOrderOperation())) {
            log.error("can not do ship operation at current status:{} where order id={},and level={}",
                    order.getStatus(), order.getId(), orderLevel.getValue());
            throw new JsonResponseException("can.not.do.ship");
        }
    }

    private String findReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Long orderIdOfFindReceiverInfo = orderId;
        OrderLevel orderLevelOfFindReceiverInfo = orderLevel;

        List<ReceiverInfo> receiverInfos = doFindReceiverInfos(orderId, orderLevel);
        //找不到子订单收货地址,则用总订单的
        if (CollectionUtils.isEmpty(receiverInfos) && orderLevel == OrderLevel.SKU) {
            orderIdOfFindReceiverInfo = findSkuOrder(orderId).getOrderId();
            orderLevelOfFindReceiverInfo = OrderLevel.SHOP;
            receiverInfos = doFindReceiverInfos(orderIdOfFindReceiverInfo, orderLevelOfFindReceiverInfo);
        }

        if (CollectionUtils.isEmpty(receiverInfos)) {
            log.error("receiverInfo not found where orderId={}", orderId);
            throw new JsonResponseException("receiver.info.not.found");
        }

        if (receiverInfos.size() > 1) {
            log.error("the order(id={},level={}) should has a receiver only,but {} found",
                    orderIdOfFindReceiverInfo, orderLevelOfFindReceiverInfo.getValue(), receiverInfos.size());
            throw new JsonResponseException("too.many.receiver");
        }
        ReceiverInfo receiverInfo = receiverInfos.get(0);

        try {
            return objectMapper.writeValueAsString(receiverInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                    orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }

    private List<SkuOrder> getSkuOrdersByOrderId(Long orderId) {
        Response<List<SkuOrder>> findSkuOrders = skuOrderReadService.findByShopOrderId(orderId);
        if (!findSkuOrders.isSuccess()) {
            log.error("fail to find sku orders by shopId={},cause:{}",
                    orderId, findSkuOrders.getError());
            throw new JsonResponseException(findSkuOrders.getError());
        }
        return findSkuOrders.getResult();
    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
    }

    private ShopSku findSkuByIdAndShopId(Long skuId, Long shopId) {
        Response<Optional<ShopSku>> rSku = shopSkuReadService.findByShopIdAndSkuId(shopId, skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        if (!rSku.getResult().isPresent()) {
            Response<Sku> skuResp = skuReadService.findSkuById(skuId);
            if (!skuResp.isSuccess()) {
                log.error("fail to find sku by id:{}, cause:{}", skuId, skuResp.getError());
                throw new JsonResponseException(skuResp.getError());
            }
            throw new InvalidException(500, "shop.item.{0}.not.exist", String.valueOf(skuResp.getResult().getItemId()));
        }
        return rSku.getResult().get();
    }

    private Item findItemById(Long itemId) {
        Response<Item> itemRes = itemReadService.findById(itemId);
        if (!itemRes.isSuccess()) {
            log.error("find item by id:{} fail,error:{}", itemId, itemRes.getError());
            throw new JsonResponseException(itemRes.getError());
        }

        return itemRes.getResult();
    }


    /**
     * 获取快递信息
     *
     * @param orderId   订单ID
     * @param orderType 订单类型
     * @return 快递的JSON信息
     */
    @RequestMapping(value = "/logistics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<KdNiaoLogisticsDto> findLogisticsInfo(@RequestParam("orderId") Long orderId, @RequestParam("orderType") Integer orderType) {

        List<OrderExpressTrack> lists = logisticsService.findExpressTrack(orderId, orderType);
        if (Arguments.isNull(lists)) {
            log.error("logistics info is null,orderId{},orderType{}", orderId, orderType);
            throw new JsonResponseException("logistics.info.null");
        }
        List<KdNiaoLogisticsDto> logisticsDtoList = Lists.newArrayList();
        try {
            for (OrderExpressTrack orderExpressTrack : lists) {
                KdNiaoLogisticsDto kdNiaoLogisticsDto = new KdNiaoLogisticsDto();
                kdNiaoLogisticsDto.setShipmentCorpName(orderExpressTrack.getShipmentCorpName());
                kdNiaoLogisticsDto.setShipmentId(orderExpressTrack.getShipmentId());
                kdNiaoLogisticsDto.setShipmentSerialNo(orderExpressTrack.getShipmentSerialNo());

                Response<Shipment> shipmentResponse = shipmentReadService.findById(kdNiaoLogisticsDto.getShipmentId());
                if (!shipmentResponse.isSuccess()) {
                    log.error("shipment find failed , id {}", kdNiaoLogisticsDto.getShipmentId());
                    throw new JsonResponseException(shipmentResponse.getError());
                }
                String shipmentCorpCode = shipmentResponse.getResult().getShipmentCorpCode();
                String steps = logisticsService.getLogisticsInfo(shipmentCorpCode, kdNiaoLogisticsDto.getShipmentSerialNo());
                Map<String,String> mapExtra = shipmentResponse.getResult().getExtra();
                if(mapExtra!=null){
                    String extra = mapExtra.get(SystemConstant.SHIPMENT_EXTRA_COMMENT);
                    kdNiaoLogisticsDto.setExtra(extra);
                }
                kdNiaoLogisticsDto.setSteps(steps);

                logisticsDtoList.add(kdNiaoLogisticsDto);
            }
            return logisticsDtoList;


        } catch (Exception e) {
            log.error("find logistics info failed,,cause{}", e);
            throw new JsonResponseException("find.logistics.failed");
        }
    }

}
