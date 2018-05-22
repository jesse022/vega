/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.calculator;

import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.settle.api.SettleFeeDetailCalculator;
import io.terminus.parana.settle.dto.RefundEntry;
import io.terminus.parana.settle.dto.SettleFeeDetail;
import io.terminus.parana.settle.dto.SettleFeeDetailTree;
import io.terminus.parana.settle.dto.SettleTrade;
import io.terminus.parana.settle.enums.PayType;
import io.terminus.parana.settle.enums.RefundLevelType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author : panxin
 */
@Slf4j
@Primary
@Component
public class VegaSettleFeeDetailCalculator implements SettleFeeDetailCalculator {
    @Override
    public SettleFeeDetailTree calculate(SettleTrade settleTrade) {
        if(settleTrade.getPayType().equals(PayType.StagePay.value())){
            return stagePay(settleTrade);
        }else {
            return normalOrMergePay(settleTrade);
        }
    }

    private SettleFeeDetailTree normalOrMergePay(SettleTrade settleTrade){

        SettleFeeDetailTree tree = new SettleFeeDetailTree();

        Payment payment = settleTrade.getPaymentByIdMap().values().stream().findFirst().get();

        SettleFeeDetail root = calcRoot(settleTrade);
        tree.setRoot(root);

        for(ShopOrder shopOrder : settleTrade.getShopOrderByIdMap().values()){
            List<SkuOrder> curSkuOrders = settleTrade.getSkuOrdersGroupByShopOrderId().get(shopOrder.getId());

            SettleFeeDetail shopDto = new SettleFeeDetail();
            tree.getShopOrderFeeMap().put(shopOrder.getId(), shopDto);
            shopDto.setId(shopOrder.getId());
            shopDto.setOriginFee(curSkuOrders.stream().mapToLong(SkuOrder::getOriginFee).sum());
            shopDto.setShipFee(shopOrder.getShipFee().longValue());
            shopDto.setShipFeeDiscount(shopOrder.getOriginShipFee().longValue()-shopOrder.getShipFee().longValue());
            shopDto.setSkuDiscount(curSkuOrders.stream().mapToLong(SkuOrder::getDiscount).sum());
            shopDto.setDiffFee(curSkuOrders.stream().mapToLong(SkuOrder::getDiffFee).sum());
            shopDto.setShopDiscount(shopOrder.getDiscount().longValue());
            shopDto.setPlatformDiscount(split(root.getPlatformDiscount(),
                    payment.getOriginFee(),
                    shopOrder.getOriginFee()-shopOrder.getDiscount()
            ));  //不带运费

            for(SkuOrder skuOrder : curSkuOrders){
                SettleFeeDetail skuDto = new SettleFeeDetail();
                tree.getSkuOrderFeeMap().put(skuOrder.getId(), skuDto);
                skuDto.setOriginFee(skuOrder.getOriginFee());
                skuDto.setShipFee(split(
                        shopOrder.getShipFee().longValue(),
                        shopDto.getOriginFee(),
                        skuOrder.getOriginFee()
                ));//按商品原价拆分
                skuDto.setShipFeeDiscount(split(
                        shopDto.getShipFeeDiscount(),
                        shopDto.getOriginFee(),
                        skuOrder.getOriginFee())); //按商品原价拆分
                skuDto.setSkuDiscount(skuOrder.getDiscount());
                skuDto.setDiffFee(skuOrder.getDiffFee().longValue());
                skuDto.setShopDiscount(split(
                        shopOrder.getDiscount().longValue(),
                        shopOrder.getOriginFee(),
                        skuOrder.getFee())); //按sku优惠后拆分
                skuDto.setPlatformDiscount(split(
                        root.getPlatformDiscount(),
                        payment.getOriginFee(),
                        skuOrder.getFee()-skuDto.getShopDiscount())); //不带运费
            }
        }
        tree.getPaymentFeeMap().put(payment.getId(),
                JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
                        JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(root), SettleFeeDetail.class));

        val shopOrderShipGroups = settleTrade.getShipmentGroupsByShopOrderId();
        val skuOrderShipGroups = settleTrade.getShipmentGroupsBySkuOrderId();
        for(RefundEntry refund : settleTrade.getRefundByIdMap().values()){

            val shopShipmentList = shopOrderShipGroups.getOrDefault(refund.getShopOrder().getId(), Collections.emptyList());

            switch (RefundLevelType.from(refund.getRefundType())) {
                case PerShopOrder:
                    SettleFeeDetail shopOrderFee = tree.getShopOrderFeeMap().get(refund.getShopOrder().getId());
                    SettleFeeDetail shopOrderRefund = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
                            JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shopOrderFee), SettleFeeDetail.class);

