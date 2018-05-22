package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.service.VegaOrderWriteService;
import com.sanlux.web.front.core.events.VegaFirstShopDispatchEvent;
import com.sanlux.web.front.core.util.ArithUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 一级经销商派单二级时,修改订单二级成本价事件
 *
 * Created by lujm on 2017/12/6.
 */
@Component
@Slf4j
public class VegaFirstShopDispatchListener {

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private VegaCategoryAuthByShopIdCacherService vegaCategoryAuthByShopIdCacherService;
    @RpcConsumer
    private VegaCategoryByItemIdCacherService vegaCategoryByItemIdCacherService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;


    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onConfirm(VegaFirstShopDispatchEvent vegaFirstShopDispatchEvent) throws Exception {
        Long shopId =  vegaFirstShopDispatchEvent.getShopId();
        Long orderId = vegaFirstShopDispatchEvent.getOrderId();

        List<SkuOrder> skuOrders = findSkuOrdersByShopOrderId(orderId);
        VegaShop vegaShop = findVegaShopByShopId(shopId);
        if (Arguments.isNull(vegaShop) ||
                !Objects.equals(vegaShop.getShop().getType(), VegaShopType.DEALER_SECOND.value())) {
            // 接单店铺不是二级经销商时
            return;
        }
        shopId = vegaShop.getShopExtra().getShopPid();

        for (SkuOrder skuOrder : skuOrders) {
            Integer secondSellerPrice = findSecondShopSkuCostPrice(skuOrder, shopId); // 二级经销商成本价
            Map<String, String> tagsMap = skuOrder.getTags();
            if(CollectionUtils.isEmpty(tagsMap)){
                tagsMap = Maps.newHashMap();
            }
            if (!Arguments.isNull(secondSellerPrice)) {
                tagsMap.put(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE, String.valueOf(secondSellerPrice));
                Response<Boolean> rsp = vegaOrderWriteService.updateSkuOrderTagsJsonById(skuOrder.getId(), tagsMap);
                if (!rsp.isSuccess()) {
                    log.error("fail to update sku order tags by skuOrderId={}, tags={}, cause:{}",
                            skuOrder.getId(), tagsMap, rsp.getError());
                }
            }
        }
    }

    private List<SkuOrder> findSkuOrdersByShopOrderId(Long shopOrderId) {
        Response<List<SkuOrder>> findResp = skuOrderReadService.findByShopOrderId(shopOrderId);
        if (!findResp.isSuccess()) {
            log.error("fail to find sku orders by shopOrderId={},cause:{}",
                    shopOrderId, findResp.getError());
            throw new ServiceException(findResp.getError());
        }
        return findResp.getResult();
    }

