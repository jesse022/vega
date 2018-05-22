package com.sanlux.trade.component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.SystemConstant;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.component.DefaultPersistedOrderMaker;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/26/16
 * Time: 3:46 PM
 */
@Slf4j
public class VegaPersistedOrderMaker extends DefaultPersistedOrderMaker {


    public VegaPersistedOrderMaker(OrderLevel orderReceiverInfoLevel, OrderLevel orderInvoiceLevel) {
        super(orderReceiverInfoLevel, orderInvoiceLevel);
    }


    //重写 塞tags信息
    @Override
    protected List<ShopOrder> retrieveShopOrders(RichOrder richOrder) {
        final List<RichSkusByShop> richSkusByShops = richOrder.getRichSkusByShops();
        List<ShopOrder> shopOrders = Lists.newArrayListWithCapacity(richSkusByShops.size());



        for (RichSkusByShop richSkusByShop : richSkusByShops) {
            ShopOrder shopOrder = new ShopOrder();
            Shop shop = richSkusByShop.getShop();
            shopOrder.setBuyerName(richOrder.getBuyer().getName());
            shopOrder.setBuyerId(richOrder.getBuyer().getId());
            shopOrder.setShopId(shop.getId());
            shopOrder.setShopName(shop.getName());
            shopOrder.setOutShopId(shop.getOuterId());
            shopOrder.setStatus(richSkusByShop.getOrderStatus());
            shopOrder.setPromotionId(richSkusByShop.getPromotionId());
            shopOrder.setFee(richSkusByShop.getFee());
            shopOrder.setOriginFee(richSkusByShop.getOriginFee());
            shopOrder.setDiscount(richSkusByShop.getDiscount());
            shopOrder.setShipFee(richSkusByShop.getShipFee());
            shopOrder.setOriginShipFee(richSkusByShop.getOriginShipFee());
            shopOrder.setShipmentPromotionId(richSkusByShop.getShipmentPromotionId());
            shopOrder.setIntegral(richSkusByShop.getIntegral());
            shopOrder.setBalance(richSkusByShop.getBalance());
            shopOrder.setPayType(richOrder.getPayType());
            shopOrder.setBuyerNote(richSkusByShop.getBuyerNote());
            shopOrder.setChannel(richSkusByShop.getChannel());
            shopOrder.setCommissionRate(richSkusByShop.getCommissionRate());
            shopOrder.setTags(makeOrderTagsMap(richOrder,null));

            Map<String, String> extraMap = richOrder.getExtra();
            if(!CollectionUtils.isEmpty(extraMap)){
                if (Arguments.notNull(extraMap.get(SystemConstant.YOUYUNCAI_ORDER_ID))) {
                    // 友云采订单ID
                    shopOrder.setOutId(extraMap.get(SystemConstant.YOUYUNCAI_ORDER_ID));
                }
                if (Arguments.notNull(extraMap.get(SystemConstant.YOUYUNCAI_ORDER_FROM))) {
                    // 友云采订单来源名称
                    shopOrder.setOutFrom(extraMap.get(SystemConstant.YOUYUNCAI_ORDER_FROM));
                }
            }


            shopOrders.add(shopOrder);
        }
        return shopOrders;
    }

    //重写 塞tags信息
    @Override
    protected ListMultimap<Long, SkuOrder> retrieveSkuOrders(RichOrder richOrder) {
        final ListMultimap<Long, SkuOrder> byShopId = ArrayListMultimap.create();
        for (RichSkusByShop richSkusByShop : richOrder.getRichSkusByShops()) {
            Long shopId = richSkusByShop.getShop().getId();
            for (RichSku richSku : richSkusByShop.getRichSkus()) {
                SkuOrder skuOrder = new SkuOrder();
                skuOrder.setShopId(shopId);
                final Sku sku = richSku.getSku();
                final Item item = richSku.getItem();
                skuOrder.setSkuId(sku.getId());
                skuOrder.setStatus(richSku.getOrderStatus());
                skuOrder.setQuantity(richSku.getQuantity());
                skuOrder.setBuyerId(richOrder.getBuyer().getId());
                skuOrder.setBuyerName(richOrder.getBuyer().getName());
                skuOrder.setItemId(item.getId());
                skuOrder.setItemName(item.getName());
                skuOrder.setShopName(MoreObjects.firstNonNull(richSkusByShop.getShop().getName(), item.getShopName()));
                final String image = !StringUtils.isEmpty(sku.getImage()) ? sku.getImage() : item.getMainImage();
                skuOrder.setSkuImage(image);
                skuOrder.setSkuAttrs(sku.getAttrs());
                skuOrder.setPromotionId(richSku.getPromotionId());
                skuOrder.setFee(richSku.getFee());
                skuOrder.setOriginFee(richSku.getOriginFee());
                skuOrder.setDiscount(richSku.getDiscount());
                skuOrder.setBalance(richSku.getBalance());
                skuOrder.setIntegral(richSku.getBalance());
                skuOrder.setShipFee(richSku.getShipFee());
                skuOrder.setShipFeeDiscount(richSku.getShipFeeDiscount());
                skuOrder.setShipmentType(richSku.getShipmentType());
                skuOrder.setItemSnapshotId(richSku.getItemSnapshotId());
                skuOrder.setChannel(richSku.getChannel());
                skuOrder.setInvoiced(false);
                skuOrder.setHasRefund(false);
                skuOrder.setPayType(richOrder.getPayType());
                skuOrder.setTags(makeOrderTagsMap(richOrder,sku));
                byShopId.put(shopId, skuOrder);
            }
        }
        return byShopId;
    }



    //tags 存放下单人身份、平台店铺名称、下单时商品最终价格
    private Map<String,String> makeOrderTagsMap(RichOrder richOrder,Sku sku){
        //获取当前登录用户身份
        String roleName = getUserRoleName(richOrder.getBuyer());
        Map<String,String> map = Maps.newHashMap();
        map.put(SystemConstant.ROLE_NAME,roleName);
        if(Arguments.notNull(sku)){
            map.put(SystemConstant.ORDER_SKU_PRICE,String.valueOf(sku.getPrice()));
            Map<String, Integer> extraPriceMap = sku.getExtraPrice();
            if(CollectionUtils.isEmpty(extraPriceMap)){
                extraPriceMap = Maps.newHashMap();
            }
            map.put(SystemConstant.ORDER_SKU_SELLER_PRICE,String.valueOf(extraPriceMap.get(SystemConstant.ORDER_SKU_SELLER_PRICE)));
            if (!Arguments.isNull(extraPriceMap.get(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE))) {
                map.put(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE, String.valueOf(extraPriceMap.get(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE)));
            }
            if (!Arguments.isNull(extraPriceMap.get(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE))) {
                map.put(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE, String.valueOf(extraPriceMap.get(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE)));
            }
        }

        //平台店铺名称
        Map<String, String> extraMap = richOrder.getExtra();
        if(!CollectionUtils.isEmpty(extraMap)){
            map.put(SystemConstant.PLATFORM_FORM_SHOP_NAME,extraMap.get(SystemConstant.PLATFORM_FORM_SHOP_NAME));

        }
        return map;
    }


}
