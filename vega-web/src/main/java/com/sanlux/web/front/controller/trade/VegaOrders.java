/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.trade;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaNoteType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.trade.component.WaitHandleAlert;
import com.sanlux.trade.dto.OrderDispatchCriteria;
import com.sanlux.trade.dto.OrderDispatchDetail;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.service.OrderDispatchRelationReadService;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.trade.service.VegaOrderWriteService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import com.sanlux.web.front.core.events.VegaFirstShopDispatchEvent;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.events.trade.VegaOrderCreatedEvent;
import com.sanlux.web.front.core.settlement.event.VegaOrderAcceptEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import com.sanlux.web.front.core.trade.VegaOrderReadLogic;
import com.sanlux.web.front.core.trade.VegaOrderWriteLogic;
import com.sanlux.web.front.service.PurchaseOrderService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.order.dto.*;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.rule.OrderRuleEngine;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.promotion.component.CouponUsageChecker;
import io.terminus.parana.promotion.component.OrderCharger;
import io.terminus.parana.promotion.component.PromotionOngoingValidator;
import io.terminus.parana.promotion.model.Promotion;
import io.terminus.parana.promotion.service.PromotionReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.util.ParanaUserMaker;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.session.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;

/**
 * 订单
 * <p>
 * Author:  songrenfei
 * Date: 2016-04-21
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/vega/order")
public class VegaOrders {

    @Autowired
    private RichOrderMaker richOrderMaker;

    @Autowired
    private OrderRuleEngine orderRuleEngine;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private OrderReadService orderReadService;

    @Autowired
    private OrderCharger charger;
    @Autowired
    private ReceiveShopParser receiveShopParser;

    @Autowired
    private PromotionOngoingValidator promotionOngoingValidator;

    @Autowired
    private DeliveryFeeCharger deliveryFeeCharger;

    @RpcConsumer
    private ShopReadService shopReadService;

    @Autowired
    private VegaOrderReadLogic orderReadLogic;

    @Autowired
    private VegaOrderWriteLogic vegaOrderWriteLogic;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private CouponUsageChecker couponUsageChecker;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;


    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;

    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @RpcConsumer
    private PaymentReadService paymentReadService;

    @Autowired
    private FlowPicker flowPicker;
    @Autowired
    private VegaOrderComponent vegaOrderComponent;
    @RpcConsumer
    private PromotionReadService promotionReadService;

    /**
     * 创建订单
     * SubmittedOrder.submittedSkusByShops.shopId(二级下单或一级下单则为真实的接单店铺id,一级下单则为商品真实的店铺)
     *
     * @param submittedOrder  订单基本信息
     * @param purchaseOrderId 采购单id
     * @param receiveShopId   接单店铺id
     * @return 订单id
     */
    @RequestMapping(value = "/purchase-order/{purchaseOrderId}/receive-shop/{shopId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Long> create(@RequestBody SubmittedOrder submittedOrder, @PathVariable(value = "purchaseOrderId") Long purchaseOrderId, @PathVariable(value = "shopId") Long receiveShopId) {

        try {

            ParanaUser paranaUser = UserUtil.getCurrentUser();

            //获取当前登录用户身份
            String roleName = getUserRoleName(paranaUser);



            //组装商品信息
            assemblySubmittedSkusByShop(submittedOrder, purchaseOrderId, receiveShopId);


            RichOrder richOrder = richOrderMaker.full(submittedOrder, getUserById());



            /**
             * 修改sku价格为最终价格 并把供货价放在extraPrice Map中
             * 修改sku库存(由于下单不判断库存,为了兼容现有的下单检查规则这里强制把sku库存塞为下单数量确保不会出现库存不足的情况)
             */
            changeSkuPriceAndStock(richOrder, paranaUser, receiveShopId);

            //检查用户是否具有购买资格
            orderRuleEngine.canBuy(richOrder);

            //检查商家营销活动的有效性
            promotionOngoingValidator.validate(richOrder);

            //检查优惠券是否符合使用规则
            couponUsageChecker.check(richOrder);

            //检查用户是否可以享受对应的营销, 如果可以, 则计算营销, 需要实付的金额, 以及运费等
            charger.charge(richOrder, null);

            //计算运费,考虑到有运费营销,这个可能要放到charger.charge之前
            deliveryFeeCharger.charge(richOrder.getRichSkusByShops(),richOrder.getReceiverInfo());

            //运费满多少包邮业务逻辑处理,临时解决方案
            changeOrderShipFee(richOrder);

            Shop platformShop = getShopById(DefaultId.PLATFROM_SHOP_ID);
            Map<String,String> extra = richOrder.getExtra();
            if(CollectionUtils.isEmpty(extra)){
                extra = Maps.newHashMap();
            }
            extra.put(SystemConstant.PLATFORM_FORM_SHOP_NAME,platformShop.getName());
            richOrder.setExtra(extra);

            Response<List<Long>> rOrder = orderWriteService.create(richOrder);

            if (!rOrder.isSuccess()) {
                log.error("failed to create {}, error code:{}", submittedOrder, rOrder.getError());
                throw new JsonResponseException(rOrder.getError());
            }

            final List<Long> shopOrderIds = rOrder.getResult();
            for (Long shopOrderId : shopOrderIds) {
                //抛出事件
                eventBus.post(new VegaOrderCreatedEvent(shopOrderId, roleName, purchaseOrderId));
                //短信提醒事件
                eventBus.post(new TradeSmsEvent(shopOrderId, TradeSmsNodeEnum.CREATE));
            }
            return shopOrderIds;
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("failed to create {}, cause:{}", submittedOrder, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.create.fail");
        }
    }


    /**
     * 普通用户取消订单(未支付)
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @return 是否操作成功
     */

    @RequestMapping(value = "/buyer/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean buyerCancelOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getId(), orderBase.getBuyerId())) {
            log.error("the order(id={},type={}) not belong to buyer(id={})",
                    orderId, orderType, paranaUser.getId());
            throw new JsonResponseException("order.not.belong.to.buyer");
        }

        Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.BUYER_CANCEL);

        if (isSuccess) {
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.BUYER_CANCEL));
        }

        return isSuccess;
    }

    /**
     * 普通用户删除订单(买家取消/超时关闭)
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @return 是否操作成功
     */

    @RequestMapping(value = "/buyer/delete", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean buyerDeleteOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "orderType", required = false, defaultValue = "1") Integer orderType) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getId(), orderBase.getBuyerId())) {
            log.error("the order(id={},type={}) not belong to buyer(id={})",
                    orderId, orderType, paranaUser.getId());
            throw new JsonResponseException("order.not.belong.to.buyer");
        }
        if(!ImmutableList.of(
                VegaOrderStatus.BUYER_CANCEL.getValue(),
                VegaOrderStatus.TIMEOUT_CANCEL.getValue(),
                VegaOrderStatus.TIMEOUT_FIRST_DEALER_CANCEL.getValue(),
                VegaOrderStatus.TIMEOUT_SECOND_DEALER_CANCEL.getValue()).contains(orderBase.getStatus())){
            log.info("failed to delete order id:{},userId:{}", orderId, paranaUser.getId());
            throw new JsonResponseException("order.status.buyer.can.not.delete");
        }

        Response<Boolean> handleRes = orderWriteService.shopOrderStatusChanged(orderId,
                orderBase.getStatus(),
                VegaOrderStatus.BUYER_DELETE.getValue());
        if(!handleRes.isSuccess()){
            log.info("failed to delete order id:{},userId:{}, cause:{}", orderId, paranaUser.getId(), handleRes.getError());
            throw new JsonResponseException("order.buyer.delete.fail");
        }
        return handleRes.getResult();
    }

    /**
     * 级经销商取消订单(未支付)
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @param sellerNote 商家备注
     * @return 是否操作成功
     */
    @RequestMapping(value = "/seller/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean sellerCancelOrder(@RequestParam("orderId") Long orderId,
                                     @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                     @RequestParam(required = false) String sellerNote) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getShopId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to seller(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.seller");
        }

        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(paranaUser);

        vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注
        //一级取消
        if (OrderUserType.DEALER_FIRST.equals(userType)) {

            Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.FIRST_DEALER_CANCEL);
            if (isSuccess) {
                //短信提醒事件
                eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.SELLER_CANCEL));
            }
            return isSuccess;
        }
        //二级取消
        if (OrderUserType.DEALER_SECOND.equals(userType)) {

            Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.SECOND_DEALER_CANCEL);
            if (isSuccess) {
                //短信提醒事件
                eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.SELLER_CANCEL));
            }
            return isSuccess;
        }



        return Boolean.FALSE;

    }


    /**
     * 经销商拒绝派给自己的单（shopId需要回滚）
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @param sellerNote 卖家备注
     * @return 是否操作成功
     */
    @RequestMapping(value = "/seller/reject-dispatch", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean sellerRejectDispatchOrder(@RequestParam("orderId") Long orderId,
                                             @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                             @RequestParam(required = false) String sellerNote) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getShopId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to seller(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.seller");
        }

        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(paranaUser);

        //短信提醒事件
        eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.DISPATCHER_REJECT));

        vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注

        if (OrderUserType.DEALER_FIRST.equals(userType)) {

            return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.FIRST_DEALER_REJECT_RECEIVE);
        }

        return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.SECOND_DEALER_REJECT_RECEIVE);

    }


    /**
     * 供应商拒绝订单(已支付) 订单关闭
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @param sellerNote 卖家备注
     * @return 是否操作成功
     */
    @RequestMapping(value = "/seller/reject-order", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean sellerRejectOrder(@RequestParam("orderId") Long orderId,
                                     @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                     @RequestParam(required = false) String sellerNote) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getShopId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to seller(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.seller");
        }

        //短信提醒事件
        eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.REJECT));
        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(paranaUser);
        if (OrderUserType.SUPPLIER.equals(userType)
                || OrderUserType.DEALER_FIRST.equals(userType)
                || OrderUserType.DEALER_SECOND.equals(userType)) {
            // 生成正逆向结算订单明细
            Response<List<Payment>> resp = paymentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.fromInt(orderType));
            List<Payment> paymentList = resp.getResult();
            Payment payment = null;
            for (Payment p : paymentList) {
                if (Objects.equal(p.getStatus(), 1)) {
                    payment = p;
                    break;
                }
            }
            if (payment == null) {
                log.error("failed to generate settle forward details, cause payment is null, " +
                        "order id = {}, order level = {}", orderId, orderLevel);
            }else {
                String channel = payment.getChannel();
                Long paymentId = payment.getId();
                String tradeNo = payment.getOutId();
                String paymentCode = payment.getPaySerialNo();
                Date paidAt = payment.getPaidAt();

                log.info("供应商或者经销商拒绝接单，生成对应的正向订单明细. userType = {}, payment = {}", userType, payment);

                eventBus.post(new VegaOrderAcceptEvent(orderId, channel, paymentId, tradeNo, paymentCode, paidAt, null));
            }
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.DISPATCHER_REJECT));
        }

        vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注

        return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.SELLER_REJECT);
    }


    /**
     * 一级经销商派单
     *
     * @param orderId 订单id
     * @param shopId  要派给某个店铺id
     * @return 是否派送成功
     */
    @RequestMapping(value = "/dispatch/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean dispatchOrder(@PathVariable(value = "id") Long orderId,
                                 @RequestParam(value = "shopId") Long shopId,
                                 @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {

        ParanaUser paranaUser = UserUtil.getCurrentUser();

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getShopId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to dealer(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.dealer");
        }
        //判断shop是否有效
        Shop dispatchShop = getShopById(shopId);


        Boolean isSuccess = vegaOrderWriteLogic.dispatchOrder(orderBase, orderLevel,
                VegaOrderEvent.FIRST_DEALER_DISPATCH_ORDER, dispatchShop, paranaUser);
        if (isSuccess) {
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.DISPATCHER));

            // 派单二级时更新二级成本价
            eventBus.post(VegaFirstShopDispatchEvent.from(orderId, shopId));
        }
        return isSuccess;
    }


    /**
     * 供应商/一级销商/二级经销商接单
     * 二级下单一级只可以接不可以派给二级
     * 1、二级下单 一级审核(只能接单)
     * 2、普通用户下单 一级审核(接单或派单)
     * 3、平台派个一级 一级审核(接单或派单)
     *
     * @param orderId 订单id
     * @return 是否派送成功
     */
    @RequestMapping(value = "/receive/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean receiveOrder(@PathVariable(value = "id") Long orderId,
                                @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {

        ParanaUser paranaUser = UserUtil.getCurrentUser();
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(paranaUser.getShopId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to dealer(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.dealer");
        }
        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(paranaUser);

        Response<List<Payment>> resp = paymentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.fromInt(orderType));
        List<Payment> paymentList = resp.getResult();
        Payment payment = null;
        for (Payment p : paymentList) {
            if (Objects.equal(p.getStatus(), 1)) {
                payment = p;
                break;
            }
        }

        // 线下支付时,不参与系统结算
        if (!Arguments.isNull(payment) && !Arguments.isNull(payment.getChannel())) {
            String channel = payment.getChannel();
            Long paymentId = payment.getId();
            String tradeNo = payment.getOutId();
            String paymentCode = payment.getPaySerialNo();
            Date paidAt = payment.getPaidAt();

            eventBus.post(new VegaOrderAcceptEvent(orderId, channel, paymentId, tradeNo, paymentCode, paidAt, null));
        }

        if (OrderUserType.DEALER_FIRST.equals(userType)) {

            dealerFirstReveiveSms(orderBase);
            return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.FIRST_DEALER_RECEIVE_ORDER);
        }

        if (OrderUserType.DEALER_SECOND.equals(userType)) {

            dealerSecondReveiveSms(orderBase);
            return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.SECOND_DEALER_RECEIVE_ORDER);
        }
        supplierReveiveSms(orderBase);
        return vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.SUPPLIER_CHECK);
    }


    @RequestMapping(value = "/paging-dispatch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderDispatchDetail> pagingDispatch(OrderDispatchCriteria orderDispatchCriteria) {
        if (orderDispatchCriteria.getStartAt() != null) {
            orderDispatchCriteria.setStartAt(handleDate(orderDispatchCriteria.getStartAt()));
        }
        if (orderDispatchCriteria.getEndAt() != null) {
            orderDispatchCriteria.setEndAt((new DateTime(handleDate(orderDispatchCriteria.getEndAt()))).plusDays(1).toDate());
        }
        Long userId = UserUtil.getUserId();
        Response<Shop> response = shopReadService.findByUserId(userId);
        if (!response.isSuccess()) {
            log.error("shopId find by userId fail ,userId {}, case {}", userId, response.getError());
            throw new JsonResponseException("shop.find.fail");
        } else {
            Long shopId = response.getResult().getId();
            orderDispatchCriteria.setReceiveShopId(shopId);
            //判断筛选条件是否有派单商家的名称,如果有处理塞入ID
            if (!StringUtil.isEmpty(orderDispatchCriteria.getDispatchShopName())) {
                Response<Shop> resp = shopReadService.findByName(orderDispatchCriteria.getDispatchShopName());
                if (!resp.isSuccess()) {
                    log.error("first level dispatch shop find by name fail ,shopName {}, case {}",
                            orderDispatchCriteria.getDispatchShopName(), resp.getError());
                    throw new JsonResponseException("shop.find.fail");
                } else {
                    Shop dispatchShop = resp.getResult();
                    orderDispatchCriteria.setDispatchShopId(dispatchShop.getId());
                }
            }

            Response<Paging<OrderDispatchRelation>> resp = orderDispatchRelationReadService.paging(orderDispatchCriteria);
            if (!resp.isSuccess()) {
                log.error("order dispatch relation paging fail ,shopId {}, case {}", shopId, resp.getError());
                throw new JsonResponseException("order.dispatch.relation.paging.fail");
            } else {
                Paging<OrderDispatchRelation> paging = resp.getResult();
                List<OrderDispatchRelation> lists = paging.getData();
                List<OrderDispatchDetail> list = Lists.newArrayList();
                for (OrderDispatchRelation orderDispatchRelation : lists) {
                    OrderDispatchDetail orderDispatchDetail = new OrderDispatchDetail();
                    orderDispatchDetail.setCreatedAt(orderDispatchRelation.getCreatedAt());
                    Map<String, String> orderCriteria = Maps.newHashMap();
                    orderCriteria.put("id", String.valueOf(orderDispatchRelation.getOrderId()));
                    Response<Paging<OrderGroup>> res = orderReadLogic.pagingOrder(orderCriteria);
                    if (!res.isSuccess()) {
                        log.error("order group paging fail ,id{}, case {}", orderDispatchRelation.getOrderId(), res.getError());
                        throw new JsonResponseException("order.paging.fail");

                    } else {
                        Paging<OrderGroup> orderGroupPaging = res.getResult();
                        List<OrderGroup> tempList = orderGroupPaging.getData();
                        if (tempList.size() > 0) {
                            orderDispatchDetail.setOrderGroup(tempList.get(0));
                        }
                    }
                    list.add(orderDispatchDetail);

                }
                return new Paging<OrderDispatchDetail>(paging.getTotal(), list);
            }
        }
    }

    /**
     * 修改运费(总订单运费 ShopOrder)
     *
     * @param orderId 订单ID
     * @param shipFee 运费
     * @return 修改结果
     */
    @RequestMapping(value = "/change/ship-fee/{orderId}", method = RequestMethod.PUT)
    public Boolean changeShipFee(@PathVariable(value = "orderId") Long orderId,
                                 @RequestParam(value = "shipFee") Integer shipFee) {
        Response<OrderDetail> orderResp = orderReadService.findOrderDetailById(orderId);
        if (!orderResp.isSuccess()) {
            log.error("failed to find order detail by orderId = ({}), cause : {}",
                    orderId, orderResp.getError());
            throw new JsonResponseException(500, orderResp.getError());
        }
        Integer diffFee = Arguments.isNull(orderResp.getResult().getShopOrder().getDiffFee()) ? 0 : orderResp.getResult().getShopOrder().getDiffFee();
        Long newFee = orderResp.getResult().getShopOrder().getOriginFee() + diffFee + shipFee;

        Response<Boolean> resp = vegaOrderWriteService.changeShopOrderShipFeeById(orderId, newFee, shipFee);
        if (!resp.isSuccess()) {
            log.error("failed to change ship fee of shopOrder by orderId = ({}), shipFee = ({}), cause : {}",
                    orderId, shipFee, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 卖家添加订单备注等
     *
     * @param orderId   订单id
     * @param orderSellerNote 备注
     * @param type            类型
     * @return 是否操作成功
     */
    @RequestMapping(value = "/note", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean sellerNoteOrder(@RequestParam("orderId") Long orderId,
                                   @RequestParam(required = false) String orderSellerNote,
                                   @RequestParam(required = false) Integer type) {
        if (Arguments.isNull(type)) {
            type = VegaNoteType.SELLER_ORDER_NOTE.value();
        }

        return vegaOrderComponent.setOrderNote(orderId,orderSellerNote, type);
    }


    /**
     * 修改订单价格和库存
     *
     * @param richOrder     richOrder
     * @param user          user
     * @param receiveShopId 接单店铺id(即当前商品所属人)
     */
    private void changeSkuPriceAndStock(RichOrder richOrder, ParanaUser user, Long receiveShopId) throws Exception {


        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(user);
        for (RichSkusByShop skusByShop : richOrder.getRichSkusByShops()) {
            for (RichSku richSku : skusByShop.getRichSkus()) {
                final Sku sku = richSku.getSku();

                Response<Integer> skuPriceResp = receiveShopParser.findSkuPrice(sku.getId(), receiveShopId,
                        user.getId(), orderUserType);
                if (!skuPriceResp.isSuccess()) {
                    log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                            sku.getId(), skusByShop.getShop().getId(), user.getId(), skuPriceResp.getError());
                    throw new JsonResponseException(skuPriceResp.getError());
                }

                Map<String, Integer> extraPrice = sku.getExtraPrice();
                if(CollectionUtils.isEmpty(extraPrice)){
                    extraPrice = Maps.newHashMap();
                }
                extraPrice.put(SystemConstant.ORDER_SKU_SELLER_PRICE,sku.getPrice());//供货价
                Integer firstSellerPrice = receiveShopParser.findSkuCostPrice(sku, receiveShopId, OrderUserType.DEALER_FIRST ); // 一级经销商成本价
                Integer secondSellerPrice = receiveShopParser.findSkuCostPrice(sku, receiveShopId, OrderUserType.DEALER_SECOND ); // 二级经销商成本价
                if (!Arguments.isNull(firstSellerPrice)) {
                    extraPrice.put(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE, firstSellerPrice);
                }
                if (!Arguments.isNull(secondSellerPrice)) {
                    extraPrice.put(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE, secondSellerPrice);
                }
                sku.setExtraPrice(extraPrice);
                sku.setPrice(skuPriceResp.getResult());
                sku.setStockQuantity(richSku.getQuantity());
            }
        }
    }

    /**
     * 全场满多少包邮临时解决方法:
     1.在parana_promotions表手工增加平台运费营销信息,默认取平台店铺状态为2和1的运费营销信息,如果有多条默认只取第一条
     2.满包邮金额从behavior_params_json.freeShipping字段获取
     3.不考虑拆单情况,一次下单只要总金额达到满邮金额就全部免邮
     * @param richOrder   richOrder
     */
    private void changeOrderShipFee(RichOrder richOrder) {
        if (Arguments.isNull(richOrder)) {
            return;
        }
        Response<List<Promotion>> listResponse = promotionReadService.findOngoingPromotion();
        if (!listResponse.isSuccess() || Arguments.isNullOrEmpty(listResponse.getResult())) {
            log.error("fail to find platform promotionInfo or promotionInfo is null ,cause:{}", listResponse.getError());
            return;
        }
        Promotion promotion = listResponse.getResult().get(0);

        if (!Arguments.isNull(promotion)) {
            Map<String, String> behaviorParamsMap = promotion.getBehaviorParams();
            if (behaviorParamsMap.containsKey("freeShipping") &&
                    !Arguments.isNull(behaviorParamsMap.get("freeShipping"))) {
                Long freeShippingFee = Long.valueOf(behaviorParamsMap.get("freeShipping"));
                Long orderOirginFee = 0L;

                for (RichSkusByShop skusByShop : richOrder.getRichSkusByShops()) {
                    orderOirginFee += skusByShop.getOriginFee(); //订单实际金额,不包括邮费
                }

                if (orderOirginFee >= freeShippingFee) {
                    for (RichSkusByShop skusByShop : richOrder.getRichSkusByShops()) {
                        skusByShop.setShipFee(0); // 实际邮费替换为0
                        skusByShop.setFee(skusByShop.getOriginFee()); // 实际订单金额为原始金额
                        skusByShop.setShipmentPromotionId(promotion.getId()); // 运费营销ID
                    }
                }
            }
        }
    }

    /**
     * 组装SubmittedOrder.submittedSkusByShops.submittedSkus
     * SubmittedOrder.submittedSkusByShops 元素中shopId,如果是一级下单则为真实的商品店铺id,否则为接单店铺id
     */
    private void assemblySubmittedSkusByShop(SubmittedOrder submittedOrder, Long purchaseOrderId, Long receiveShopId) {

        //获取采购单信息 并验证是否存在
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderById(purchaseOrderId);

        List<SubmittedSkusByShop> submittedSkusByShops = submittedOrder.getSubmittedSkusByShops();
        if (CollectionUtils.isEmpty(submittedSkusByShops)) {
            throw new JsonResponseException("order.sku.invalid");
        }
        for (SubmittedSkusByShop submittedSkusByShop : submittedSkusByShops) {
            List<PurchaseSkuOrder> purchaseSkuOrders;
            List<SubmittedSku> submittedSkus;
            //非一级经销商下单(不用拆单,取采购单全部商品)
            if (Objects.equal(submittedSkusByShop.getShopId(), receiveShopId)) {
                //采购单下的所有商品
                purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderId(purchaseOrder.getId());
                submittedSkus = assemblySubmittedSku(purchaseSkuOrders);
            } else {
                //某个店铺下的商品 一级下单
                purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderAndShopId(purchaseOrderId,
                        submittedSkusByShop.getShopId());
                submittedSkus = assemblySubmittedSku(purchaseSkuOrders);
                //兼容下单封装子订单按店铺归组(这里就先不把一级下的商品归属于平台了)
                //submittedSkusByShop.setShopId(receiveShopId);//设置接单店铺id,即平台店铺id,所以单先下给平台
            }
            submittedSkusByShop.setSubmittedSkus(submittedSkus);
        }
    }

    private List<SubmittedSku> assemblySubmittedSku(List<PurchaseSkuOrder> purchaseSkuOrders) {
        List<SubmittedSku> submittedSkus = Lists.newArrayListWithExpectedSize(purchaseSkuOrders.size());
        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders) {
            SubmittedSku submittedSku = new SubmittedSku();
            submittedSku.setSkuId(purchaseSkuOrder.getSkuId());
            submittedSku.setQuantity(purchaseSkuOrder.getQuantity());

            submittedSkus.add(submittedSku);
        }
        return submittedSkus;
    }

    private ParanaUser getUserById() {
        Long userId = UserUtil.getUserId();
        Response<User> userR = userReadService.findById(userId);
        if (!userR.isSuccess()) {
            log.error("fail to find user by id {}, error code:{}",
                    userId, userR.getError());
            throw new JsonResponseException(userR.getError());
        }
        return ParanaUserMaker.from(userR.getResult());
    }

    private Shop getShopById(Long shopId) {
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }

    //处理日期函数,将前端传来的日期精确到时分秒
    private Date handleDate(Date date) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String dateStr = dateFormat.format(date);
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            log.error("date forMate fail ,cause{}", date);
            e.printStackTrace();
        }
        return date;
    }

    //一级接单 短信提醒
    private void dealerFirstReveiveSms(OrderBase orderBase) {
        //发给派单人(派给一级)
        if (orderBase.getStatus().equals(VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue())) {

            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.DISPATCHER_RECEIVE));
            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.CHECKED));

        }
        //发给下单人(下给一级)
        if (orderBase.getStatus().equals(VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue())) {
            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.CHECKED));
        }

    }

    //二级接单 短信提醒
    private void dealerSecondReveiveSms(OrderBase orderBase) {
        //发给派单人(派给二级)
        if (orderBase.getStatus().equals(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue())
                || orderBase.getStatus().equals(VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue())) {

            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.DISPATCHER_RECEIVE));
            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.CHECKED));

        }
        //发给下单人(下给二级)
        if (orderBase.getStatus().equals(VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue())) {
            eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.CHECKED));
        }

    }

    //供应商接单 短信提醒
    private void supplierReveiveSms(OrderBase orderBase) {
        eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.DISPATCHER_RECEIVE));
        eventBus.post(new TradeSmsEvent(orderBase.getId(), TradeSmsNodeEnum.CHECKED));

    }


    @RequestMapping(value = "/sku",method = RequestMethod.GET ,produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaOrderDetail.SkuOrderAndOperation findSkuOrderDetailById(Long skuOrderId) {

        Response<VegaOrderDetail.SkuOrderAndOperation> response = vegaOrderReadService.findSkuOrderDetailById(skuOrderId,flowPicker);
        if (!response.isSuccess()) {
            log.error("find sku order detail failed , id {} , cause {}", skuOrderId, response.getError());
                throw new JsonResponseException("sku.order.detail.find.fail");
        } else {
            return response.getResult();
        }
    }


    @RequestMapping(value = "/fee",method = RequestMethod.GET)
    public Long countOrdersFee(@RequestParam String orderIds) {
        List<Long> orderIdList = Splitters.splitToLong(orderIds,Splitters.COMMA);
        Long fee =0L;

        Response<List<ShopOrder>> response = shopOrderReadService.findByIds(orderIdList);
        if (!response.isSuccess()) {
            log.error("find shop order  failed ,by ids {} , cause {}", orderIdList, response.getError());
            throw new JsonResponseException("sku.order.detail.find.fail");
        } else {
            List<ShopOrder> shopOrders = response.getResult();
            for (ShopOrder shopOrder : shopOrders){
                fee+=shopOrder.getFee();
            }
        }

        return fee;
    }


    @RequestMapping(value = "/wait/handle/alert",method = RequestMethod.GET ,produces = MediaType.APPLICATION_JSON_VALUE)
    public WaitHandleAlert findWaitHandleAlert() {

        WaitHandleAlert alert = new WaitHandleAlert();
        Long userId = UserUtil.getUserId();
        //待付款
        OrderCriteria criteria = new OrderCriteria();
        criteria.setBuyerId(userId);
        criteria.setStatus(Lists.newArrayList(VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue()));
        alert.setWaitPay(getTotal(criteria));
        //待发货:status "3,4,5,6,7,8,9,10,11,12,13,14,15,18,19,20,21,-3,-4,-6,-7,-8,-9,-11
        criteria.setStatus(Lists.newArrayList(
                VegaOrderStatus.PAID_WAIT_CHECK.getValue(),//3
                VegaOrderStatus.WAIT_SUPPLIER_CHECK.getValue(),//4
                VegaOrderStatus.SUPPLIER_CHECKED_WAIT_SHIPPED.getValue(),//5
                VegaOrderStatus.PLATFORM_CHECKED_WAIT_FIRST_DEALER_CHECK.getValue(),//6
                VegaOrderStatus.WAIT_FIRST_DEALER_CHECK.getValue(),//7
                VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPPED.getValue(),//8
                VegaOrderStatus.WAIT_SECOND_DEALER_CHECK.getValue(),//9
                VegaOrderStatus.FIRST_DEALER_CHECKED_OUT_WAIT_SHIPP_PLATFORM.getValue(),//10
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK.getValue(),//11
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_SECOND_DEALER_CHECK_PLATFORM.getValue(),//12
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_PLATFORM.getValue(),//13
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_FIRST_DEALER.getValue(),//14
                VegaOrderStatus.SECOND_DEALER_CHECKED_WAIT_SHIPP_SECOND_DEALER.getValue(),//15
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT.getValue(),//18
                VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),//19
                VegaOrderStatus.FIRST_DEALER_CHECKED_WAIT_OUT_PLATFORM.getValue(),//20
                VegaOrderStatus.FIRST_DEALER_OUT_WAIT_OVER_PLATFORM.getValue(),//21
                VegaOrderStatus.PLATFORM_REJECT.getValue(),//-3
                VegaOrderStatus.SUPPLIER_REJECT.getValue(),//-4
                VegaOrderStatus.FIRST_DEALER_REJECT.getValue(),//-6
                VegaOrderStatus.FIRST_DEALER_REJECT_RECEIVE.getValue(),//-7
                VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE.getValue(),//-8
                VegaOrderStatus.SECOND_DEALER_REJECT.getValue(),//-9
                VegaOrderStatus.SECOND_DEALER_REJECT_RECEIVE_PLATFORM.getValue()//-11
        ));
        alert.setWaitDeliver(getTotal(criteria));
        criteria.setStatus(Lists.newArrayList(VegaOrderStatus.SHIPPED.getValue()));
        alert.setWaitConfirmArrive(getTotal(criteria));

        return alert;
    }

    //这里只取total
    private Long getTotal(OrderCriteria criteria){
        Response<Paging<ShopOrder>> ordersR = shopOrderReadService.findBy(1,1,criteria);
        if(!ordersR.isSuccess()){
            log.error("paging order criteria:{} fail,error:{}",criteria,ordersR.getError());
            throw new JsonResponseException(ordersR.getError());
        }

        return ordersR.getResult().getTotal();
    }






}
