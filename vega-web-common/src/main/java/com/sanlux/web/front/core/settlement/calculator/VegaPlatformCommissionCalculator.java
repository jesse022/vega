/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.calculator;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SettleConstants;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.service.OrderDispatchRelationReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.settle.api.PlatformCommissionCalculator;
import io.terminus.parana.settle.dto.PlatformCommissionTree;
import io.terminus.parana.settle.dto.RefundEntry;
import io.terminus.parana.settle.dto.SettleFeeDetail;
import io.terminus.parana.settle.dto.SettleFeeDetailTree;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.enums.RefundLevelType;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static io.terminus.parana.settle.api.SettleFeeDetailCalculator.split;

/**
 * @author : panxin
 */
@Slf4j
@Primary
@Component
public class VegaPlatformCommissionCalculator implements PlatformCommissionCalculator {

    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;

    @Override
    public PlatformCommissionTree calculate(SettleTrade settleTrade, SettleFeeDetailTree feeDetailTree) {
        PlatformCommissionTree tree = new PlatformCommissionTree();

        OrderDispatchRelation dispatchRelation = null; // 派送关系
        Long platformCommission = 0L; // 平台抽佣
        Shop dispatcher = null; // 派单人
        Shop receiver = null; // 接单人

        // 平台店铺, 查询佣金率
        Shop platformShop = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = platformShop.getTags();

        // 平台抽佣率
        Long platformCommissionRate = tags.get(SettleConstants.PLATFORM_COMMISSION_RATE) == null ?
                0 : Long.valueOf(tags.get(SettleConstants.PLATFORM_COMMISSION_RATE));
        log.info("platformCommissionRate = {} ", platformCommissionRate);

        val groups = settleTrade.getSkuOrdersGroupByShopOrderId();
        for (ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()) {
            SettleFeeDetail shopDto = feeDetailTree.getShopOrderFeeMap().get(shopOrder.getId());
            Long shopCommission = 0L;

            // vega Order start ....

            // 查找派送关系
            Optional<OrderDispatchRelation> opt = findDispatchOrderRelationByOrder(shopOrder);

            // 查不到派送信息则是下到自己店铺的订单, 平台抽佣 = 0
            if (!opt.isPresent()) {
                tree.getShopOrderCommissionMap().put(shopOrder.getId(), shopCommission);
                for (SkuOrder skuOrder : groups.get(shopOrder.getId())) {
                    tree.getSkuOrderCommissionMap().put(skuOrder.getId(), shopCommission);
                }
                log.info("can not find dispatch order relation, platformCommission = {}", platformCommission);
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
                if (opt.isPresent()) {
                    // 找到了则是平台派给一级, 一级再派给二级, 平台抽佣, 一级经销商抽佣
                    Long commissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                            MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
                    platformCommission += split(commissionBase, platformCommissionRate);
                    shopCommission = split(commissionBase, platformCommissionRate);
                    log.info("平台派单给一级经销商, 一级经销商再派单给二级经销商, 平台抽佣: {}", platformCommission);
                } // else  不用管
            }

            // 平台派的单, 平台抽佣
            if (ShopTypeHelper.isPlatformShop(dispatcher.getType())) {
                // 派给供应商, 平台赚差价
                if (ShopTypeHelper.isSupplierShop(receiver.getType())) {
                    List<SkuOrder> skuOrders = groups.get(shopOrder.getId());
                    for (SkuOrder skuOrder : skuOrders) {
                        // 下单时Sku最终价格
                        Long orderSkuPrice = Long.valueOf(skuOrder.getTags().get(SystemConstant.ORDER_SKU_PRICE));
                        // 下单时Sku供货价
                        Long orderSkuSellerPrice = Long.valueOf(skuOrder.getTags().get(SystemConstant.ORDER_SKU_SELLER_PRICE));
                        // 差价
                        // Long diffFee = (orderSkuPrice - orderSkuSellerPrice);
                        Long diffFee = (orderSkuPrice - orderSkuSellerPrice);
                        platformCommission += diffFee * skuOrder.getQuantity();
                        shopCommission += (orderSkuPrice - orderSkuSellerPrice) * skuOrder.getQuantity();
                        log.info("sku orderSkuPrice = {}, orderSkuSellerPrice ={}, 差价 = {}",
                                orderSkuPrice, orderSkuSellerPrice, diffFee);
                    }

                    log.info("平台派单给供应商, 抽佣赚差价: {}", platformCommission);
                }

                // 平台派给一级经销商, 平台抽佣
                if (ShopTypeHelper.isFirstDealerShop(receiver.getType())) {
                    Long platformCommissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                            MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
                    platformCommission += split(platformCommissionBase, platformCommissionRate);
                    shopCommission = split(platformCommissionBase, platformCommissionRate);
                    log.info("平台派单给一级经销商, 平台抽佣: {}", platformCommission);
                }
            }

            // vega Order end ....

            for (SkuOrder skuOrder : groups.get(shopOrder.getId())) {
                SettleFeeDetail skuDto = feeDetailTree.getSkuOrderFeeMap().get(skuOrder.getId());
                Long skuCommission = split(shopCommission, shopDto.getSellerOriginIncome(), skuDto.getSellerOriginIncome());
                tree.getSkuOrderCommissionMap().put(skuOrder.getId(), skuCommission);
            }

            tree.getShopOrderCommissionMap().put(shopOrder.getId(), shopCommission);
        }
        tree.setRootCommission(platformCommission);

        for (Long paymentId : settleTrade.getPaymentByIdMap().keySet()) {
            tree.getPaymentCommissionMap().put(paymentId, tree.getRootCommission());
        }

        for (RefundEntry refundEntry : settleTrade.getRefundByIdMap().values()) {

            long refundCommission = 0L;
            switch (RefundLevelType.from(refundEntry.getRefundType())) {
                case PerShopOrder:
                    refundCommission = tree.getShopOrderCommissionMap().get(refundEntry.getShopOrder().getId());
                    break;
                case PerSkuOrder:
                    refundCommission = tree.getSkuOrderCommissionMap().get(refundEntry.getSkuOrder().getId());
                    break;
                case MultiSkuOrder:
                    refundCommission = refundEntry.getSkuOrderList().stream()
                            .mapToLong(x -> tree.getSkuOrderCommissionMap().get(x.getId()))
                            .sum();
                    break;
            }
            tree.getRefundCommissionMap().put(refundEntry.getRefund().getId(), refundCommission);
        }

        return tree;
    }