    /**
     * 获取二级经销商SKU成本价
     * @param skuOrder skuOrder
     * @param shopPid 上级店铺Id
     * @return sku price
     */
    public Integer findSecondShopSkuCostPrice(SkuOrder skuOrder, Long shopPid) {
        try {
            ShopSku shopSku = findShopSkuByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, skuOrder.getSkuId());
            if (Arguments.isNull(shopSku)) {
                return null;
            }

            Integer shopSkuPrice = shopSku.getPrice();
            Float discount = findSecondShopItemDiscount(shopPid, skuOrder.getItemId());

            Integer price =
                    (int) Math.round(
                            ArithUtil.div(ArithUtil.mul(shopSkuPrice.doubleValue(), discount.doubleValue()),
                                    DefaultDiscount.COUNT_PRICE_DIVISOR.doubleValue())
                    );
            return price > shopSkuPrice ? shopSkuPrice : price;
        } catch (Exception e) {
            log.error("find sku second shop cost price fail,skuId:{}, sellerShopId:{}, cause:{}",
                    skuOrder.getSkuId(), shopPid, e.getMessage());
            return  null;
        }

    }

    private ShopSku findShopSkuByShopIdAndSkuId(Long shopId, Long skuId) {
        Response<Optional<ShopSku>> shopSkuResponse = shopSkuReadService.findByShopIdAndSkuId(shopId, skuId);
        if (!shopSkuResponse.isSuccess()) {
            log.error("find shop sku fail, shopId:{}, skuId:{}, cause:{}", shopId, skuId, shopSkuResponse.getError());
            throw new ServiceException(shopSkuResponse.getError());
        }
        if (shopSkuResponse.getResult().isPresent()) {
            return shopSkuResponse.getResult().get();
        }
        return null;
    }

    /**
     * 通过店铺ID查询店铺信息
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private VegaShop findVegaShopByShopId(Long shopId) {
        Response<VegaShop> shopResp = vegaShopReadService.findByShopId(shopId);
        if (!shopResp.isSuccess()) {
            log.error("failed to find shop extra by shopId = ({}), cause : {}", shopId, shopResp.getError());
            return null;
        }
        return shopResp.getResult();
    }

    /**
     * 根据上级店铺ID和商品ID,获取二级经销商当前折扣
     * @param shopPid 上级店铺Id
     * @param itemId itemId
     * @return discount
     */
    public Float findSecondShopItemDiscount(Long shopPid, Long itemId) {
        try {
            Long rankId = DefaultId.SECOND_SHOP_RANK_ID;

            //取类目折扣
            Float discount = findCategoryDiscount(shopPid, itemId, rankId);
            if (!discount.equals(DefaultDiscount.NOT_FIND_DISCOUNT.floatValue())) {
                return discount;
            }
            //取会员折扣
            return findMemberLevelDiscount(shopPid, rankId).floatValue();
        } catch (Exception e) {
            log.error("find second shop item discount fail, shopPid:{}, itemId:{}, cause:{}",
                    shopPid, itemId, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 获取类目折扣
     *
     * @param shopPid 接单店铺ID
     * @param itemId  商品ID
     * @param rankId  用户等级ID
     * @return 折扣0为无折扣
     */
    public Float findCategoryDiscount(Long shopPid, Long itemId, Long rankId) {
        try {
            Float discount = DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            if ( shopPid.equals(DefaultId.PLATFROM_SHOP_ID) ) {
                return discount;
            }
            Long categoryId = findCategoryIdsByItemId(itemId);

            //取类目上的折扣
            List<VegaCategoryDiscountDto> discountDtoList = findCategoryDiscountList(shopPid);
            if (CollectionUtils.isEmpty(discountDtoList)) {
                return discount;
            }
            Map<Long, VegaCategoryDiscountDto> firstDiscountCategory =
                    Maps.uniqueIndex(discountDtoList, VegaCategoryDiscountDto::getCategoryId);
            VegaCategoryDiscountDto firstDiscount = firstDiscountCategory.get(categoryId);

            if (Objects.isNull(firstDiscount) || !firstDiscount.getIsUse()) {
                return discount;
            }
            Map<Long, MemberDiscountDto> firstMemberDiscount =
                    Maps.uniqueIndex(firstDiscount.getCategoryMemberDiscount(), MemberDiscountDto::getMemberLevelId);
            return firstMemberDiscount.get(rankId).getDiscount().floatValue();
        }catch (Exception e) {
            log.error("find category discount fail, shopId:{}, itemId:{}, rankId:{}, cause:{}",
                    shopPid, itemId, rankId, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 根据会员等级获取折扣
     * @param shopId 接单店铺
     * @param rankId 会员等级
     * @return 折扣信息
     */
    private Integer findMemberLevelDiscount(Long shopId, Long rankId) {
        Response<VegaShopExtra> response = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find item member level discount fail,shopId:{},cause:{}",
                    shopId, response.getError());
            throw new JsonResponseException(response.getError());
        }

        Map<String, Integer> memberDiscount = response.getResult().getMemberDiscount();
        if (CollectionUtils.isEmpty(memberDiscount) || Arguments.isNull(memberDiscount.get(rankId.toString()))) {
            return DefaultDiscount.NOT_FIND_DISCOUNT;
        }
        return memberDiscount.get(rankId.toString());
    }

    /**
     * 获取店铺类目折扣
     * @param shopId 店铺Id
     * @return 折扣信息
     */
    private List<VegaCategoryDiscountDto> findCategoryDiscountList(Long shopId) {

        Response<Optional<CategoryAuthe>> categoryAuthResp = vegaCategoryAuthByShopIdCacherService.findByShopId(shopId);
        if (!categoryAuthResp.isSuccess()) {
            log.error("find category discount list fail, shopId:{}, cause:{}",
                    shopId, categoryAuthResp.getError());
            throw new JsonResponseException(categoryAuthResp.getError());
        }
        Optional<CategoryAuthe> categoryAutheOptional = categoryAuthResp.getResult();
        if (!categoryAutheOptional.isPresent() || Arguments.isNull(categoryAutheOptional.get())
                && !CollectionUtils.isEmpty(categoryAutheOptional.get().getDiscountList())) {
            return Collections.<VegaCategoryDiscountDto>emptyList();
        }
        return categoryAutheOptional.get().getDiscountList();
    }

    /**
     * 根据商品ID获取类目ID
     * @param itemId 商品Id
     * @return 类目Id
     */
    private Long findCategoryIdsByItemId(Long itemId) {
        Response<Long> categoryIdDtoResponse =
                vegaCategoryByItemIdCacherService.findByItemId(itemId);
        if (!categoryIdDtoResponse.isSuccess()) {
            log.error("find categoryIds by itemId fail, itemId:{}, cause:{}",
                    itemId, categoryIdDtoResponse.getError());
            throw new JsonResponseException(categoryIdDtoResponse.getError());
        }
        return categoryIdDtoResponse.getResult();
    }



}
