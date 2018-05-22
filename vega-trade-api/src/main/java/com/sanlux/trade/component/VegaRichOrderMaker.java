package com.sanlux.trade.component;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.order.dto.*;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.settle.enums.CommissionBusinessType;
import io.terminus.parana.settle.model.CommissionRule;
import io.terminus.parana.settle.service.CommissionRuleReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.sanlux.common.helper.UserTypeHelper.getOrderUserTypeByUser;

/**
 * vega 订单封装组件
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/17/16
 * Time: 10:26 PM
 */
@Slf4j
public class VegaRichOrderMaker implements RichOrderMaker {

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;

    @RpcConsumer
    private CommissionRuleReadService commissionRuleReadService;

    /**
     * 为订单预览页组装订单, 此时一般不包括发货区域, 以及优惠信息等
     * 一级经销商下单需拆单,根据不同的店铺id查询各个店铺下的商品 店铺id不变是谁就是谁
     * @param submittedSkus 主要包括skuId和购买数量信息
     * @param buyer         买家信息 shopId为接单shopId
     * @param channel       下单渠道
     * @return 组装好的商品及sku信息
     */
    @Override
    public RichOrder partial(List<SubmittedSku> submittedSkus, ParanaUser buyer, Integer channel) {
        List<RichSku> richSkus = Lists.newArrayListWithCapacity(submittedSkus.size());
        for (SubmittedSku submittedSku : submittedSkus) {
            if (submittedSku.getQuantity() <= 0) {
                continue;
            }
            Sku sku = findSkuById(submittedSku.getSkuId());

            Item item = findItemById(sku.getItemId());

            RichSku richSku = new RichSku();
            richSku.setItem(item);
            richSku.setSku(sku);
            richSku.setQuantity(submittedSku.getQuantity());
            richSku.setPromotionId(submittedSku.getPromotionId());
            richSkus.add(richSku);
        }

        //获取当前登录用户身份
        OrderUserType userType = getOrderUserTypeByUser(buyer);

        final Long receiveShopId =buyer.getShopId();

        ListMultimap<Long, RichSku> byShopId = Multimaps.index(richSkus, new Function<RichSku, Long>() {
            @Override
            public Long apply(RichSku richSku) {
                if(userType.equals(OrderUserType.DEALER_FIRST)){
                    return richSku.getItem().getShopId();
                }
                return receiveShopId;
            }
        });
        List<RichSkusByShop> richSkusByShops = Lists.newArrayListWithCapacity(byShopId.keySet().size());
        for (Long shopId : byShopId.keySet()) {

            Shop shop = findShopById(shopId);
            RichSkusByShop richSkusByShop = new RichSkusByShop();
            richSkusByShop.setShop(shop);
            richSkusByShop.setChannel(channel);
            richSkusByShop.setRichSkus(byShopId.get(shopId));
            richSkusByShops.add(richSkusByShop);

            Integer commissionRate = findCommissionRateByShopId(shopId);
            richSkusByShop.setCommissionRate(commissionRate);
        }

        RichOrder richOrder = new RichOrder();
        richOrder.setBuyer(buyer);
        richOrder.setRichSkusByShops(richSkusByShops);
        return richOrder;
    }


