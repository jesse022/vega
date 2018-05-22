/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.trade;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.settlement.event.VegaOrderAcceptEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import io.terminus.parana.web.core.component.order.RefundLogic;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.constants.RequestParams;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.event.RefundCallbackEvent;
import io.terminus.pay.model.TradeResult;
import io.terminus.pay.service.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 退款有关操作
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-24
 */
@RestController
@Slf4j
public class VegaRefunds {

    private final RefundLogic refundLogic;

    private final CommonRefundLogic commonRefundLogic;

    private final ChannelRegistry channelRegistry;
    private final EventBus eventBus;
    private final VegaOrderComponent vegaOrderComponent;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private PaymentReadService paymentReadService;
    private final FlowPicker flowPicker;



    @Autowired
    public VegaRefunds(RefundLogic refundLogic,
                       CommonRefundLogic commonRefundLogic,
                       ChannelRegistry channelRegistry,
                       EventBus eventBus,
                       VegaOrderComponent vegaOrderComponent,
                       FlowPicker flowPicker) {
        this.refundLogic = refundLogic;
        this.commonRefundLogic = commonRefundLogic;
        this.channelRegistry = channelRegistry;
        this.eventBus = eventBus;
        this.vegaOrderComponent = vegaOrderComponent;
        this.flowPicker = flowPicker;
    }

    /**
     * 当前不能跨订单申请退款, 但是支持同一订单的多个子订单申请退款
     *
     * @param buyerNote 买家备注
     * @param orderIds  (子)订单列表
     * @param orderType 订单级别
     * @return 创建的退款单id
     */
    @RequestMapping(value = "/api/vega/buyer/order/refund", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long applyRefund(@RequestParam(value = "buyerNote", required = false) String buyerNote,
                            @RequestParam("orderIds") List<Long> orderIds,
                            @RequestParam(value = "orderType", defaultValue = "2") Integer orderType) {
        if (CollectionUtils.isEmpty(orderIds)) {
            log.error("no orderIds specified when apply refund");
            throw new JsonResponseException("order.refund.fail");
        }
        try {
            ParanaUser buyer = UserUtil.getCurrentUser();
            OrderLevel orderLevel = OrderLevel.fromInt(orderType);

            Long orderId;
            if(orderLevel.equals(OrderLevel.SHOP)){
                orderId = orderIds.get(0);
            }else {
                orderId = findShopOrderIdBySkuOrderId(orderIds.get(0));
            }

            Response<ShopOrder> rShopOrder = shopOrderReadService.findById(orderId);
            if (!rShopOrder.isSuccess()) {
                log.error("failed to find shopOrder(id={}), error code:{}", orderId, rShopOrder.getError());
                throw new JsonResponseException(rShopOrder.getError());
            }


            //检查订单是否适合退款
            ShopOrder shopOrder = rShopOrder.getResult();

            Flow flow = flowPicker.pick(shopOrder, OrderLevel.SHOP);
            //todo 这里的订单状态应该是方法内部 自己转换获取
            Integer orderStatus= flow.target(shopOrder.getStatus(),VegaOrderEvent.REFUND_APPLY.toOrderOperation());

            Long refundId = refundLogic.applyRefund(orderIds, orderLevel, buyer, buyerNote, VegaOrderEvent.REFUND_APPLY.toOrderOperation(),
                    orderStatus);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.APPLY_REFUND));
            return refundId;
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("failed to  apply refund for order(ids={}, level={}), cause:{}",
                    orderIds, orderType, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.fail");
        }
    }