                    //如果主订单已经发货, 则不退运费
                    if(!shopShipmentList.isEmpty()){
                        shopOrderRefund.setShipped(true);
                    }

                    tree.getRefundFeeMap().put(refund.getRefund().getId(),shopOrderRefund);
                    break;
                case PerSkuOrder:
                    SettleFeeDetail skuOrderFeeDetail = tree.getSkuOrderFeeMap().get(refund.getSkuOrder().getId());
                    SettleFeeDetail skuOrderRefund = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
                            JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(skuOrderFeeDetail), SettleFeeDetail.class);

                    val skuShipmentList = skuOrderShipGroups.getOrDefault(refund.getSkuOrder().getId(), Collections.emptyList());
                    if(!shopShipmentList.isEmpty() || !skuShipmentList.isEmpty()){ //如果主订单或子订单已经发货,则不退运费
                        skuOrderRefund.setShipped(true);
                    }
                    tree.getRefundFeeMap().put(refund.getRefund().getId(), skuOrderRefund);
                    break;
                case MultiSkuOrder: //这种场景parana还不支持
                    List<SettleFeeDetail> skuFeeDetails = refund.getSkuOrderList().stream()
                            .map(x-> tree.getSkuOrderFeeMap().get(x.getOrderId()))
                            .collect(toList());
                    SettleFeeDetail refundFeeDetail = new SettleFeeDetail();
                    refundFeeDetail.setOriginFee(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getOriginFee).sum());
                    refundFeeDetail.setShipFee(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getShipFee).sum());
                    refundFeeDetail.setShipFeeDiscount(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getShipFeeDiscount).sum());
                    refundFeeDetail.setSkuDiscount(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getSkuDiscount).sum());
                    refundFeeDetail.setShopDiscount(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getShopDiscount).sum());
                    refundFeeDetail.setDiffFee(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getDiffFee).sum());
                    refundFeeDetail.setPlatformDiscount(skuFeeDetails.stream().mapToLong(SettleFeeDetail::getPlatformDiscount).sum());
                    tree.getRefundFeeMap().put(refund.getRefund().getId(),refundFeeDetail);
                    //todo 发货策略不知如何定义
            }
        }
        return tree;
    }

    private SettleFeeDetailTree stagePay(SettleTrade settleTrade){
        SettleFeeDetailTree tree = new SettleFeeDetailTree();

        //root
        SettleFeeDetail root = calcRoot(settleTrade);
        tree.setRoot(root);

        //ShopOrder
        ShopOrder shopOrder = settleTrade.getShopOrderByIdMap().values().iterator().next();
        SettleFeeDetail shopDto = BeanMapper.map(root, SettleFeeDetail.class);
        tree.getShopOrderFeeMap().put(shopOrder.getId(), shopDto);

        //SkuOrder
        for(SkuOrder skuOrder : settleTrade.getSkuOrderList()){
            SettleFeeDetail skuDto = new SettleFeeDetail();
            tree.getSkuOrderFeeMap().put(skuOrder.getId(), skuDto);

            skuDto.setOriginFee(skuOrder.getOriginFee());
            skuDto.setShipFee(split(shopOrder.getShipFee().longValue(),shopDto.getOriginFee(), skuOrder.getOriginFee()));//按商品原价拆分
            skuDto.setShipFeeDiscount(split(shopDto.getShipFeeDiscount(), shopDto.getOriginFee(), skuOrder.getOriginFee())); //按商品原价拆分
            skuDto.setSkuDiscount(skuOrder.getDiscount());
            skuDto.setShopDiscount(split(shopOrder.getDiscount().longValue(), shopOrder.getOriginFee(), skuOrder.getFee())); //按sku优惠后拆分
            long commission = 0L;
            for(Payment payment : settleTrade.getPaymentByIdMap().values()) {
                commission += split(payment.getDiscount().longValue(), shopDto.getOriginFee()-shopDto.getSellerDiscount(), skuOrder.getFee() - skuDto.getShopDiscount()); //不带运费
            }
            skuDto.setPlatformDiscount(commission);
        }

        //payment
        for(Payment payment : settleTrade.getPaymentByIdMap().values()){
            SettleFeeDetail paymentDto = new SettleFeeDetail();
            tree.getPaymentFeeMap().put(payment.getId(), paymentDto);

            BigDecimal rate = getRate(shopDto.getOriginFee()-shopDto.getSellerDiscount(), payment.getOriginFee()); //按支付单原价
            paymentDto.setOriginFee(split(shopDto.getOriginFee(), rate));
            paymentDto.setSkuDiscount(split(shopDto.getSkuDiscount(), rate));
            paymentDto.setShopDiscount(split(shopDto.getShopDiscount(), rate));
            paymentDto.setShipFee(split(shopDto.getShipFee(), rate));
            paymentDto.setShipFeeDiscount(split(shopDto.getShipFeeDiscount(), rate));
            paymentDto.setPlatformDiscount(payment.getDiscount().longValue());
        }

        //refund
        for(RefundEntry refundEntry : settleTrade.getRefundByIdMap().values()){
            switch (RefundLevelType.from(refundEntry.getRefundType())) {
                case StagePerShopOrder:
                    SettleFeeDetail shopOrderRefund = tree.getPaymentFeeMap().get(refundEntry.getRefund().getPaymentId());
                    tree.getRefundFeeMap().put(refundEntry.getRefund().getId(), BeanMapper.map(shopOrderRefund, SettleFeeDetail.class));
                    break;
                case StagePerSkuOrder:
                    SettleFeeDetail skuDto = tree.getSkuOrderFeeMap().get(refundEntry.getSkuOrder().getId());
                    BigDecimal rate = getRate(shopDto.getOriginFee()-shopDto.getSellerDiscount(), refundEntry.getPayment().getOriginFee()); //按支付单原价

                    SettleFeeDetail refundFeeDetail = new SettleFeeDetail();
                    tree.getRefundFeeMap().put(refundEntry.getRefund().getId(), refundFeeDetail);

                    refundFeeDetail.setOriginFee(split(skuDto.getOriginFee(), rate));
                    refundFeeDetail.setSkuDiscount(split(skuDto.getSkuDiscount(), rate));
                    refundFeeDetail.setShopDiscount(split(skuDto.getShopDiscount(), rate));
                    refundFeeDetail.setShipFee(split(skuDto.getShipFee(), rate));
                    refundFeeDetail.setShipFeeDiscount(split(skuDto.getShipFeeDiscount(), rate));
                    refundFeeDetail.setPlatformDiscount(split(refundEntry.getPayment().getDiscount().longValue(), refundEntry.getPayment().getFee(), refundEntry.getRefund().getFee())); //todo
                    break;
            }
        }
        return tree;
    }

    private SettleFeeDetail calcRoot(SettleTrade settleTrade){
        SettleFeeDetail root = new SettleFeeDetail();
        root.setId(0L);
        root.setOriginFee(settleTrade.getSkuOrderList().stream().mapToLong(SkuOrder::getOriginFee).sum());
        root.setShipFee(settleTrade.getShopOrderByIdMap().values().stream().mapToLong(ShopOrder::getShipFee).sum());
        root.setShipFeeDiscount(settleTrade.getShopOrderByIdMap().values().stream().mapToLong(x-> x.getOriginShipFee() - x.getShipFee()).sum());
        root.setSkuDiscount(settleTrade.getSkuOrderList().stream().mapToLong(SkuOrder::getDiscount).sum());
        root.setShopDiscount(settleTrade.getShopOrderByIdMap().values().stream().mapToLong(ShopOrder::getDiscount).sum());
        root.setPlatformDiscount(settleTrade.getPaymentByIdMap().values().stream().mapToLong(Payment::getDiscount).sum());
        root.setDiffFee(settleTrade.getSkuOrderList().stream().mapToLong(SkuOrder::getDiffFee).sum());
        return root;
    }

    private static Long split(Long toSplit, Long total, Long part){
        if(toSplit==null){
            toSplit=0L;
        }
        if(total==null){
            return null;
        }
        if(part == null){
            part=0L;
        }
        return new BigDecimal(toSplit).multiply(
                new BigDecimal(part).divide(new BigDecimal(total), 10, BigDecimal.ROUND_HALF_DOWN))
                .setScale(0, BigDecimal.ROUND_HALF_DOWN)
                .longValue();//取0位小数 向上取整
    }

    static Long split(Long toSplit, Long rate){
        return new BigDecimal(toSplit)
                .multiply(new BigDecimal(rate)
                        .divide(new BigDecimal(10000), 10, BigDecimal.ROUND_HALF_DOWN))
                .setScale(0, BigDecimal.ROUND_HALF_DOWN)
                .longValue();//取0位小数 向上取整
    }

    private static Long split(Long toSplit, BigDecimal rate){
        return new BigDecimal(toSplit)
                .multiply(rate)
                .setScale(0, BigDecimal.ROUND_HALF_DOWN)
                .longValue();//取0位小数 向上取整
    }

    private static BigDecimal getRate(Long total, Long part){
        return new BigDecimal(part).divide(new BigDecimal(total), 10, BigDecimal.ROUND_HALF_DOWN);
    }
}
