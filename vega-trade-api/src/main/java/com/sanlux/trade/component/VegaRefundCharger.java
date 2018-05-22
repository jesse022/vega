package com.sanlux.trade.component;

import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.api.RefundCharger;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 计算退款金额, 如果是总单退款, 分为发货前退款和发货后退款.
 * 发货前退款的公式为:
 * shopOrder.getOriginFee() + shopOrder.getShipFee() - paymentDiscount - shopOrder.getDiscount()
 * 发货后的退款公式为:
 * shopOrder.getOriginFee() - paymentDiscount - shopOrder.getDiscount()
 * 平台优惠(paymentDiscount) 计算公式为:
 * payment.getDiscount() * (shopOrder.getOriginFee() - shopOrder.getDiscount())  / payment.getOriginFee()
 *
 * 如果是子单退款,同样的分为发货前和发货后.
 * 发货前公式为:
 * skuOrder.getOriginFee() + shipFee - paymentDiscount - shopDiscount - skuOrder.getDiscount();
 * 发货后公式为:
 * skuOrder.getOriginFee() - paymentDiscount - shopDiscount - skuOrder.getDiscount();
 * 平台优惠(paymentDiscount)计算公式为:
 * payment.getDiscount() * (skuOrder.getOriginFee() - skuOrder.getDiscount()-shopDiscount) / payment.getOriginFee();
 * 店铺优惠(shopDiscount) 计算公式为:
 * shopOrder.getDiscount() * (skuOrder.getOriginFee() - skuOrder.getDiscount()) / shopOrder.getOriginFee();
 * 运费(shipFee)计算公式为:
 * shopOrder.getShipFee() * skuOrder.getOriginFee() / realShopOriginFee;
 *
 * 需要注意!!!
 * 这里的支付单(payment)的原价(originFee),是减去了所有和其关联的店铺优惠,商品优惠之后的金额
 * 店铺订单(shopOrder),是减去了所有和其关联的商品优惠之后的金额
 *
 * 所以最后计算运费的公式中 realShopOriginFee 其实是shopOrder.getOriginFee() + 所有skuOrder.discount() = 所有skuOrder.getOriginFee()
 * Mail: F@terminus.io
 * Data: 16/7/11
 * Author: yangzefeng
 */
@Slf4j
public class VegaRefundCharger implements RefundCharger {

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    /**
     * 计算退款金额, 只能一个总订单退款,或者一个总订单下面的多个子订单一起退款, 默认就是返回支付金额不考虑营销
     * @param payment 支付单
     * @param shopOrder 总订单
     * @param skuOrders 子订单
     * @return 退款金额
     */
    @Override
    public List<Long> charge(Payment payment, ShopOrder shopOrder, List<SkuOrder> skuOrders) {

        List<Long> refunds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(skuOrders)) {
            for (SkuOrder skuOrder : skuOrders) {
                refunds.add(calculateSkuOrderDiscount(payment, skuOrder));
            }
            return refunds;
        }

        if (shopOrder != null) {
            refunds.add(calculateShopOrderDiscount(payment, shopOrder));
            return refunds;
        }