    /**
     * 退款单审核通过
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/seller/refund/{refundId}/agree", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void agreeRefund(@PathVariable("refundId") Long refundId,
                            @RequestParam(required = true) Long refundFee,
                            @RequestParam(required = false) OrderOperation orderOperation) {
        try {
            Response<Refund> resp = refundReadService.findById(refundId);
            if (!resp.isSuccess()) {
                log.error("failed to find refund by refundId = {}, cause : {}", refundId, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            if (refundFee > resp.getResult().getFee()) {
                log.error("refund fee ={} can not greater than actual pay fee ={}",refundFee, resp.getResult().getFee());
                throw new JsonResponseException("refund.fee.greater.than.actual.pay.fee");
            }

            Refund update = new Refund();
            update.setId(refundId);
            update.setFee(refundFee);
            Response<Boolean> response = refundWriteService.update(update);
            if (!response.isSuccess()) {
                log.error("update refund :{} fail,error:{}", update, response.getError());
                throw new JsonResponseException(response.getError());
            }

            ParanaUser seller = UserUtil.getCurrentUser();
            refundLogic.agreeRefund(refundId, seller, Arguments.isNull(orderOperation) ? VegaOrderEvent.REFUND_APPLY_AGREE.toOrderOperation() : orderOperation);
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.AGREE_REFUND));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to agree refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.agree.fail");
        }
    }

    /**
     * 供应商退款单审核通过,需提交平台运营审核
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/supplier/refund/{refundId}/agree", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void agreeRefund(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            refundLogic.agreeRefund(refundId, seller, VegaOrderEvent.SUPPLIER_REFUND_APPLY_AGREE.toOrderOperation());
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to agree refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.agree.fail");
        }
    }

    /**
     * 退款单审核通过(一级经销商接单,买家付完款后待审核节点)
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/first/dealer/refund/{refundId}/agree", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void firstDealerAgreeRefund(@PathVariable("refundId") Long refundId,
                            @RequestParam(required = true) Long refundFee) {
        try {
            agreeRefund(refundId, refundFee, VegaOrderEvent.FIRST_DEALER_REFUND_APPLY_AGREE.toOrderOperation());


            // 生成正逆向结算订单明细
            Response<List<OrderRefund>> resp = refundReadService.findOrderIdsByRefundId(refundId);
            if (!resp.isSuccess()) {
                log.error("failed to find orderRefund by refundId = {}, cause : {}",
                        refundId, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }

            Long orderId = resp.getResult().get(0).getOrderId(); //目前业务一个退款单肯定对应一个订单号
            Integer orderType = resp.getResult().get(0).getOrderType();

            Response<List<Payment>> resPayMent = paymentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.fromInt(orderType));
            List<Payment> paymentList = resPayMent.getResult();
            Payment payment = null;
            for (Payment p : paymentList) {
                if (Objects.equal(p.getStatus(), 1)) {
                    payment = p;
                    break;
                }
            }
            if (!Arguments.isNull(payment)) {
                Long paymentId = payment.getId();
                String tradeNo = payment.getOutId();
                String channel = payment.getChannel();
                String paymentCode = payment.getPaySerialNo();
                Date paidAt = payment.getPaidAt();
                log.info("一级接单流程，买家已付款一级待审核节点退款审核通过时生成对应的结算正向订单明细. userType = {}, payment = {}", orderType, payment);
                eventBus.post(new VegaOrderAcceptEvent(orderId, channel, paymentId, tradeNo, paymentCode, paidAt, null));
            }else {
                log.error("failed to generate settle forward details, cause payment is null, " +
                        "order id = {}, order type = {}", orderId, orderType);
            }
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to agree refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.agree.fail");
        }
    }

    /**
     * 退款单审核拒绝
     *
     * @param refundId 退款单id
     * @param sellerNote 卖家备注
     */
    @RequestMapping(value = "/api/vega/seller/refund/{refundId}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void rejectRefund(@PathVariable("refundId") Long refundId,@RequestParam(required = false) String sellerNote) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            refundLogic.rejectRefund(refundId, seller, VegaOrderEvent.REFUND_APPLY_REJECT.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.REJECT_REFUND));
            vegaOrderComponent.setRefundSellerNote(refundId,sellerNote);//添加拒绝备注
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to reject refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.reject.fail");
        }
    }

    /**
     * 取消退款
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/buyer/refund/{refundId}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void cancelRefund(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser buyer = UserUtil.getCurrentUser();
            refundLogic.cancelRefund(refundId, buyer, VegaOrderEvent.REFUND_APPLY_CANCEL.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.BUYER_CANCEL_REFUND));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to cancel refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.refund.cancel.fail");
        }
    }

    /**
     * 退款回调函数,所有真实发生退款的接口都应该回调到这里
     *
     */
    @RequestMapping(value = "/api/vega/refund/notify/{channel}/account/{accountNo}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String postRefund(@PathVariable("channel")String channel,
                             @PathVariable("accountNo") String accountNo,
                             HttpServletRequest request) {
        try {
            log.info("=========begin refund notify,channel={},accountNo={},params={}", channel, accountNo, request.getParameterMap());
            PayChannel paymentChannel=channelRegistry.findChannel(channel);
            request.setAttribute(RequestParams.ACCOUNT, accountNo);
            paymentChannel.verify(request);
            TradeResult refundedInfo=paymentChannel.refundCallback(request);

            if(refundedInfo.isFail()){
                return "fail";
            }
            //根据outId查询退款单
            Response<Refund> refundR = refundReadService.findByOutId(refundedInfo.getMerchantSerialNo());
            if (!refundR.isSuccess()) {
                log.error("fail to find refund by out id {}, error code:{}, return directly",
                        refundedInfo.getMerchantSerialNo(), refundR.getError());
                return refundedInfo.getCallbackResponse();
            }
            Refund refund = refundR.getResult();
            Map<String, String> tages = refund.getTags();
            String platformRefundTag = tages.get(SystemConstant.PLATFORM_REFUND);

            Response<Shop> shopRes = shopReadService.findById(refund.getShopId());
            if (!shopRes.isSuccess()) {
                log.error("find shop by id:{} fail,error:{}", refund.getShopId(), shopRes.getError());
                return refundedInfo.getCallbackResponse();
            }
            OrderOperation orderOperation;
            if(Objects.equal(shopRes.getResult().getType(), VegaShopType.PLATFORM.value())
                    || Objects.equal(platformRefundTag, SystemConstant.PLATFORM_REFUND_TAG)) {
                orderOperation = VegaOrderEvent.REFUND_ADMIN.toOrderOperation();
            }else {
                orderOperation = VegaOrderEvent.REFUND.toOrderOperation();

            }

            refundLogic.postRefund(refundedInfo.getMerchantSerialNo(),
                refundedInfo.getTradeAt(), orderOperation);
            log.info("=========end refund notify,channel={},accountNo={},params={}", channel, accountNo, request.getParameterMap());
            return refundedInfo.getCallbackResponse();
        } catch (Exception e) {
            log.error("fail to call back refund by request params ({}), cause:{}",
                    request.getParameterMap(), Throwables.getStackTraceAsString(e));
            return "fail";
        }
    }




    /**
     * 退款预处理
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/seller/refund/{refundId}/pre", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void preRefund(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            Integer orderStatus = VegaOrderStatus.REFUND.getValue();
            Refund refund =commonRefundLogic.getRefund(refundId);
            Map<String, String> tages = refund.getTags();
            String isShiped = tages.get(SystemConstant.IS_SHIPPED);
            if(isShiped.equals(SystemConstant.SHIPPED)){
                orderStatus = VegaOrderStatus.RETURN_REFUND.getValue();
            }

            commonRefundLogic.preRefund(refundId, seller, VegaOrderEvent.REFUND.toOrderOperation(), orderStatus);
            if(refund.getChannel().contains(AllinpayChannels.CHANNEL)){
                Refund update = new Refund();
                update.setId(refundId);
                update.setSellerNote(SystemConstant.ALLINPAY_REFUND_APPLY);
                Response<Boolean> response = refundWriteService.update(update);
                if(!response.isSuccess()){
                    log.error("update refund :{} fail,error:{}",update,response.getError());
                }
            }

            //微信支付退款则以同步通知为准，更新订单状态
            if(refund.getChannel().startsWith("wechatpay")){
                TradeResult tradeResult = new TradeResult();
                tradeResult.setTradeAt(new Date());
                tradeResult.setMerchantSerialNo(refund.getOutId());
                tradeResult.setChannel(refund.getChannel());
                tradeResult.setStatus(TradeStatus.SUCCESS.value());
                //生成结算事件
                eventBus.post(new RefundCallbackEvent(tradeResult));
                log.info("[WECHAT-REFUND]post wechat pay  event bus refund id:{}",refund.getId());
            }

        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to pre refund by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.pre.refund.fail");
        }
    }

    private Long findShopOrderIdBySkuOrderId(Long skuOrderId){

        Response<SkuOrder> skuOrderRes = skuOrderReadService.findById(skuOrderId);
        if(!skuOrderRes.isSuccess()){
            log.error("find sku order by id:{} fail,error:{}",skuOrderId,skuOrderRes.getError());
            throw new JsonResponseException(skuOrderRes.getError());
        }

        return skuOrderRes.getResult().getOrderId();
    }





}