    /**
     * 提交创建订单请求, 此时应包括创建订单所需要的全部信息
     *
     * @param submittedOrder 包括skuId和购买数量信息, 以及收货地址, 发票, 各级别优惠等信息
     * @param buyer          买家信息
     * @return 组装好的订单信息
     */
    @Override
    public RichOrder full(SubmittedOrder submittedOrder, ParanaUser buyer) {

        RichOrder richOrder = new RichOrder();
        Long globalReceiverInfoId = submittedOrder.getReceiverInfoId();
        if (globalReceiverInfoId != null) {
            final ReceiverInfo result = findReceiverInfoById(globalReceiverInfoId);
            richOrder.setReceiverInfo(result);
        }

        richOrder.setBuyer(buyer);
        richOrder.setPayType(submittedOrder.getPayType());
        richOrder.setExtra(submittedOrder.getExtra());
        richOrder.setPromotionId(submittedOrder.getPromotionId());

        final List<SubmittedSkusByShop> submittedSkusByShops = submittedOrder.getSubmittedSkusByShops();
        if (CollectionUtils.isEmpty(submittedSkusByShops)) {
            log.error("no shop orders specified");
            throw new ServiceException("shop.orders.empty");
        }

        List<RichSkusByShop> richSkusByShops = Lists.newArrayListWithExpectedSize(submittedSkusByShops.size());
        for (SubmittedSkusByShop submittedSkusByShop : submittedSkusByShops) {
            RichSkusByShop richSkusByShop = new RichSkusByShop();
            Long shopId = submittedSkusByShop.getShopId();
            Shop shop = findShopById(shopId);
            richSkusByShop.setShop(shop);
            richSkusByShop.setBuyerNote(submittedSkusByShop.getBuyerNote());
            richSkusByShop.setInvoiceId(submittedSkusByShop.getInvoiceId());
            richSkusByShop.setShipmentType(submittedSkusByShop.getShipmentType());
            richSkusByShop.setPromotionId(submittedSkusByShop.getPromotionId());
            richSkusByShop.setShipmentPromotionId(submittedSkusByShop.getShipmentPromotionId());
            richSkusByShop.setChannel(submittedOrder.getChannel());
            richSkusByShop.setOrderStatus(getOrderInitStatus(shop));
            final Long receiverInfoId = submittedSkusByShop.getReceiverInfoId();
            if (receiverInfoId != null) {
                richSkusByShop.setReceiverInfo(findReceiverInfoById(receiverInfoId));
            }

            //Integer commissionRate = findCommissionRateByShopId(shopId);
            richSkusByShop.setCommissionRate(0);

            List<SubmittedSku> submittedSkus = submittedSkusByShop.getSubmittedSkus();
            if (CollectionUtils.isEmpty(submittedSkus)) {
                log.error("no sku specified for shop(id={})", shopId);
                throw new ServiceException("shop.sku.empty");
            }
            List<RichSku> richSkus = Lists.newArrayListWithExpectedSize(submittedSkus.size());
            for (SubmittedSku submittedSku : submittedSkus) {
                RichSku richSku = new RichSku();
                richSku.setPromotionId(submittedSku.getPromotionId());
                richSku.setShipmentType(submittedSku.getShipmentType());
                richSku.setQuantity(submittedSku.getQuantity());
                richSku.setInvoiceId(submittedSku.getInvoiceId());
                richSku.setChannel(submittedOrder.getChannel());
                richSku.setOrderStatus(getOrderInitStatus(shop));
                if (submittedSku.getReceiverInfoId() != null) {
                    submittedSku.setReceiverInfoId(submittedSku.getReceiverInfoId());
                }

                final Long skuId = submittedSku.getSkuId();
                Sku sku = findSkuById(skuId);
                //这里就不检测 sku中的shopId是否与接单的shopId是否一致(非一级下单有可能多家店铺下个平台)
                /*if (!Objects.equal(sku.getShopId(), shopId)) {
                    log.error("sku(id={}) not belong to shop(id={})", skuId, shopId);
                    throw new ServiceException("shop.sku.mismatch");
                }*/

                richSku.setSku(sku);

                Item item = findItemById(sku.getItemId());
                richSku.setItem(item);
                richSkus.add(richSku);
            }
            richSkusByShop.setRichSkus(richSkus);
            richSkusByShops.add(richSkusByShop);
        }
        richOrder.setRichSkusByShops(richSkusByShops);
        //todo: handle this, 将各种级别的运费计算出来, 并将结果设置到对应的richOrder字段中去

        //promotionPicker.pick(richOrder);
        return richOrder;
    }

