package com.sanlux.web.front.controller.trade;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import io.terminus.parana.web.core.component.order.ReturnLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Desc: 退货有关操作
 * Mail: F@terminus.io
 * Data: 16/5/30
 * Author: yangzefeng
 */
@RestController
@Slf4j
public class VegaReturns {

    private final ReturnLogic returnLogic;
    private final EventBus eventBus;
    private final CommonRefundLogic commonRefundLogic;
    private final VegaOrderComponent vegaOrderComponent;


    @Autowired
    public VegaReturns(ReturnLogic returnLogic,EventBus eventBus,CommonRefundLogic commonRefundLogic,
                       VegaOrderComponent vegaOrderComponent) {
        this.returnLogic = returnLogic;
        this.eventBus=eventBus;
        this.commonRefundLogic=commonRefundLogic;
        this.vegaOrderComponent=vegaOrderComponent;
    }

    /**
     * 当前不能跨订单申请退货, 但是支持同一订单的多个子订单申请退货
     *
     * @param buyerNote   买家备注
     * @param orderIds    (子)订单列表
     * @param orderType   订单级别
     * @return  创建的退款单id
     */
    @RequestMapping(value = "/api/vega/buyer/order/return", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long applyReturn(@RequestParam(value = "buyerNote", required = false) String buyerNote,
                            @RequestParam("orderIds") List<Long> orderIds,
                            @RequestParam(value = "orderType", defaultValue = "2") Integer orderType) {
        if(CollectionUtils.isEmpty(orderIds)){
            log.error("no orderIds specified when apply return");
            throw new JsonResponseException("order.return.fail");
        }
        try {
            ParanaUser buyer = UserUtil.getCurrentUser();
            OrderLevel orderLevel = OrderLevel.fromInt(orderType);
            Response<Long> orderIdRes = vegaOrderComponent.getShopOrderId(orderIds.get(0),orderType);
            if(!orderIdRes.isSuccess()) {
                log.error("send sms message fail,because shop order id not found by order id:{} order type:{}", orderIds.get(0), orderType);
            }else {
                //短信提醒事件
                eventBus.post(new TradeSmsEvent(orderIdRes.getResult(), TradeSmsNodeEnum.APPLY_RETURN));
            }

            return returnLogic.applyReturn(orderIds, orderLevel,buyer, buyerNote, VegaOrderEvent.RETURN_APPLY.toOrderOperation(),
                    VegaOrderStatus.RETURN_APPLY.getValue());
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("failed to  apply return for order(ids={}, level={}), cause:{}",
                    orderIds, orderType, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.fail");
        }
    }

    /**
     * 退货单审核通过
     *
     * @param refundId    退款单id
     */
    @RequestMapping(value = "/api/vega/seller/return/{refundId}/agree", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void agreeReturnApply(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            returnLogic.agreeReturn(refundId, seller, VegaOrderEvent.RETURN_APPLY_AGREE.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.AGREE_RETURN));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to agree return by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.agree.fail");
        }
    }

    /**
     * 拒绝退货申请
     *
     * @param refundId    退款单id
     * @param sellerNote 卖家备注
     */
    @RequestMapping(value = "/api/vega/seller/return/{refundId}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void rejectReturnApply(@PathVariable("refundId") Long refundId,@RequestParam(required = false) String sellerNote) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            returnLogic.rejectReturnApply(refundId, seller, VegaOrderEvent.RETURN_APPLY_REJECT.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.REJECT_RETURN));
            vegaOrderComponent.setRefundSellerNote(refundId,sellerNote);//添加拒绝备注
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to reject return apply by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.apply.reject.fail");
        }
    }

    /**
     * 取消退款申请
     *
     * @param refundId    退款单id
     */
    @RequestMapping(value = "/api/vega/buyer/return/{refundId}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void cancelReturn(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser buyer = UserUtil.getCurrentUser();
            returnLogic.cancelReturn(refundId, buyer, VegaOrderEvent.RETURN_APPLY_CANCEL.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.BUYER_CANCEL_RETURN));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to cancel return by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.cancel.fail");
        }
    }

    /**
     * 退货发货
     *
     * @param refundId    退款单id
     * @param shipmentCorpCode  物流公司编号
     * @param shipmentSerialNo    物流单号
     */
    @RequestMapping(value = "/api/vega/buyer/return/{refundId}/deliver", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void returns(@PathVariable("refundId") Long refundId,@RequestParam(value = "corpCode",required = false) String shipmentCorpCode,
                        @RequestParam(value = "serialNo",required = false) String shipmentSerialNo,
                        @RequestParam(value = "annexUrl", required = false) String annexUrl) {
        try {
            ParanaUser buyer = UserUtil.getCurrentUser();
            returnLogic.returns(refundId, buyer, VegaOrderEvent.RETURN.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.BUYER_RETURN));

            vegaOrderComponent.setRefundExpressInfo(refundId,shipmentCorpCode,shipmentSerialNo,annexUrl);//物流公司
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to returns by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.fail");
        }
    }

    /**
     * 退货拒收
     *
     * @param refundId    退款单id
     * @param sellerNote 卖家备注
     */
    @RequestMapping(value = "/api/vega/seller/return/{refundId}/reject-return", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void rejectReturn(@PathVariable("refundId") Long refundId,@RequestParam(required = false) String sellerNote) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            returnLogic.rejectReturn(refundId, seller, VegaOrderEvent.RETURN_REJECT.toOrderOperation());
            vegaOrderComponent.setRefundSellerNote(refundId,sellerNote);//添加拒绝备注
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to reject return by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.reject.fail");
        }
    }

    /**
     * 退货确认收货
     *
     * @param refundId    退款单id
     */
    @RequestMapping(value = "/api/vega/seller/return/{refundId}/confirm-return", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void confirmReturn(@PathVariable("refundId") Long refundId) {
        try {
            ParanaUser seller = UserUtil.getCurrentUser();
            returnLogic.confirmReturn(refundId, seller, VegaOrderEvent.RETURN_CONFIRM.toOrderOperation());
            ShopOrder shopOrder = commonRefundLogic.findShopOrderByRefundId(refundId);
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(shopOrder.getId(), TradeSmsNodeEnum.SELLER_RECEIVE_RETURN));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("fail to confirm return by refund id={}, cause:{}",
                    refundId, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("order.return.confirm.fail");
        }
    }


}