        throw new JsonResponseException("refund.charger.fail");
    }

    private long calculateShopOrderDiscount(Payment payment, ShopOrder shopOrder) {
        //拆平台优惠
        long paymentDiscount = 0L;
        if (payment.getDiscount() != null) {
            long shopOrderFee = calculateShopOrderFee(shopOrder);
            paymentDiscount =
                    new BigDecimal(payment.getDiscount())
                    .multiply(new BigDecimal(shopOrderFee))
                    .divide(new BigDecimal(payment.getOriginFee()), BigDecimal.ROUND_HALF_UP).longValue();
        }

        long shopDiscount = 0L;
        if (shopOrder.getDiscount() != null) {
            shopDiscount = shopOrder.getDiscount();
        }

        //运费, 发货前可退,未发货不退
        if (!Objects.equals(shopOrder.getStatus(), VegaOrderStatus.SHIPPED.getValue())) {
            return shopOrder.getOriginFee()
                   + shopOrder.getShipFee()
                   - paymentDiscount
                   - shopDiscount;
        }
        if (Objects.equals(shopOrder.getStatus(), VegaOrderStatus.SHIPPED.getValue())) {
            return shopOrder.getOriginFee()
                   - paymentDiscount
                   - shopDiscount;
        }

        throw new JsonResponseException("status.not.support.refund");
    }

    private long calculateSkuOrderDiscount(Payment payment, SkuOrder skuOrder) {
        Response<ShopOrder> shopOrderR = shopOrderReadService.findById(skuOrder.getOrderId());
        if (!shopOrderR.isSuccess()) {
            log.error("fail to find shopOrder by id {}, error code:{}",
                    skuOrder.getOrderId(), shopOrderR.getError());
            throw new JsonResponseException(shopOrderR.getError());
        }
        ShopOrder shopOrder = shopOrderR.getResult();

        Response<List<SkuOrder>> skuOrdersR = skuOrderReadService.findByShopOrderId(shopOrder.getId());
        if (!skuOrdersR.isSuccess()) {
            log.error("fail to find skuOrders by shopOrder id {}, error code:{}",
                    shopOrder.getId(), skuOrdersR.getError());
            throw new JsonResponseException(skuOrdersR.getError());
        }
        List<SkuOrder> skuOrders = skuOrdersR.getResult();
        long realShopOriginFee = 0;
        for (SkuOrder so : skuOrders) {
            realShopOriginFee += so.getOriginFee();
        }

        //拆店铺优惠
        long shopDiscount = 0L;
        if (shopOrder.getDiscount() != null) {
            long skuFee = calculateSkuOrderFee(skuOrder);
            shopDiscount =
                    new BigDecimal(shopOrder.getDiscount())
                            .multiply(new BigDecimal(skuFee))
                            .divide(new BigDecimal(shopOrder.getOriginFee()), BigDecimal.ROUND_HALF_UP).longValue();
        }

        //拆平台优惠
        long paymentDiscount = 0L;
        if (payment.getDiscount() != null) {
            long skuFee = calculateSkuOrderFee(skuOrder);
            paymentDiscount =
                    new BigDecimal(payment.getDiscount())
                    .multiply(new BigDecimal(skuFee-shopDiscount))
                    .divide(new BigDecimal(payment.getOriginFee()), BigDecimal.ROUND_HALF_UP).longValue();
        }

        long skuDisCount = 0L;
        if (skuOrder.getDiscount() != null) {
            skuDisCount = skuOrder.getDiscount();
        }

        //运费, 发货前可退,未发货不退
        long shipFee =
                new BigDecimal(shopOrder.getShipFee())
                .multiply(new BigDecimal(skuOrder.getOriginFee()))
                .divide(new BigDecimal(realShopOriginFee), BigDecimal.ROUND_HALF_UP).longValue();

        if (!Objects.equals(skuOrder.getStatus(), VegaOrderStatus.SHIPPED.getValue())) {
            return skuOrder.getOriginFee() + shipFee - paymentDiscount - shopDiscount - skuDisCount;
        }
        if (Objects.equals(skuOrder.getStatus(), VegaOrderStatus.SHIPPED.getValue())) {
            return skuOrder.getOriginFee() - paymentDiscount - shopDiscount - skuDisCount;
        }

        throw new JsonResponseException("status.not.support.refund");
    }

    private long calculateSkuOrderFee(SkuOrder skuOrder) {
        long skuFee = skuOrder.getOriginFee();
        if (skuOrder.getDiscount() != null) {
            skuFee = skuOrder.getOriginFee() - skuOrder.getDiscount();
        }
        return skuFee;
    }

    private long calculateShopOrderFee(ShopOrder shopOrder) {
        long shopOrderFee = shopOrder.getOriginFee();
        if (shopOrder.getDiscount() != null) {
            shopOrderFee = shopOrder.getOriginFee() - shopOrder.getDiscount();
        }
        return shopOrderFee;
    }
}
