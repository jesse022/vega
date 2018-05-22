package com.sanlux.web.front.component.item;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.service.ShopUserReadService;
import com.sanlux.user.service.UserRankReadService;
import com.sanlux.user.service.VegaRegionIdsByShopIdCacherService;
import com.sanlux.web.front.core.util.ArithUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 接单店铺 解析
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/18/16
 * Time: 1:52 PM
 */
@Slf4j
@Service
public class ReceiveShopParser {

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private VegaRegionIdsByShopIdCacherService vegaRegionIdsByShopIdCacherService;

    @RpcConsumer
    private VegaCategoryAuthByShopIdCacherService vegaCategoryAuthByShopIdCacherService;

    @RpcConsumer
    private ShopUserReadService shopUserReadService;

    @RpcConsumer
    private VegaCategoryByItemIdCacherService vegaCategoryByItemIdCacherService;

    @RpcConsumer
    private UserRankReadService userRankReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private UserReadService userReadService;


    //获取二级经销商所属一级经销商shopId
    public Response<Long> getShopPidForDealerSecond(Long userId) {

        Response<Shop> shopRes = shopReadService.findByUserId(userId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by user id:{} fail,error:{}", userId, shopRes.getError());
            return Response.fail(shopRes.getError());
        }
        Shop shop = shopRes.getResult();

        Response<VegaShopExtra> shopExtraRes = vegaShopReadService.findVegaShopExtraByShopId(shop.getId());
        if (!shopExtraRes.isSuccess()) {
            log.error("find shop extra by shop id:{} fail,error:{}", shop.getId(), shopExtraRes.getError());
            return Response.fail(shopExtraRes.getError());
        }
        Long shopPid = shopExtraRes.getResult().getShopPid();
        if (checkShopStatusByShopId(shopPid)) {
            return Response.ok(shopPid);

        }

        return Response.fail("first.shop.status.not.normal");
    }


    /**
     * 获取普通用户下单 接单shopId
     * modify by lujm on 2017/03/31
     * 逻辑描述
     *    1.专属会员,获取对应经销商接单
     *    2.根据收货地址匹配当地二级经销商,匹配到就二级接单
     *    3.根据收货地址匹配当地一级经销商,匹配到就一级接单
     *    4.匹配不到二级/一级的或购买商品二级/一级不经营等其他情况,平台接单
     * @param receiverInfo 收货地址信息
     * @param purchaseSkuOrders 采购单信息
     * @param user 用户信息
     * @return 接单shopId
     */
    public Response<Long> getShopIdForBuyer(ReceiverInfo receiverInfo,
                                            List<PurchaseSkuOrder> purchaseSkuOrders,
                                            ParanaUser user) {
        try {

            List<Long> shopIds = Lists.newArrayList();
            ShopUser shopUser = findShopUser(user.getId());

            if (Arguments.isNull(shopUser)) {
                //根据收货地址及供货区域,获取shopIds
                shopIds = getFirstShopIdsByRegionId(receiverInfo.getRegionId());
            } else {
                //专属会员shopId
                //shopIds.add(shopUser.getShopId());
                return Response.ok(shopUser.getShopId());
            }
            //如果没有匹配到一家则平台接单
            if (CollectionUtils.isEmpty(shopIds)) {
                return Response.ok(DefaultId.PLATFROM_SHOP_ID);
            }

            //进行匹配,增加二级经销商判断逻辑
            Optional<Long> receiveShopId = getShopIdBySku(shopIds, purchaseSkuOrders);
            //匹配到一家一级经销商
            if (receiveShopId.isPresent()) {
                return Response.ok(receiveShopId.get());
            } else {
                //如果没有匹配到一家一级经销商则平台接单
                return Response.ok(DefaultId.PLATFROM_SHOP_ID);
            }
        } catch (ServiceException e) {
            log.error("get shop id for buyer receiver info id:{} ,error:{}", receiverInfo.getId(), e.getMessage());
            return Response.fail(e.getMessage());
        }

    }


