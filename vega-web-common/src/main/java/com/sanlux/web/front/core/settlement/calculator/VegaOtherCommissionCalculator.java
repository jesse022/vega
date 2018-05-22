/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.calculator;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SettleConstants;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.service.OrderDispatchRelationReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.settle.api.OtherCommissionCalculator;
import io.terminus.parana.settle.dto.OtherCommission;
import io.terminus.parana.settle.dto.OtherCommissionTree;
import io.terminus.parana.settle.dto.RefundEntry;
import io.terminus.parana.settle.dto.SettleFeeDetailTree;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.enums.RefundLevelType;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.terminus.parana.settle.api.SettleFeeDetailCalculator.split;

/**
 * @author : panxin
 */
@Slf4j
@Primary
@Component
public class VegaOtherCommissionCalculator implements OtherCommissionCalculator {

    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;
    @RpcConsumer
    private PaymentReadService paymentReadService;

    @Override
    public OtherCommissionTree calculate(SettleTrade settleTrade, SettleFeeDetailTree feeDetailTree) {
//        OtherCommissionTree tree = new OtherCommissionTree();
//        tree.setRootCommission(new OtherCommission());
//        for(Payment payment : settleTrade.getPaymentByIdMap().values()){
//            tree.getPaymentCommissionMap().put(payment.getId(), new OtherCommission());
//        }
//        for(RefundEntry refundEntry : settleTrade.getRefundByIdMap().values()){
//            tree.getRefundCommissionMap().put(refundEntry.getRefund().getId(), new OtherCommission());
//        }
//        for(ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()){
//            tree.getShopOrderCommissionMap().put(shopOrder.getId(), new OtherCommission());
//        }
//        for(SkuOrder skuOrder : settleTrade.getSkuOrderList()){
//            tree.getSkuOrderCommissionMap().put(skuOrder.getId(), new OtherCommission());
//        }

        return generateOtherCommission(settleTrade, feeDetailTree);
    }


