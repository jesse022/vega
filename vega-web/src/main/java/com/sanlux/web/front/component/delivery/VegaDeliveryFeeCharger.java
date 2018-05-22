package com.sanlux.web.front.component.delivery;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.service.ShopItemDeliveryFeeReadService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.delivery.dto.RichItemDeliveryFee;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.order.component.DefaultDeliveryFeeCharger;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 运费计算
 * <ul>
 * <li>
 * 接单的shop如果是平台、买家身份为一级经销商,则取商品自身的运费模板（真实商家);
 * 接单的shop如果是平台、买家身份为普通用户,则商品运费为0;
 * </li>
 * <li>
 * 接单的shop如果是一级、买家身份二级,则取一级下商品的运费模板（找不到的商品或模板不存在则运费为0);
 * </li>
 * </ul>
 * <p>
 * Author:cp
 * Created on 8/25/16.
 */
@Component
@Slf4j
public class VegaDeliveryFeeCharger extends DefaultDeliveryFeeCharger implements DeliveryFeeCharger {

    @RpcConsumer
    private ShopItemDeliveryFeeReadService shopItemDeliveryFeeReadService;



    @Autowired
    private ReceiveShopParser receiveShopParser;


    @Override
    protected RichItemDeliveryFee findRichItemDeliveryFee(Long itemId, Integer addressId) {
        ParanaUser user = UserUtil.getCurrentUser();
        if (user == null) {
            return null;
        }
        Long shopId = findShopIdByCurrentUser(itemId, addressId, user);

        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(user);

        if (orderUserType == OrderUserType.DEALER_FIRST) {
            Map<Long, RichItemDeliveryFee> richItemDeliveryFeeByItemIdIndex = findRichItemDeliveryFee(Arrays.asList(itemId));
            return richItemDeliveryFeeByItemIdIndex.get(itemId);
        }
        return findRickItemDeliveryFeeByItemIdsAndShopId(itemId, shopId);


    }


    @Override
    public Integer charge(Long skuId, Integer quantity, Integer addressId) {
        return super.charge(skuId, quantity, addressId);
    }

    @Override
    public Map<Long, Integer> charge(List<RichSkusByShop> richSkusByShops, ReceiverInfo receiverInfo) {
        final ParanaUser buyer = UserUtil.getCurrentUser();
        final ReceiverInfo globalReceiverInfo = receiverInfo;

        Map<Long, Integer> shopIdToDeliveryFee = new HashMap<>();
        for (RichSkusByShop richSkusByShop : richSkusByShops) {
            //一级下单 shop为真实店铺
            final Shop shop = richSkusByShop.getShop();
            VegaShopType shopType = VegaShopType.from(shop.getType());

            Integer deliveryFee;
            switch (shopType) {
                case PLATFORM:
                    deliveryFee = chargeForPlatform(buyer, richSkusByShop, globalReceiverInfo);
                    break;
                case DEALER_FIRST:
                    deliveryFee = chargeForDealer(richSkusByShop, globalReceiverInfo);
                    break;
                case DEALER_SECOND:
                    deliveryFee = chargeForDealer(richSkusByShop, globalReceiverInfo);
                    break;
                case SUPPLIER:
                    deliveryFee = chargeForPlatform(buyer, richSkusByShop, globalReceiverInfo);
                    break;
                default:
                    log.error("illegal order receiver,shopType:{}", shopType);
                    throw new JsonResponseException("illegal.order.receiver");
            }
            shopIdToDeliveryFee.put(shop.getId(), deliveryFee);
        }
        return shopIdToDeliveryFee;
    }