    /**
     * 采购单获取普通用户接单店铺ID
     * add by lujm on 2017/2/16
     *
     * 1.采购单不考虑收货地址情况
     * 2.专属会员对应经销商接单
     * 3.非专属会员平台接单
     * @param user 用户信息
     * @return 店铺ID
     */
    public Response<Long> getShopIdByrBuyer(ParanaUser user) {
        try {
            Long shopId =DefaultId.PLATFROM_SHOP_ID;//默认平台接单
            ShopUser shopUser = findShopUser(user.getId());
            if(!Arguments.isNull(shopUser)){
                shopId=shopUser.getShopId();
            }
            return Response.ok(shopId);
        } catch (ServiceException e) {
            log.error("get shop id for buyer info fail id:{} ,error:{}", user.getId(), e.getMessage());
            return Response.fail(e.getMessage());
        }

    }


    private Optional<Long> getShopIdBySku(List<Long> shopIds, List<PurchaseSkuOrder> purchaseSkuOrders) {
        List<Long> itemIds = Lists.transform(purchaseSkuOrders, new Function<PurchaseSkuOrder, Long>() {
            @Nullable
            @Override
            public Long apply(PurchaseSkuOrder input) {
                Sku sku = findSkuById(input.getSkuId());
                return sku.getItemId();
            }
        });

        Response<Optional<Long>> shopIdRes =
                vegaCategoryAuthByShopIdCacherService.findShopIdForReceiveOrder(shopIds, itemIds);
        if (!shopIdRes.isSuccess()) {
            log.error("find receive order shop by shop ids:{} items ids:{} fail,error:{}",
                    shopIds, itemIds, shopIdRes.getError());
            throw new ServiceException(shopIdRes.getError());
        }

        return shopIdRes.getResult();
    }

    //查询根据收货地址匹配到的一级经销商
    private List<Long> getFirstShopIdsByRegionId(Integer regionId) {

        Response<Optional<List<Long>>> shopIdsRes = vegaRegionIdsByShopIdCacherService.findShopIdsByRegionId(regionId);
        if (!shopIdsRes.isSuccess()) {
            log.error("find first shop ids by region id:{} fail,error:{}", regionId, shopIdsRes.getError());
            throw new ServiceException(shopIdsRes.getError());
        }
        if (!shopIdsRes.getResult().isPresent()) {
            return Collections.<Long>emptyList();
        }

        return shopIdsRes.getResult().get();
    }


    /**
     * 获取当前商品应该接单的店铺ID
     *
     * @param itemId       商品ID
     * @param regionId     地址区ID
     * @param user 当前登录用户
     * @return 商品应接单店铺ID
     */
    public Response<Long> findShopIdForItemDetail(Long itemId, Integer regionId, ParanaUser user) {

        try {
            if (isShopFirst(UserTypeHelper.getOrderUserTypeByUser(user))) {
                //取消供应商身份判断,供应商为买家时作为普通用户处理
                return Response.ok(DefaultId.PLATFROM_SHOP_ID);
            } else if (isShopSecond(UserTypeHelper.getOrderUserTypeByUser(user))) {
                if (user.getShopId() == null) {
                    log.error("shop second shopId is null");
                    return Response.fail("second.shopId.is.null");
                }
                Long shopPid = findPidOfShopId(user.getShopId());
                if (checkShopStatusByShopId(shopPid)) {
                    return Response.ok(shopPid);
                }
                return Response.ok(DefaultId.PLATFROM_SHOP_ID);
            } else {
                List<Long> shopIds = Lists.newArrayList();
                ShopUser shopUser = findShopUser(user.getId());
                if (Arguments.isNull(shopUser)) {
                    //普通用户根据地址找到唯一一家一级经销商或者平台
                    shopIds = getFirstShopIdsByRegionId(regionId);
                } else {
                    shopIds.add(shopUser.getShopId());
                }

                //如果没有匹配到经销商则平台接单
                if (CollectionUtils.isEmpty(shopIds)) {
                    return Response.ok(DefaultId.PLATFROM_SHOP_ID);
                }
                return Response.ok(findShopIdForItem(shopIds, itemId));
            }
        } catch (ServiceException e) {
            log.error("get shop id for item detail fail, itemId:{}, regionId:{}, user:{}, cause:{}",
                    itemId, regionId, user, e.getMessage());
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 根据用户Id获取接单店铺
     * 专属会员:对应服务商接单
     * 其他:平台接单
     * @param userId 用户Id
     * @return 接单店铺
     */
    public Response<Long> findReceiveShopIdByUserId(Long userId) {
        try {
            ShopUser shopUser = findShopUser(userId);
            if (Arguments.notNull(shopUser)) {
                return Response.ok(shopUser.getShopId());
            }

            return Response.ok(DefaultId.PLATFROM_SHOP_ID);
        } catch (ServiceException e) {
            log.error("get shop id for user fail, userId:{}, cause:{}", userId, e.getMessage());
            return Response.fail(e.getMessage());
        }
    }

    private Boolean checkShopStatusByShopId (Long shopId) {

        Response<Boolean> response = vegaShopReadService.checkShopStatusByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("fail to check shop status, shopId:{}, cause:{}", shopId, response.getError());
            throw new ServiceException(response.getError());
        }
        return response.getResult();
    }

    private Long findPidOfShopId(Long shopId) {
        Response<VegaShopExtra> shopExtraResponse = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!shopExtraResponse.isSuccess()) {
            log.error("fail to find item detail shopId:{}, cause:{}",
                    shopId, shopExtraResponse.getError());
            throw new ServiceException(shopExtraResponse.getError());
        }
        return shopExtraResponse.getResult().getShopPid();
    }