    private OtherCommissionTree generateOtherCommission(SettleTrade settleTrade, SettleFeeDetailTree feeDetailTree) {
        OtherCommissionTree otherCommissionTree = new OtherCommissionTree(); // 佣金
        OrderDispatchRelation dispatchRelation = null; // 派送关系
        Long firstDealerCommission = 0L; // 一级经销商抽佣
        Shop dispatcher = null; // 派单人
        Shop receiver = null; // 接单人

        Shop platformShop = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = platformShop.getTags();

        // 一级经销商抽佣率
        Long firstDealerCommissionRate = tags.get(SettleConstants.FIRST_DEALER_COMMISSION_RATE) == null ?
                0 : Long.valueOf(tags.get(SettleConstants.FIRST_DEALER_COMMISSION_RATE));
        log.info("firstDealerCommissionRate = {}", firstDealerCommissionRate);

        OtherCommission otherCommission = null;
        //otherCommission.setCommission1(firstDealerCommission);
        for (ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()) {
            Optional<OrderDispatchRelation> opt = findDispatchOrderRelationByOrder(shopOrder);

            // 查不到派送信息则是下到自己店铺的订单, 平台抽佣 = 0, 店铺(经销商)抽佣 = 0
            if (!opt.isPresent()) {
                log.info("can not find dispatch order relation, firstDealerCommission = {}", firstDealerCommission);
                continue;
            }

            // 有信息在判断派送这的信息和当前店铺的信息来计算抽佣
            dispatchRelation = opt.get();
            Long dispatchShopId = dispatchRelation.getDispatchShopId();
            Long receiverShopId = dispatchRelation.getReceiveShopId();
            dispatcher = findShopById(receiverShopId); // 派单人
            receiver = findShopById(dispatchShopId); // 接单人

            log.info("派单人: {}", dispatcher);
            log.info("接单人: {}", receiver);

            // 一级经销商派送的订单
            if (ShopTypeHelper.isFirstDealerShop(dispatcher.getType())) {
                // 再判断是否是由平台派给一级的
                opt = findDispatchOrderRelationByOrderIdAndShopId(shopOrder.getId(), receiverShopId);

                // 找不到就是一级派给二级的单, 一级经销商抽佣
                if (!opt.isPresent()) {
                    Long firstDealerCommissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                            MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
                    firstDealerCommission += split(firstDealerCommissionBase, firstDealerCommissionRate);

                    log.info("一级经销商派单给二级经销商, 一级经销商抽佣: {}", firstDealerCommission);
                    continue;
                }

                // 找到了则是平台派给一级, 一级再派给二级, 平台抽佣, 一级经销商抽佣
                Long commissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                        MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
                firstDealerCommission += split(commissionBase, firstDealerCommissionRate);
                log.info("平台派单给一级经销商, 一级经销商再派单给二级经销商, 一级经销商抽佣: {}", firstDealerCommission);
            }
            // 其他则肯定是平台派的单, 这里不用管, 只计算经销商抽佣
        }
        otherCommission = new OtherCommission();
        otherCommission.setCommission1(firstDealerCommission);
        otherCommissionTree.setRootCommission(otherCommission);

        for (Payment payment : settleTrade.getPaymentByIdMap().values()) {
            otherCommissionTree.getPaymentCommissionMap().put(payment.getId(), otherCommission);
        }

        for(RefundEntry refundEntry : settleTrade.getRefundByIdMap().values()){
            long refundCommission = 0L;
            Long refundId = refundEntry.getRefund().getId();
            Long originalFee = 0L;
            Long shipFee = 0L;
            switch (RefundLevelType.from(refundEntry.getRefundType())) {
                case PerShopOrder:
                    originalFee = feeDetailTree.getRefundFeeMap().get(refundId).getOriginFee();
                    shipFee = feeDetailTree.getRefundFeeMap().get(refundId).getShipFee();
                    refundCommission = split(originalFee + shipFee, firstDealerCommissionRate);
                    log.debug("总单退款 = {}, refundId = {}", refundCommission, refundId);
                    //refundCommission = otherCommissionTree.getShopOrderCommissionMap().get(refundEntry.getShopOrder().getId()).getCommission1();
                    break;
                case PerSkuOrder:
                    originalFee = feeDetailTree.getRefundFeeMap().get(refundId).getOriginFee();
                    shipFee = feeDetailTree.getRefundFeeMap().get(refundId).getShipFee();
                    refundCommission = split(originalFee + shipFee, firstDealerCommissionRate);
                    log.debug("子单退款 = {}, refundId = {}, (一个子单)", refundCommission, refundId);
                    //feeDetailTree.getSkuOrderFeeMap().get(refundEntry.getSkuOrder().getId()).getId();
                    //refundCommission = otherCommissionTree.getSkuOrderCommissionMap().get(refundEntry.getSkuOrder().getId()).getCommission1();
                    break;
                case MultiSkuOrder:
                    // 暂时都没有这个功能哟
                    //log.info("子单退款 = {}, refundId = {}, (多个子单)", refundCommission, refundId);
                    //refundCommission = refundEntry.getSkuOrderList().stream()
                    //        .mapToLong(x -> otherCommissionTree.getSkuOrderCommissionMap().get(x.getId()).getCommission1())
                    //        .sum();
                    break;
            }
            OtherCommission refundOthComm = new OtherCommission();
            refundOthComm.setCommission1(refundCommission);
            otherCommissionTree.getRefundCommissionMap().put(refundEntry.getRefund().getId(), refundOthComm);
        }

        for(ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()){
            otherCommissionTree.getShopOrderCommissionMap().put(shopOrder.getId(), otherCommission);
        }

        for(SkuOrder skuOrder : settleTrade.getSkuOrderList()){
            otherCommissionTree.getSkuOrderCommissionMap().put(skuOrder.getId(), otherCommission);
        }

        return otherCommissionTree;
    }

    /**
     * 查询派送关系
     *
     * @param shopOrder 订单
     * @return 信息
     */
    private Optional<OrderDispatchRelation> findDispatchOrderRelationByOrder(ShopOrder shopOrder) {
        Long orderId = shopOrder.getId();
        Long dispatchShopId = shopOrder.getShopId();
        return findDispatchOrderRelationByOrderIdAndShopId(orderId, dispatchShopId);
    }

    /**
     * 查询派送关系
     *
     * @param orderId        订单id
     * @param dispatchShopId 派送者店铺ID
     * @return 信息
     */
    private Optional<OrderDispatchRelation> findDispatchOrderRelationByOrderIdAndShopId(Long orderId,
                                                                                        Long dispatchShopId) {
        Response<Optional<OrderDispatchRelation>> resp = orderDispatchRelationReadService.
                findByOrderIdAndDispatchShopId(orderId, dispatchShopId);
        if (!resp.isSuccess()) {
            log.error("failed to find order dispatch relation by orderId = {}, dispatchShopId = {}, cause : {}",
                    orderId, dispatchShopId, resp.getError());
            return Optional.absent();
        }
        return resp.getResult();
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
}