    private Integer chargeForPlatform(ParanaUser buyer, RichSkusByShop richSkusByShop, ReceiverInfo globalReceiverInfo) {
        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);
        switch (orderUserType) {
            case NORMAL_USER: // 普通用户下单也改用供应商的运费模板
            case SUPPLIER:
            case SERVICE_MANAGER: //买家是供应商|业务经理身份时,当做普通用户处理
            case DEALER_FIRST:
                List<Long> itemIds = extractItemIds(richSkusByShop);
                Map<Long, RichItemDeliveryFee> richItemDeliveryFeeByItemIdIndex = findRichItemDeliveryFee(itemIds);
                return chargeForShop(richSkusByShop, globalReceiverInfo, richItemDeliveryFeeByItemIdIndex);
            default:
                log.error("illegal user type({}) when platform receive order",
                        orderUserType);
                throw new JsonResponseException("illegal.user.type");
        }
    }

    private Integer chargeForDealer(RichSkusByShop richSkusByShop, ReceiverInfo globalReceiverInfo) {
        List<Long> itemIds = extractItemIds(richSkusByShop);
        final Long shopIdOfFirstDealer = richSkusByShop.getShop().getId();
        Map<Long, RichItemDeliveryFee> richItemDeliveryFeeByItemIdIndex = findRichItemDeliveryFeeForDealer(shopIdOfFirstDealer, itemIds);
        return chargeForShop(richSkusByShop, globalReceiverInfo, richItemDeliveryFeeByItemIdIndex);
    }

    private Map<Long, RichItemDeliveryFee> findRichItemDeliveryFeeForDealer(Long shopIdOfFirstDealer, List<Long> itemIds) {
        Response<List<RichItemDeliveryFee>> findResp = shopItemDeliveryFeeReadService.findDeliveryFeeDetailByShopIdAndItemIds(shopIdOfFirstDealer, itemIds);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop item delivery fee detail by first dealer shop id={},itemIds={},cause:{}",
                    shopIdOfFirstDealer, itemIds, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        List<RichItemDeliveryFee> richItemDeliveryFees = findResp.getResult();

        if (CollectionUtils.isEmpty(richItemDeliveryFees)) {
            return Collections.EMPTY_MAP;
        }

        return Maps.uniqueIndex(richItemDeliveryFees, new Function<RichItemDeliveryFee, Long>() {
            @Override
            public Long apply(RichItemDeliveryFee richItemDeliveryFee) {
                return richItemDeliveryFee.getItemDeliveryFee().getItemId();
            }
        });
    }

    private List<Long> extractItemIds(RichSkusByShop richSkusByShop) {
        Set<Long> itemIds = new HashSet<>();
        for (RichSku richSku : richSkusByShop.getRichSkus()) {
            itemIds.add(richSku.getItem().getId());
        }
        return Lists.newArrayList(itemIds);
    }

    private Long findShopIdByCurrentUser(Long itemId, Integer addressId, ParanaUser user) {
        Long shopId = DefaultId.PLATFROM_SHOP_ID;
        if (user != null) {
            shopId = findItemOwnerShopId(itemId, addressId, user);
        }
        return shopId;
    }

    private Long findItemOwnerShopId(Long itemId, Integer regionId, ParanaUser user) {
        Response<Long> itemOwnerShopIdResp =
                receiveShopParser.findShopIdForItemDetail(itemId, regionId, user);
        if (!itemOwnerShopIdResp.isSuccess()) {
            log.error("find shopId for item detail fail, itemId:{}, regionId:{}, user:{}, cause:{}",
                    itemId ,regionId, user, itemOwnerShopIdResp.getError());
            throw new JsonResponseException(itemOwnerShopIdResp.getError());
        }
        return itemOwnerShopIdResp.getResult();
    }

    private RichItemDeliveryFee findRickItemDeliveryFeeByItemIdsAndShopId (Long itemId, Long shopId) {
        Response<List<RichItemDeliveryFee>> response =
                shopItemDeliveryFeeReadService.findDeliveryFeeDetailByShopIdAndItemIds(shopId, Arrays.asList(itemId));
        if (!response.isSuccess()) {
            log.error("find rick item delivery fee by shopId:{}, and itemId:{}, cause:{}",
                    shopId, itemId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<RichItemDeliveryFee> deliveryFees = response.getResult();
        if (CollectionUtils.isEmpty(deliveryFees)) {
            return null;
        }
        return deliveryFees.get(0);
    }


}