    private Long findShopIdForItem(List<Long> shopIds, Long itemId) {
        Response<Optional<Long>> shopIdResp =
                vegaCategoryAuthByShopIdCacherService.findShopIdForItem(shopIds, itemId);
        if (!shopIdResp.isSuccess()) {
            log.error("fail to find first shopId with category auth, itemId:{}, shopIds:{},cause:{}",
                    itemId, shopIds, shopIdResp.getError());
            throw new ServiceException(shopIdResp.getError());
        }
        if (!shopIdResp.getResult().isPresent()) {
            return DefaultId.PLATFROM_SHOP_ID;
        }
        return shopIdResp.getResult().get();
    }

    private Boolean isSupplier(OrderUserType orderUserType) {
        return orderUserType.equals(OrderUserType.SUPPLIER);
    }

    private Boolean isShopFirst(OrderUserType orderUserType) {
        return orderUserType.equals(OrderUserType.DEALER_FIRST);
    }

    private Boolean isShopSecond(OrderUserType orderUserType) {
        return orderUserType.equals(OrderUserType.DEALER_SECOND);
    }

    private Boolean isNormalUser(OrderUserType orderUserType) {
        return orderUserType.equals(OrderUserType.NORMAL_USER);
    }


    /**
     * 获取SKU真实价格
     * @param skuId SKUID
     * @param sellerShopId sellerShopId
     * @param userId userId
     * @param orderUserType orderUserType
     * @return sku price
     */
    public Response<Integer> findSkuPrice(Long skuId, Long sellerShopId, Long userId,
                                          OrderUserType orderUserType) {
        try {
            Sku sku = findSkuById(skuId);

            ShopSku shopSku = findShopSkuByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, skuId);
            if (Arguments.isNull(shopSku)) {
                return Response.fail("platform.shop.sku.not.set");
            }

            Integer shopSkuPrice = shopSku.getPrice();
            Float discount = DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            if (!userId.equals(DefaultId.NOT_LOG_IN_USER_ID)) {
                if (Objects.equals(orderUserType.name(), OrderUserType.SUPPLIER.name()) ||
                        Objects.equals(orderUserType.name(), OrderUserType.SERVICE_MANAGER.name()) ) {
                    //供应商|业务经理作为买家家时,享受普通会员折扣
                    orderUserType = OrderUserType.NORMAL_USER;
                }
                discount = findItemDiscount(sellerShopId, sku.getItemId(), userId, orderUserType);
            }
            if (Objects.equals(discount, DefaultDiscount.NOT_FIND_DISCOUNT.floatValue())) {
                return Response.ok(shopSkuPrice);
            }

            Integer price =
                    (int) Math.round(
                            ArithUtil.div(ArithUtil.mul(shopSkuPrice.doubleValue(), discount.doubleValue()),
                            DefaultDiscount.COUNT_PRICE_DIVISOR.doubleValue())
                    );
            return Response.ok(price > shopSkuPrice ? shopSkuPrice : price);
        } catch (Exception e) {
            log.error("find sku price fail,skuId:{}, sellerShopId:{}, userId:{}, orderUserType:{}, cause:{}",
                    skuId, sellerShopId, userId, orderUserType, e.getMessage());
            return Response.fail(e.getMessage());
        }

    }

    /**
     * 获取经销商SKU成本价
     * @param sku sku
     * @param sellerShopId 卖家店铺Id
     * @param orderUserType orderUserType
     * @return sku price
     */
    public Integer findSkuCostPrice(Sku sku, Long sellerShopId, OrderUserType orderUserType) {
        try {
            ShopSku shopSku = findShopSkuByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, sku.getId());
            if (Arguments.isNull(shopSku)) {
                return null;
            }
            if (Objects.equals(orderUserType, OrderUserType.DEALER_SECOND)) {
                // 接单店铺为二级经销商时
                VegaShop vegaShop = findVegaShopByShopId(sellerShopId);
                if (!Arguments.isNull(vegaShop) && Objects.equals(vegaShop.getShop().getType(), VegaShopType.DEALER_SECOND.value())) {
                    sellerShopId = vegaShop.getShopExtra().getShopPid();
                } else {
                    return 0;
                }
            }

            Integer shopSkuPrice = shopSku.getPrice();
            Float discount = findItemDiscount(sellerShopId, sku.getItemId(), null, orderUserType);

            Integer price =
                    (int) Math.round(
                            ArithUtil.div(ArithUtil.mul(shopSkuPrice.doubleValue(), discount.doubleValue()),
                                    DefaultDiscount.COUNT_PRICE_DIVISOR.doubleValue())
                    );
            return price > shopSkuPrice ? shopSkuPrice : price;
        } catch (Exception e) {
            log.error("find sku seller cost price fail,skuId:{}, sellerShopId:{}, orderUserType:{}, cause:{}",
                    sku.getId(), sellerShopId, orderUserType, e.getMessage());
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
     * 根据店铺ID和商品ID,和当前登录用户获取当前折扣
     * @param sellerShopId sellerShopId
     * @param itemId itemId
     * @param userId userId
     * @param orderUserType orderUserType
     * @return discount
     */
    public Float findItemDiscount(Long sellerShopId, Long itemId, Long userId,
                                    OrderUserType orderUserType) {
        try {
            Long rankId = DefaultId.SECOND_SHOP_RANK_ID;

            if (isSupplier(orderUserType)) {
                return DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            }else if(isShopFirst(orderUserType)) {
                return findFirstShopDiscount(itemId);
            } else if (isNormalUser(orderUserType)) {
                rankId = findRankIdByUserId(userId);
            }

            if (Objects.equals(rankId, DefaultId.PALT_FROM_RANK_ID) || sellerShopId.equals(DefaultId.PLATFROM_SHOP_ID)) {
                return DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            }

            //取类目折扣
            Float discount = findCategoryDiscount(sellerShopId, itemId, rankId);
            if (!discount.equals(DefaultDiscount.NOT_FIND_DISCOUNT.floatValue())) {
                return discount;
            }
            //取会员折扣
            return findMemberLevelDiscount(sellerShopId, rankId).floatValue();
        } catch (Exception e) {
            log.error("find item discount fail, sellerShopId:{}, itemId:{}, userId:{}, cause:{}",
                    sellerShopId, itemId, userId, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    private Float findFirstShopDiscount(Long itemId) {
        try {
            List<VegaCategoryDiscountDto> discountDtoList = findCategoryDiscountList(DefaultId.PLATFROM_SHOP_ID);
            Long categoryId = findCategoryIdsByItemId(itemId);
            Map<Long, VegaCategoryDiscountDto> discountsMap =
                    Maps.uniqueIndex(discountDtoList, VegaCategoryDiscountDto::getCategoryId);
            Float discount = discountsMap.get(categoryId).getCategoryDiscount();
            return discount == null ? DefaultDiscount.NOT_FIND_DISCOUNT.floatValue() : discount;
        } catch (Exception e) {
            log.error("find category discount for first shop fail, itemId:{}, cause:{}", itemId, e.getMessage());
            throw new ServiceException("find.category.discount.for.first.shop.fail");
        }
    }

    private Long findRankIdByUserId(Long userId) {

        Response userResponse = userReadService.findById(userId);
        if (!userResponse.isSuccess()) {
            log.error("find User by id:{} fail,cause:{}", userId, userResponse.getError());
            throw new ServiceException(userResponse.getError());
        }
        User user = (User)userResponse.getResult();
        if (user.getRoles().contains(UserRole.ADMIN.name())) {
            return DefaultId.PALT_FROM_RANK_ID;
        }

        Response<UserRank> userRankResponse = userRankReadService.findUserRankByUserId(userId);
        if (!userRankResponse.isSuccess()) {
            log.error("find rankId by userId fail, userId:{},cause:{}",
                    userId, userRankResponse.getError());
        }
        return userRankResponse.getResult().getRankId();
    }


    private Integer findMemberLevelDiscount(Long shopId, Long rankId) {
        Response<VegaShopExtra> response = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find item member level discount fail,shopId:{},cause:{}",
                    shopId, response.getError());
            throw new JsonResponseException(response.getError());
        }

        Map<String, Integer> memberDiscount = response.getResult().getMemberDiscount();
        if (!CollectionUtils.isEmpty(memberDiscount) && Arguments.notNull(memberDiscount.get(rankId.toString()))) {
            return memberDiscount.get(rankId.toString());
        }
        return DefaultDiscount.NOT_FIND_DISCOUNT;
    }


    private ShopUser findShopUser(Long userId) {
        Response<Optional<ShopUser>> shopUserResponse =
                shopUserReadService.findShopUserByUserId(userId);
        if (!shopUserResponse.isSuccess()) {
            log.error("find item special discount fail,userId:{},cause:{}",
                    userId, shopUserResponse.getError());
            throw new ServiceException(shopUserResponse.getError());
        }
        Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
        if (shopUserOptional.isPresent()) {
            return shopUserResponse.getResult().get();
        }
        return null;
    }


    /**
     * 获取类目折扣
     *
     * @param shopId 店铺ID
     * @param itemId 商品ID
     * @param rankId 用户等级ID
     * @return 折扣0为无折扣
     */
    public Float findCategoryDiscount(Long shopId, Long itemId, Long rankId) {
        try {
            Float discount = DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            if (shopId.equals(DefaultId.PLATFROM_SHOP_ID)) {
                return discount;
            }
            Long categoryId = findCategoryIdsByItemId(itemId);

            //取类目上的折扣
            List<VegaCategoryDiscountDto> discountDtoList = findCategoryDiscountList(shopId);
            if (CollectionUtils.isEmpty(discountDtoList)) {
                return discount;
            }
            Map<Long, VegaCategoryDiscountDto> firstDiscountCategory =
                    Maps.uniqueIndex(discountDtoList, VegaCategoryDiscountDto::getCategoryId);
            VegaCategoryDiscountDto firstDiscount = firstDiscountCategory.get(categoryId);

            if (Objects.isNull(firstDiscount) || !firstDiscount.getIsUse()) {
                //log.error("find category not auth,shopId:{}, categoryId:{}", shopId, categoryId);
                //throw new ServiceException("category.not.auth");
                return DefaultDiscount.NOT_FIND_DISCOUNT.floatValue();
            }
            Map<Long, MemberDiscountDto> firstMemberDiscount =
                    Maps.uniqueIndex(firstDiscount.getCategoryMemberDiscount(), MemberDiscountDto::getMemberLevelId);
            return firstMemberDiscount.get(rankId).getDiscount().floatValue();
        }catch (Exception e) {
            log.error("find category discount fail, shopId:{}, itemId:{}, rankId:{}, cause:{}",
                    shopId, itemId, rankId, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    private List<VegaCategoryDiscountDto> findCategoryDiscountList(Long shopId) {

        Response<Optional<CategoryAuthe>> categoryAuthResp = vegaCategoryAuthByShopIdCacherService.findByShopId(shopId);
        if (!categoryAuthResp.isSuccess()) {
            log.error("find category discount list fail, shopId:{}, cause:{}",
                    shopId, categoryAuthResp.getError());
            throw new JsonResponseException(categoryAuthResp.getError());
        }
        Optional<CategoryAuthe> categoryAutheOptional = categoryAuthResp.getResult();
        if (categoryAutheOptional.isPresent() && Arguments.notNull(categoryAutheOptional.get())
                && !CollectionUtils.isEmpty(categoryAutheOptional.get().getDiscountList())) {
            return categoryAutheOptional.get().getDiscountList();
        }
        return Collections.<VegaCategoryDiscountDto>emptyList();
    }

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

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
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


}
