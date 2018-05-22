package com.sanlux.web.front.controller.delivery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.service.PurchaseOrderReadService;
import com.sanlux.trade.service.PurchaseSkuOrderReadService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import com.sanlux.web.front.service.PurchaseOrderService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.SubmittedSku;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.promotion.model.Promotion;
import io.terminus.parana.promotion.service.PromotionReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;
import static com.sanlux.common.helper.UserTypeHelper.getOrderUserTypeByUser;

/**
 * 下单预览页运费计算
 * Author:songrenfei
 * Created on 6/15/16.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/delivery-fee-charge")
public class VegaDeliveryFeeChargers {

    @Autowired
    private RichOrderMaker richOrderMaker;

    @Autowired
    private DeliveryFeeCharger deliveryFeeCharger;

    @Autowired
    private ReceiveShopParser receiveShopParser;

    @Autowired
    private PurchaseOrderService purchaseOrderService;



    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;

    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private PromotionReadService promotionReadService;



    private final static TypeReference<List<SubmittedSku>> LIST_OF_SUBMITTED_SKU =
            new TypeReference<List<SubmittedSku>>() {
            };

    @RequestMapping(value = "/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer vegaChargeForSku(@RequestParam("skuId") Long skuId,
                                @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                @RequestParam("addressId") Integer addressId) {
        return deliveryFeeCharger.charge(skuId, quantity, addressId);
    }

    @RequestMapping(value = "/order-preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<Long, Integer> vegaChargeForOrderPreview(@RequestParam("purchaseOrderId") Long purchaseOrderId,
                                                    @RequestParam("receiverInfoId") Long receiverInfoId) {
        //buyer中的shopId 强制塞为接单人的shopId
        final ParanaUser buyer = UserUtil.getCurrentUser();
        try {

            //获取采购单信息 并验证是否存在
            PurchaseOrder purchaseOrder = getPurchaseOrderById(purchaseOrderId);

            //采购单下的所有商品
            List<PurchaseSkuOrder> purchaseSkuOrders = getPurchaseSkuOrdersByPurchaseOrderById(purchaseOrder.getId());

            List<SubmittedSku> submittedSkus = transToSubmittedSku(purchaseSkuOrders);
            if (CollectionUtils.isEmpty(submittedSkus)) {
                throw new JsonResponseException("sku.not.provided");
            }

            Response<ReceiverInfo> receiverInfoResp = receiverInfoReadService.findById(receiverInfoId);
            if (!receiverInfoResp.isSuccess()) {
                log.error("fail to find receiverInfo by id:{},cause:{}", receiverInfoId, receiverInfoResp.getError());
                throw new JsonResponseException(receiverInfoResp.getError());
            }

            /*
             全场满多少包邮临时解决方法:
             1.在parana_promotions表手工增加平台运费营销信息,默认取平台店铺状态为2和1的运费营销信息,如果有多条默认只取第一条
             2.满包邮金额从behavior_params_json.freeShipping字段获取
             3.不考虑拆单情况,一次下单只要总金额达到满邮金额就全部免邮
             */
            OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);
            String roleName = getUserRoleName(buyer);
            //获取接单人店铺id
            Long receiveShopId = getReceiveShopId(roleName,buyer,receiverInfoResp.getResult(),purchaseSkuOrders);
            //当前订单商品金额
            Map<Long, Long> OrderOirginFeeMap = countOrderOirginFee(purchaseOrderId, receiveShopId, orderUserType,buyer);

            Promotion promotion = null;
            Response<List<Promotion>> listResponse = promotionReadService.findOngoingPromotion();
            if (!listResponse.isSuccess()) {
                log.error("fail to find platform promotionInfo,cause:{}", listResponse.getError());
            } else {
                if (!Arguments.isNullOrEmpty(listResponse.getResult()) ) {
                    promotion = listResponse.getResult().get(0);
                }
            }



            //强制塞为接单人的shopId
            setReceiveShopIdToParanaUser(buyer,purchaseSkuOrders,receiverInfoResp.getResult());

            RichOrder richOrder = richOrderMaker.partial(submittedSkus, buyer, null);
            richOrder.setReceiverInfo(receiverInfoResp.getResult());
            Map<Long, Integer> deliveryFeeMap = deliveryFeeCharger.charge(richOrder.getRichSkusByShops(),richOrder.getReceiverInfo());

            if (!Arguments.isNull(promotion)) {
                Map<String, String> behaviorParamsMap = promotion.getBehaviorParams();
                if (behaviorParamsMap.containsKey("freeShipping") &&
                        !Arguments.isNull(behaviorParamsMap.get("freeShipping"))) {
                    // 免邮金额
                    Long freeShippingFee = Long.valueOf(behaviorParamsMap.get("freeShipping"));
                    // 订单总金额,不考虑拆单,只要订单总金额超出满邮金额就免邮
                    Long orderOirginFee = 0L;
                    for (Long index : deliveryFeeMap.keySet()) {
                        if (OrderOirginFeeMap.containsKey(index)) {
                            orderOirginFee += OrderOirginFeeMap.get(index);
                        }
                    }
                    if (orderOirginFee >= freeShippingFee) {
                        for (Long key :deliveryFeeMap.keySet()) {
                            deliveryFeeMap.replace(key, 0);
                        }
                    }
                }
            }

            return deliveryFeeMap;
        } catch (Exception e) {
            log.error("fail to charge delivery fee for order preview,purchase order id:{},cause:{}",
                    purchaseOrderId, Throwables.getStackTraceAsString(e));
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            throw new JsonResponseException("charge.delivery.fee.fail");
        }
    }

    //purchaseSkuOrder to submittedSku
    private List<SubmittedSku> transToSubmittedSku(List<PurchaseSkuOrder> purchaseSkuOrders) {

        List<SubmittedSku> submittedSkus = Lists.newArrayList();
        for (PurchaseSkuOrder purchaseSkuOrder: purchaseSkuOrders){
            SubmittedSku submittedSku = new SubmittedSku();
            submittedSku.setQuantity(purchaseSkuOrder.getQuantity());
            submittedSku.setSkuId(purchaseSkuOrder.getSkuId());
            submittedSkus.add(submittedSku);
        }
        return submittedSkus;
    }


    private PurchaseOrder getPurchaseOrderById(Long purchaseOrderId){

        //检测当前采购单是否存在
        Response<Optional<PurchaseOrder>> existRes = purchaseOrderReadService.findPurchaseOrderById(purchaseOrderId);
        if(!existRes.isSuccess()){
            log.error("find purchase order by id:{} fail,error:{}",purchaseOrderId,existRes.getError());
            throw new JsonResponseException(existRes.getError());
        }

        if(!existRes.getResult().isPresent()){
            log.error("not find purchase order by id:{}",purchaseOrderId);
            throw new JsonResponseException("purchase.order.not.exist");
        }

        return existRes.getResult().get();

    }


    private List<PurchaseSkuOrder> getPurchaseSkuOrdersByPurchaseOrderById(Long purchaseOrderId){

        //检测当前采购单是否存在
        Response<List<PurchaseSkuOrder>> skuRes = purchaseSkuOrderReadService.finByPurchaseOrderIdAndStatus(purchaseOrderId, 1);
        if(!skuRes.isSuccess()){
            log.error("find purchase sku order by purchase order id:{} status:{} fail,error:{}",purchaseOrderId,1,skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        return skuRes.getResult();

    }


    private void setReceiveShopIdToParanaUser(ParanaUser buyer,List<PurchaseSkuOrder> purchaseSkuOrders,ReceiverInfo receiverInfo){
        OrderUserType userType = getOrderUserTypeByUser(buyer);

        //一级下单
        if(userType.equals(OrderUserType.DEALER_FIRST)){
            buyer.setShopId(DefaultId.PLATFROM_SHOP_ID);
            return;
        }

        //二级下单则取上级
        if(userType.equals(OrderUserType.DEALER_SECOND)){
            Response<Long> shopPidRes = receiveShopParser.getShopPidForDealerSecond(buyer.getId());
            if(!shopPidRes.isSuccess()){
                log.error("get shop pid by dealer second user id:{}",buyer.getId());
                throw new JsonResponseException(shopPidRes.getError());
            }
            //extra中shopPid 即为一级经销商id
            buyer.setShopId(shopPidRes.getResult());
            return;
        }

        //判断普通用户(或业务经理/供应商)下单 的接单人shopId 平台或一级经销商
        if(userType.equals(OrderUserType.NORMAL_USER)
                || userType.equals(OrderUserType.SERVICE_MANAGER)
                || userType.equals(OrderUserType.SUPPLIER)){
            Response<Long> shoIdRes = receiveShopParser.getShopIdForBuyer(receiverInfo, purchaseSkuOrders,buyer);
            if(!shoIdRes.isSuccess()){
                log.error("get shop id for buyer where receive info id:{} error:{}", receiverInfo.getId(), shoIdRes.getError());
                throw new JsonResponseException(shoIdRes.getError());

            }
            buyer.setShopId(shoIdRes.getResult());
        }

        //一级下单则不需要塞,就按商品自身所属的shop信息,类似于b2c下单

    }

    /**
     * 获取接单店铺逻辑
     * @param roleName           用户角色
     * @param buyer              用户信息
     * @param receiverInfo       收货地址信息
     * @param purchaseSkuOrders  采购单信息
     * @return  接单店铺Id
     */
    private Long getReceiveShopId(String roleName,ParanaUser buyer,ReceiverInfo receiverInfo,List<PurchaseSkuOrder> purchaseSkuOrders){

        //一级下单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            return 0L;
        }
        //二级下单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_SECOND.name())){
            Response<Long> shopPidRes = receiveShopParser.getShopPidForDealerSecond(buyer.getId());
            if(shopPidRes.isSuccess()){
                return shopPidRes.getResult();
            }
            log.error("get shop pid by dealer second user id:{}",buyer.getId());
            throw new JsonResponseException(shopPidRes.getError());
        }

        //普通用户
        //收货地址不为空,则去匹配经销商
        if(!Arguments.isNull(receiverInfo)){
            if(StringUtils.equals(roleName, UserRole.BUYER.name())){
                Response<Long> shoIdRes = receiveShopParser.getShopIdForBuyer(receiverInfo, purchaseSkuOrders, buyer);
                if(shoIdRes.isSuccess()){
                    return shoIdRes.getResult();
                }
                log.error("get shop id for buyer where receive info id:{} error:{}",receiverInfo.getId(),shoIdRes.getError());
                throw new JsonResponseException(shoIdRes.getError());
            }
        }else {
            return 0L;//没有收货地址则默认平台接单即商品按零售价展示
        }
        throw new JsonResponseException("not.matching.receive.shop");
    }

    /**
     * 计算单个订单的商品金额
     * @param purchaseOrderId 采购单id
     * @param receiveShopId 接单店铺id (平台店铺id为0)
     * @return 订单金额
     */
    private Map<Long, Long> countOrderOirginFee(Long purchaseOrderId, Long receiveShopId, OrderUserType orderUserType, ParanaUser user) {
        Map<Long, Long> OrderOirginFeeMap = Maps.newHashMap();

        List<PurchaseSkuOrder> purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderId(purchaseOrderId);

        if (CollectionUtils.isEmpty(purchaseSkuOrders)) {
            log.error("not valid sku to create order where purchase order id:{}, receive shop id:{} orderUserType:{} ", purchaseOrderId, receiveShopId, orderUserType.toString());
            throw new JsonResponseException("not.valid.purchase.sku.order");

        }
        Long originFee = 0L;

        if (Objects.equals(orderUserType, OrderUserType.DEALER_FIRST)) {
            // 一级经销商下单时需要拆单,所以根据该id获取到对应的商品

            List<Long> shopIds = Lists.transform(purchaseSkuOrders, new Function<PurchaseSkuOrder, Long>() {
                @Override
                public Long apply(PurchaseSkuOrder purchaseSkuOrder) {
                    return purchaseSkuOrder.getShopId();
                }
            });
            List<Long> shopIdsSet = ImmutableSet.copyOf(shopIds).asList();
            for (Long shopId : shopIdsSet) {
                originFee = 0L;
                List<PurchaseSkuOrder> purchaseSkuOrdersFilter = Lists.newArrayList();

                purchaseSkuOrders.stream().filter(purchaseSkuOrder -> Objects.equals(shopId, purchaseSkuOrder.getShopId())).forEach(purchaseSkuOrdersFilter::add
                );
                if (!Arguments.isNullOrEmpty(purchaseSkuOrdersFilter)) {
                    for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrdersFilter) {
                        Sku sku = findSkuById(purchaseSkuOrder.getSkuId());
                        //计算真实价格
                        Integer skuPrice = getRealSkuPrice(sku.getId(), receiveShopId, user.getId(), orderUserType);
                        originFee += Long.valueOf(skuPrice) * purchaseSkuOrder.getQuantity();
                    }
                    OrderOirginFeeMap.put(shopId, originFee);
                }
            }
            return OrderOirginFeeMap;
        }

        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders) {
            Sku sku = findSkuById(purchaseSkuOrder.getSkuId());
            //计算真实价格
            Integer skuPrice = getRealSkuPrice(sku.getId(), receiveShopId, user.getId(), orderUserType);
            originFee += Long.valueOf(skuPrice) * purchaseSkuOrder.getQuantity();
        }
        OrderOirginFeeMap.put(receiveShopId, originFee); //接单店铺订单总金额
        return OrderOirginFeeMap;
    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
    }

    private Integer getRealSkuPrice(Long skuId, Long receiveShopId, Long userId,
                                    OrderUserType orderUserType){
        Response<Integer> skuPriceResp =
                receiveShopParser.findSkuPrice(skuId,receiveShopId, userId, orderUserType);
        if (!skuPriceResp.isSuccess()){
            log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                    skuId, receiveShopId, userId, skuPriceResp.getError());
            throw new JsonResponseException(skuPriceResp.getError());
        }

        return skuPriceResp.getResult();
    }


}
