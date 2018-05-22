package com.sanlux.web.admin.shop;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.user.service.ShopUserWriteService;
import com.sanlux.user.service.UserExtWriteService;
import com.sanlux.user.service.UserRankWriteService;
import com.sanlux.web.front.core.events.CreateSecondShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户升级/降级control
 *    1.普通用户升级为一级经销商
 *    2.普通用户升级为二级经销商
 *    3.二级经销商降级为普通用户
 * 下面三种情况运营要求取消,前端暂时不需要对接
 *    4.二级经销商升级为一级经销商
 *    5.一级经销商降级为二级经销商(暂未考虑一级所属二级的业务处理情况)
 *    6.一级经销商降级为普通用户
 * Created by lujm on 2017/3/10.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/admin/move")
public class AdminUserMove {
    @RpcConsumer
    private UserWriteService<User> userWriteService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private UserExtWriteService userExtWriteService;
    @RpcConsumer
    private UserRankWriteService userRankWriteService;
    @Autowired
    private EventBus eventBus;


    /**
     * 普通用户升级为经销商
     * steps:
     * 1. 更新用户表信息
     * 2. 新建店铺信息
     * 3. 自动解除普通用户的会员专属关系
     * 4. 设置类目折扣(二级经销商)
     *
     * @param userId   用户ID
     * @param shopType 升级类型 2:一级 3:二级
     * @param shopID   上级店铺ID 升级为二级经销商时需要传入
     * @return 店铺ID
     */
    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public Boolean userMove(@RequestParam Long userId,
                            @RequestParam Integer shopType,
                            @RequestParam(required = false) Long shopID) {
        if (userId == null) {
            log.error("failed to move user, cause user id is null");
            throw new JsonResponseException(500, "user.id.null");
        }
        Response<VegaShop> resp = vegaShopReadService.findByUserId(userId);
        if (resp.isSuccess()) {
            log.info("failed to move user, cause shop by userId = {} is exist", userId);
            throw new JsonResponseException(500, "shop.user.exist.move.fail");
        }

        /**
         * 1.更新用户表信息
         * */
        User user = updateUser(userId, shopType);

        /**
         * 2.新建店铺基本信息
         * */
        VegaShop vegaShop = new VegaShop();
        Shop shop = new Shop();
        VegaShopExtra shopExtra = new VegaShopExtra();

        shop.setUserId(user.getId());
        shop.setUserName(user.getName());
        shop.setName(user.getName());//店铺名称默认为用户名名称
        shop.setPhone(user.getMobile());

        if (ShopTypeHelper.isFirstDealerShop(shopType)) {
            //一级经销商
            shop.setStatus(VegaShopStatus.FROZEN.value());//新增的一级经销商,待维护完供货区域和授权类目之后再解冻
            shop.setType(VegaShopType.DEALER_FIRST.value());
        } else {
            //二级经销商
            shop.setStatus(VegaShopStatus.WAIT.value());//待审核状态
            shop.setType(VegaShopType.DEALER_SECOND.value());

            if (shopID == null) {
                log.error("failed to move user with dealer_second, cause shop pid is null");
                throw new JsonResponseException(500, "shop.id.is.null");
            }
        }
        shopExtra = updateVegaShopExtra(shop, user, shopExtra, shopID);
        Response<Long> shopResp = vegaShopWriteService.create(shop, shopExtra);
        vegaShop.setShop(shop);
        vegaShop.setShopExtra(shopExtra);

        if (!shopResp.isSuccess()) {
            log.error("failed to create shop : ({}), cause : {}", vegaShop, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }

        /**
         * 3.删除专属会员关系
         * */
        Response<Boolean> response = shopUserWriteService.deleteShopUserById(userId);
        if (!response.isSuccess()) {
            log.error("delete shop user  fail,userId{},error{}", userId, response.getError());
            throw new JsonResponseException(response.getError());
        }

        /**
         * 4.升级为二级经销商时,需设置二级类目折扣
         * */
        if (ShopTypeHelper.isSecondDealerShop(shopType)) {
            eventBus.post(CreateSecondShopEvent.from(shopResp.getResult()));
        }

        return Boolean.TRUE;
    }

    /**
     * 一级二级经销商相互转换;经销商降级为普通用户
     * steps:
     * 1. 更新用户表信息
     * 2. 更新店铺信息
     * 3. 解除经销商的专属会员关系(经销商降级为普通用户)
     * 4. 初始化会员等级信息(经销商降级为普通用户)
     *
     * @param shopId   店铺ID
     * @param shopType 升/降级类型 2:一级 3:二级 1:普通用户
     * @param shopPID  上级店铺ID 降级为二级经销商时需要传入
     * @return 店铺ID
     */
    @RequestMapping(value = "/dealer", method = RequestMethod.GET)
    public Boolean dealerMove(@RequestParam Long shopId,
                              @RequestParam Integer shopType,
                              @RequestParam(required = false) Long shopPID) {

        Response<VegaShop> vegaShopResponse= vegaShopReadService.findByShopId(shopId);
        if (!vegaShopResponse.isSuccess()) {
            log.error("failed to find shop by shopId = {}, cause : {}", shopId, vegaShopResponse.getError());
            throw new JsonResponseException(500, vegaShopResponse.getError());
        }
        /**
         * 1.更新用户信息
         * */
        updateUser(vegaShopResponse.getResult().getShop().getUserId(), shopType);

        /**
         * 2.更新店铺基本信息
         * */
        VegaShop vegaShop = new VegaShop();
        Shop shop = new Shop();
        VegaShopExtra shopExtra = new VegaShopExtra();

        shop.setId(shopId);
        if (ShopTypeHelper.isFirstDealerShop(shopType)) {
            //二级升级为一级经销商
            shop.setStatus(VegaShopStatus.FROZEN.value());//冻结状态
            shop.setType(VegaShopType.DEALER_FIRST.value());
            shopExtra.setShopStatus(VegaShopStatus.FROZEN.value());
            shopExtra.setShopType(VegaShopType.DEALER_FIRST.value());

            //解除二级经销商上级店铺信息
            shopExtra.setShopPid(null);
            shopExtra.setShopParentName(null);
        } else if(ShopTypeHelper.isSecondDealerShop(shopType)){
            //一级降级为二级经销商
            shop.setStatus(VegaShopStatus.WAIT.value());//待审核状态
            shop.setType(VegaShopType.DEALER_SECOND.value());
            shopExtra.setShopStatus(VegaShopStatus.WAIT.value());
            shopExtra.setShopType(VegaShopType.DEALER_SECOND.value());

            //关联二级经销商上级店铺信息
            shopExtra.setShopPid(shopPID);
            shopExtra.setShopParentName(getShop(shopPID).getShop().getName());
        }else{
            //经销商降级为普通用户
            shop.setStatus(VegaShopStatus.FROZEN.value());//冻结状态
            shopExtra.setShopStatus(VegaShopStatus.FROZEN.value());//冻结状态
            shopExtra.setShopPid(-1L);//代表上级店铺信息不变,update时特殊处理
            shopExtra.setShopParentName("");//代表上级店铺信息不变,update时特殊处理

            /**
             * 3.删除经销商对应的专属会员关系
             * */
            Response<Boolean> response = shopUserWriteService.deleteShopUserByShopId(shopId);
            if (!response.isSuccess()) {
                log.error("delete shop user  fail,shopId{},error{}", shop.getId(), response.getError());
                throw new JsonResponseException(response.getError());
            }

            /**
             * 4.初始化会员等级信息
             * */
            Response<Boolean> booleanResponse = userRankWriteService.initUserRank(vegaShopResponse.getResult().getShop().getUserId());
            if (!booleanResponse.isSuccess()) {
                log.error("user init fail , userId {} , case {}", vegaShopResponse.getResult().getShop().getUserId(), booleanResponse.getError());
            }
        }
        Response<Boolean> resp = vegaShopWriteService.updateShopStatus(shop, shopExtra);
        vegaShop.setShop(shop);
        vegaShop.setShopExtra(shopExtra);
        if (!resp.isSuccess()) {
            log.error("failed to update shop : ({}), cause : {}", vegaShop, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return Boolean.TRUE;
    }

    /**
     * 获取店铺信息
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private VegaShop getShop(Long shopId){
        Response<VegaShop> resp = vegaShopReadService.findByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by shopId = {}, cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param shopType 经销商类型
     * @return 用户信息
     */
    private User updateUser(Long userId, Integer shopType) {
        Response<User> resp = userReadService.findById(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
            throw new JsonResponseException(500, "user.id.null");
        }
        User user = resp.getResult();
        if (Objects.equal(user.getName(),null)) {
            log.info("user move fail because user name is null");
            throw new JsonResponseException(500, "user.name.null");
        }
        if (ShopTypeHelper.isFirstDealerShop(shopType)) {
            // 一级经销商角色、类型
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.DEALER_FIRST.name()));
        }else if (ShopTypeHelper.isSecondDealerShop(shopType)) {
            // 二级经销商角色、类型
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.DEALER_SECOND.name()));
        }else{
            //普通用户
            user.setRoles(Arrays.asList(UserRole.BUYER.name()));
        }

