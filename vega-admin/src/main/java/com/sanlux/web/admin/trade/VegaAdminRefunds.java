/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.trade;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import io.terminus.parana.web.core.component.order.RefundLogic;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.enums.TradeStatus;
import io.terminus.pay.event.RefundCallbackEvent;
import io.terminus.pay.model.TradeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 退款有关操作
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-24
 */
@RestController
@Slf4j
public class VegaAdminRefunds {

    private final RefundLogic refundLogic;

    private final CommonRefundLogic commonRefundLogic;

    private final ChannelRegistry channelRegistry;
    private final EventBus eventBus;
    private final VegaOrderComponent vegaOrderComponent;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private RefundReadService refundReadService;
    private final FlowPicker flowPicker;



    @Autowired
    public VegaAdminRefunds(RefundLogic refundLogic,
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
     * 退款单审核通过
     *
     * @param refundId  退款单id
     * @param refundFee 实际退款金额
     */
    @RequestMapping(value = "/api/vega/admin/refund/{refundId}/agree", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void agreeRefund(@PathVariable("refundId") Long refundId,
                            @RequestParam(required = true) Long refundFee ) {
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
            Map<String, String> tages = resp.getResult().getTags();
            tages.put(SystemConstant.PLATFORM_REFUND,SystemConstant.PLATFORM_REFUND_TAG); // 标记平台退款标志

            Refund update = new Refund();
            update.setId(refundId);
            update.setFee(refundFee);
            update.setTags(tages);
            Response<Boolean> response = refundWriteService.update(update);
            if (!response.isSuccess()) {
                log.error("update refund :{} fail,error:{}", update, response.getError());
                throw new JsonResponseException(response.getError());
            }

            ParanaUser seller = UserUtil.getCurrentUser();
            seller.setShopId(resp.getResult().getShopId());//运营后台退款强制塞入退款单的店铺ID,按照SKU退款数据库记录的是供应商的shopId
            refundLogic.agreeRefund(refundId, seller, VegaOrderEvent.REFUND_APPLY_ADMIN_AGREE.toOrderOperation());
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
     * 退款单审核拒绝
     *
     * @param refundId 退款单id
     * @param sellerNote 卖家拒绝原因
     */
    @RequestMapping(value = "/api/vega/admin/refund/{refundId}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void rejectRefund(@PathVariable("refundId") Long refundId,@RequestParam(required = false) String sellerNote) {
        try {
            Response<Refund> resp = refundReadService.findById(refundId);
            if (!resp.isSuccess()) {
                log.error("failed to find refund by refundId = {}, cause : {}", refundId, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            ParanaUser seller = UserUtil.getCurrentUser();
            seller.setShopId(resp.getResult().getShopId());//运营后台退款强制塞入退款单的店铺ID,按照SKU退款数据库记录的是供应商的shopId
            refundLogic.rejectRefund(refundId, seller, VegaOrderEvent.REFUND_APPLY_ADMIN_REJECT.toOrderOperation());
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
     * 退款预处理
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/vega/admin/refund/{refundId}/pre", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void preRefund(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            Refund refund =commonRefundLogic.getRefund(refundId);
            seller.setShopId(refund.getShopId());//运营后台退款强制塞入退款单的店铺ID,按照SKU退款数据库记录的是供应商的shopId
            Map<String, String> tages = refund.getTags();
            String isShiped = tages.get(SystemConstant.IS_SHIPPED);
            Integer orderStatus = VegaOrderStatus.REFUND.getValue();
            if(isShiped.equals(SystemConstant.SHIPPED)){
                orderStatus = VegaOrderStatus.RETURN_REFUND.getValue();
            }
            commonRefundLogic.preRefund(refundId, seller, VegaOrderEvent.REFUND_ADMIN.toOrderOperation(), orderStatus);
            if(refund.getChannel().contains(AllinpayChannels.CHANNEL)) {
                Refund update = new Refund();
                update.setId(refundId);
                update.setSellerNote(SystemConstant.ALLINPAY_REFUND_APPLY);
                Response<Boolean> response = refundWriteService.update(update);
                if (!response.isSuccess()) {
                    log.error("update refund :{} fail,error:{}", update, response.getError());
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



}