    private Long calculateCommission(SettleTrade settleTrade) {
        OrderDispatchRelation dispatchRelation = null;
        Shop dispatcher = null; // 派单人
        Shop receiver = null; // 接单人

        Long platformCommission = 0L; // 平台抽佣
        Long firstDealerCommission = 0L; // 一级经销商抽佣

        Shop platformShop = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = platformShop.getTags();

        // 平台抽佣率
        Long platformCommissionRate = tags.get(SettleConstants.PLATFORM_COMMISSION_RATE) == null ?
                0 : Long.valueOf(tags.get(SettleConstants.PLATFORM_COMMISSION_RATE));
        // 一级经销商抽佣率
        Long firstDealerCommissionRate = tags.get(SettleConstants.FIRST_DEALER_COMMISSION_RATE) == null ?
                0 : Long.valueOf(tags.get(SettleConstants.FIRST_DEALER_COMMISSION_RATE));

        log.info("platformCommissionRate = {}, firstDealerCommissionRate = {}",
                platformCommissionRate, firstDealerCommissionRate);

        for (ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()) {
            Optional<OrderDispatchRelation> opt = findDispatchOrderRelationByOrder(shopOrder);

            // 查不到派送信息则是下到自己店铺的订单, 平台抽佣 = 0, 店铺(经销商)抽佣 = 0
            if (!opt.isPresent()) {
                log.info("can not find dispatch order relation, platformCommission = {}, firstDealerCommission = {}",
                        platformCommission, firstDealerCommission);
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
//                    firstDealerCommission += SettleFeeUtil.getPlatformCommission(firstDealerCommissionBase,
//                            firstDealerCommissionRate);

                    log.info("一级经销商派单给二级经销商, 一级经销商抽佣: {}", firstDealerCommission);
                    continue;
                }

                // 找到了则是平台派给一级, 一级再派给二级, 平台抽佣, 一级经销商抽佣
                Long commissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                        MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
//                firstDealerCommission += SettleFeeUtil.getPlatformCommission(commissionBase,
//                        firstDealerCommissionRate);
//                platformCommission += SettleFeeUtil.getPlatformCommission(commissionBase,
//                        platformCommissionRate);

                log.info("平台派单给一级经销商, 一级经销商再派单给二级经销商, 平台抽佣: {}, 一级经销商抽佣: {}", platformCommission, firstDealerCommission);
                continue;
            }

            // 平台派的单, 平台抽佣
            if (ShopTypeHelper.isPlatformShop(dispatcher.getType())) {
                // 派给供应商, 平台赚差价
                if (ShopTypeHelper.isSupplierShop(receiver.getType())) {
                    List<SkuOrder> skuOrders = settleTrade.getSkuOrdersGroupByShopOrderId().get(shopOrder.getId());
                    for (SkuOrder skuOrder : skuOrders) {
                        // 下单时Sku最终价格
                        Long orderSkuPrice = Long.valueOf(skuOrder.getTags().get(SystemConstant.ORDER_SKU_PRICE));
                        // 下单时Sku供货价
                        Long orderSkuSellerPrice = Long.valueOf(skuOrder.getTags().get(SystemConstant.ORDER_SKU_SELLER_PRICE));
                        // 差价
                        platformCommission += (orderSkuPrice - orderSkuSellerPrice) * skuOrder.getQuantity();
                    }

                    log.info("平台派单给供应商, 抽佣赚差价: {}", platformCommission);
                    continue;
                }

                // 平台派给一级经销商, 平台抽佣
                if (ShopTypeHelper.isFirstDealerShop(receiver.getType())) {
                    Long platformCommissionBase = shopOrder.getOriginFee() + shopOrder.getShipFee() -
                            MoreObjects.firstNonNull(shopOrder.getDiscount(), 0);
//                    platformCommission += SettleFeeUtil.getPlatformCommission(platformCommissionBase,
//                            platformCommissionRate);

                    log.info("平台派单给一级经销商, 平台抽佣: {}", platformCommission);
                }
            }
        }

        log.info("platformCommission = {}, firstDealerCommission = {}", platformCommission, firstDealerCommission);

        // 经销商抽佣
        Map<String, String> extra = Maps.newHashMap();
        extra.put(SettleConstants.DEALER_COMMISSION, String.valueOf(firstDealerCommission));
//        settleFee.setExtra(extra);
//         平台抽佣
//        settleFee.setPlatformCommission(platformCommission);

        return 0L;
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