    /**
     * 根据订单和子订单列表组装对应的richOrder
     *
     * @param shopOrders 订单列表
     * @param skuOrders  子订单列表
     * @param buyer      买家
     * @return 组装好的订单信息
     */
    @Override
    public RichOrder fromOrders(List<ShopOrder> shopOrders, List<SkuOrder> skuOrders, ParanaUser buyer) {
        if (CollectionUtils.isEmpty(shopOrders) || CollectionUtils.isEmpty(skuOrders)) {
            log.error("shopOrders and skuOrders can not empty");
            throw new ServiceException("order.empty");
        }
        ListMultimap<Long, SkuOrder> byShopOrderId = Multimaps.index(skuOrders, new Function<SkuOrder, Long>() {
            @Override
            public Long apply(SkuOrder skuOrder) {
                return skuOrder.getOrderId();
            }
        });
        List<RichSkusByShop> richSkusByShops = Lists.newArrayListWithCapacity(shopOrders.size());
        for (ShopOrder shopOrder : shopOrders) {
            final Long shopOrderId = shopOrder.getId();
            if (!Objects.equal(shopOrder.getBuyerId(), buyer.getId())) {
                log.error("shopOrder(id={})'s buyerId={}, but current buyer(id={})",
                        shopOrderId, shopOrder.getBuyerId(), buyer.getId());
                throw new ServiceException("order.buyer.mismatch");
            }
            RichSkusByShop richSkusByShop = new RichSkusByShop();
            Long shopId = shopOrder.getShopId();
            Shop shop = findShopById(shopId);
            richSkusByShop.setShop(shop);
            richSkusByShop.setPromotionId(shopOrder.getPromotionId());
            richSkusByShop.setBuyerNote(shopOrder.getBuyerNote());
            richSkusByShop.setShipmentType(shopOrder.getShipmentType());
            richSkusByShop.setReceiverInfo(findReceiverInfoByOrderId(shopOrderId));
            richSkusByShop.setChannel(shopOrder.getChannel());

            List<SkuOrder> groupedSkuOrders = byShopOrderId.get(shopOrderId);
            if (CollectionUtils.isEmpty(groupedSkuOrders)) {
                log.error("no sku order found for shopOrder(id={})", shopOrderId);
                throw new ServiceException("sku.order.empty");
            }
            List<RichSku> richSkus = Lists.newArrayListWithCapacity(groupedSkuOrders.size());
            for (SkuOrder skuOrder : groupedSkuOrders) {
                RichSku richSku = new RichSku();
                richSku.setPromotionId(skuOrder.getPromotionId());
                richSku.setShipmentType(skuOrder.getShipmentType());
                richSku.setQuantity(skuOrder.getQuantity());
                richSku.setChannel(skuOrder.getChannel());

                final Long skuId = skuOrder.getSkuId();
                Sku sku = findSkuById(skuId);
                if (!Objects.equal(sku.getShopId(), shopId)) {
                    log.error("sku(id={}) not belong to shop(id={})", skuId, shopId);
                    throw new ServiceException("shop.sku.mismatch");
                }
                richSku.setSku(sku);

                Item item = findItemById(sku.getItemId());
                richSku.setItem(item);
                richSkus.add(richSku);
            }
            richSkusByShop.setRichSkus(richSkus);
            richSkusByShops.add(richSkusByShop);

            Integer commissionRate = findCommissionRateByShopId(shopId);
            richSkusByShop.setCommissionRate(commissionRate);
        }
        RichOrder richOrder = new RichOrder();
        richOrder.setRichSkusByShops(richSkusByShops);
        richOrder.setBuyer(buyer);
        return richOrder;
    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new ServiceException(rSku.getError());
        }
        return rSku.getResult();
    }

    private Shop findShopById(Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            log.error("failed to find shop(id={}), error code:{}", shopId, rShop.getError());
            throw new ServiceException(rShop.getError());
        }
        return rShop.getResult();
    }

    private Item findItemById(Long itemId) {
        Response<Item> rItem = itemReadService.findById(itemId);
        if (!rItem.isSuccess()) {
            log.error("failed to find item(id={}), error code:{}", itemId, rItem.getError());
            throw new ServiceException(rItem.getError());
        }
        return rItem.getResult();
    }

    private ReceiverInfo findReceiverInfoById(Long receiverInfoId) {
        Response<ReceiverInfo> rReceiverInfo = receiverInfoReadService.findById(receiverInfoId);
        if (!rReceiverInfo.isSuccess()) {
            log.error("receiverInfo(id={}) find fail, error code:{}", receiverInfoId, rReceiverInfo.getError());
            throw new ServiceException(rReceiverInfo.getError());
        }
        return rReceiverInfo.getResult();
    }


    private ReceiverInfo findReceiverInfoByOrderId(Long shopOrderId) {
        Response<List<ReceiverInfo>> rReceiverInfo = receiverInfoReadService.findByOrderId(shopOrderId, OrderLevel.SHOP);
        if (!rReceiverInfo.isSuccess()) {
            log.error("receiverInfo(orderId={}) find fail, error code:{}", shopOrderId, rReceiverInfo.getError());
            throw new ServiceException(rReceiverInfo.getError());
        }
        List<ReceiverInfo> receiverInfos = rReceiverInfo.getResult();
        if (receiverInfos.isEmpty()) {
            log.warn("no receiverInfos found for shopOrder(id={})", shopOrderId);
            return null;
        }
        return receiverInfos.get(0);
    }

    private Integer findCommissionRateByShopId(Long shopId) {
        Response<CommissionRule> rComssionRule = commissionRuleReadService.findMatchCommissionRule(shopId, CommissionBusinessType.SHOP.value());
        if (!rComssionRule.isSuccess()) {
            log.error("findMatchCommissionRule for shop fail, shopId={}, cause={}", shopId, rComssionRule.getError());
            throw new ServiceException(rComssionRule.getError());
        }
        CommissionRule commissionRule = rComssionRule.getResult();
        if (commissionRule == null) {
            return 0;
        } else {
            return commissionRule.getRate();
        }
    }

    private Integer getOrderInitStatus(Shop shop){
        //{平台店铺(普通用户下单一级接不了单) 或 供应商店铺 (一级下单)====》会下给平台,所以初始状态为待平台审核}
        if(Objects.equal(shop.getType(), VegaShopType.PLATFORM.value())||Objects.equal(shop.getType(), VegaShopType.SUPPLIER.value())){
            return VegaOrderStatus.NOT_PAID_PLATFORM.getValue();
        }
        //下给一级
        if(Objects.equal(shop.getType(), VegaShopType.DEALER_FIRST.value())){
            return VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue();
        }
        //下给二级
        return VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue();
    }

}