        Response<Boolean> userResp = userExtWriteService.update(user);
        if (!userResp.isSuccess()) {
            log.error("failed to update user = ({}), cause : {}", user, userResp.getError());
            throw new JsonResponseException(500, userResp.getError());
        }
        return user;
    }

    /**
     * 修改shopExtra
     * @param shop      店铺基本信息
     * @param user      用户信息
     * @param shopExtra 店铺额外信息
     * @param shopID    上级店铺ID
     * @return 修改后的信息
     */
    private VegaShopExtra updateVegaShopExtra(Shop shop, User user, VegaShopExtra shopExtra, Long shopID) {
        shopExtra.setShopName(shop.getName());
        shopExtra.setUserId(user.getId());
        shopExtra.setUserName(user.getName());
        shopExtra.setShopStatus(shop.getStatus());
        shopExtra.setShopType(shop.getType());

        // 默认采购折扣
        if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            shopExtra.setPurchaseDiscount(DefaultDiscount.NOT_FIND_DISCOUNT);
        }
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            shopExtra.setPurchaseDiscount(DefaultDiscount.DEALER_SECOND_PURCHASE_DISCOUNT);
        }
        // 默认会员倍率
        Response<List<Rank>> rankResp = rankReadService.findAll();
        if (!rankResp.isSuccess()) {
            log.error("failed to find all ranks, cause : {}", rankResp.getError());
            throw new JsonResponseException(500, rankResp.getError());
        }
        Map<String, Integer> rankDiscountMap = Maps.newHashMap();
        for (Rank rank : rankResp.getResult()) {
            rankDiscountMap.put(String.valueOf(rank.getId()), DefaultDiscount.MEMBER_RANK_DISCOUNT);
        }
        if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            rankDiscountMap.put(String.valueOf(DefaultId.SECOND_SHOP_RANK_ID), DefaultDiscount.MEMBER_RANK_DISCOUNT);
        }
        shopExtra.setMemberDiscount(rankDiscountMap);
        //设置二级经销商上级信息
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            shopExtra.setShopPid(shopID);
            shopExtra.setShopParentName(getShop(shopID).getShop().getName());
        }
        return shopExtra;
    }


}
