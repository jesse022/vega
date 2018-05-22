/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultName;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.ShopSuggestion;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.dto.VegaShopUserDto;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.enums.VegaShopAuthorize;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.web.front.core.events.CreateSecondShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserStatus;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/shop")
public class VegaShops {

    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private UserWriteService<User> userWriteService;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private EventBus eventBus;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    /**
     * 信息分页
     *
     * @param criteria 分页查询条件
     * @return 信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<VegaShopUserDto> vegaShopPaging(VegaShopCriteria criteria) {
        ParanaUser user = UserUtil.getCurrentUser();
        Response<Shop> shopResp = shopReadService.findByUserId(user.getId());
        if (!shopResp.isSuccess()) {
            log.error("failed to find shop by userId = {}, cause : {}", UserUtil.getUserId(), shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }

        criteria.setShopPid(shopResp.getResult().getId());

        Long userId = null;
        if (!Strings.isNullOrEmpty(criteria.getUserName())) {
            Response<User> userResp = userReadService.findBy(criteria.getUserName(), LoginType.NAME);
            if (!userResp.isSuccess()) {
                // 查不到用户返回empty, 不报500
                log.error("failed to find user by userName = {}, loginBy = {}, cause : {}",
                        criteria.getUserName(), LoginType.NAME, userResp.getError());
                return Paging.empty();
            }
            userId = userResp.getResult().getId();
            criteria.setUserId(userId);
        }

        Response<Paging<VegaShop>> resp = vegaShopReadService.pagingSecondaryShop(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging vegaShop by criteria : ({}), cause : {}", criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return packageData(resp.getResult());
    }

    /**
     * 封装信息
     *
     * @param paging 原始数据...
     * @return 新数据...
     */
    private Paging<VegaShopUserDto> packageData(Paging<VegaShop> paging) {
        Paging<VegaShopUserDto> dtoPaging = new Paging<>();
        Shop shop = null;
        VegaShopExtra shopExtra = null;
        VegaShopUserDto dto = null;
        List<VegaShop> vegaShops = paging.getData();
        List<VegaShopUserDto> dtoList = Lists.newArrayList();
        for (VegaShop vegaShop : vegaShops) {
            shop = vegaShop.getShop();
            shopExtra = vegaShop.getShopExtra();

            dto = new VegaShopUserDto(shop, shopExtra);

            dto.setUserName(shop.getUserName());
            dto.setMobile(shop.getPhone());

            dtoList.add(dto);
        }

        dtoPaging.setTotal(paging.getTotal());
        dtoPaging.setData(dtoList);

        return dtoPaging;
    }

    /**
     * 一级经销商设置二级经销商默认倍率
     *
     * @param childShopId 二级经销商公司ID
     * @param discount    倍率
     * @return true: 设置成功, false: 设置失败
     */
    @RequestMapping(value = "/set_purchase_discount", method = RequestMethod.PUT)
    public Boolean setPurchaseDiscount(@RequestParam(value = "childShopId") Long childShopId,
                                       @RequestParam(value = "discount") Integer discount) {
        Response<Shop> parentResp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!parentResp.isSuccess()) {
            log.error("failed to find shop by userId = {}, cause : {}", UserUtil.getUserId(), parentResp.getError());
            throw new JsonResponseException(500, parentResp.getError());
        }
        Response<VegaShopExtra> shopExtraResp = vegaShopReadService.findVegaShopExtraByShopId(childShopId);
        if (!shopExtraResp.isSuccess()) {
            log.error("failed to find shop extra by shopId = {}, cause : {}",
                    childShopId, shopExtraResp.getError());
            throw new JsonResponseException(500, shopExtraResp.getError());
        }

        Shop parent = parentResp.getResult();
        VegaShopExtra childShopExtra = shopExtraResp.getResult();

        // 判断店铺关系是否合法(上下级)
        if (!Objects.equal(parent.getId(), childShopExtra.getShopPid())) {
            log.error("failed to set purchase discount, target shopId = {}, discount = {}," +
                    " cause target.shop.is.not.the.child", childShopId, discount);
            throw new JsonResponseException("target.shop.is.not.the.child");
        }

        Response<Boolean> resp = vegaShopWriteService.changePurchaseDiscount(childShopId, discount);
        if (!resp.isSuccess()) {
            log.error("failed to set purchase discount by shop id = {}, discount = {}, cause : {}",
                    childShopId, discount, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 设置默认会员等级倍率
     *
     * @param discount 倍率JSON
     * @return true: 设置成功, false: 设置失败
     */
    @RequestMapping(value = "/set_default_member_discount", method = RequestMethod.PUT)
    public Boolean setDefaultMemberDiscount(@RequestParam(value = "discount") String discount) {
        checkArgument(notNull(discount), "discount.is.empty");
        Response<VegaShop> resp = vegaShopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error("failed to find shop by userId = {}, cause : {}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        Long shopId = resp.getResult().getShopExtra().getShopId();
        Response<Boolean> respResult = vegaShopWriteService.changeDefaultMemberDiscount(shopId, discount);
        if (!resp.isSuccess()) {
            log.error("failed to set default member discount by shopId = {}, discount = {}, cause : {}",
                    shopId, discount, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return respResult.getResult();
    }

    /**
     * 获取默认会员等级倍率
     *
     * @return 倍率信息
     */
    @RequestMapping(value = "/get_default_member_discount", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RichShopMemberDiscount getDefaultDiscount() {
        Long userId = UserUtil.getUserId();
        // 店铺信息
        Response<VegaShop> resp = vegaShopReadService.findByUserId(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by userId = {}, cause : {}", userId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        Response<VegaShopExtra> shopExtraResp = vegaShopReadService.findVegaShopExtraByUserId(userId);
        if (!shopExtraResp.isSuccess()) {
            log.error("failed to find shop extra by userId = {}, cause : {}", userId, shopExtraResp.getError());
            throw new JsonResponseException(500, shopExtraResp.getError());
        }

        Map<String, Integer> defaultMemberDiscount = resp.getResult().getShopExtra().getMemberDiscount();

        // 会员等级信息
        Response<List<Rank>> respRank = rankReadService.findAll();
        if (!respRank.isSuccess()) {
            log.error("failed to get all rank, cause : {}", respRank.getError());
            throw new JsonResponseException(500, respRank.getError());
        }
        List<Rank> ranks = respRank.getResult();
        if (!ShopTypeHelper.isSecondDealerShop(resp.getResult().getShop().getType())) {
            Rank secondShop = new Rank();
            secondShop.setId(DefaultId.SECOND_SHOP_RANK_ID);
            secondShop.setName(DefaultName.SECOND_SHOP_NAME);
            ranks.add(secondShop);
        }


        for (Rank rank : ranks) {
            rank.setDiscount(defaultMemberDiscount.get(String.valueOf(rank.getId())));
        }

        RichShopMemberDiscount richShopMemberDiscount = new RichShopMemberDiscount();
        richShopMemberDiscount.setVegaShop(resp.getResult());
        richShopMemberDiscount.setRankList(ranks);
        richShopMemberDiscount.setDiscountLowerLimit(shopExtraResp.getResult().getDiscountLowerLimit());

        return richShopMemberDiscount;
    }

    /**
     * 根据名称查询当前店铺的下级经销商信息
     *
     * @param name 下级经销商名称
     * @return 店铺信息
     */
    @RequestMapping(value = "/find-child-shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopSuggestion> findChildShopsByName(@RequestParam(value = "name") String name) {
        // 获取当前用户店铺ID
        ParanaUser user = UserUtil.getCurrentUser();
        Shop shop = shopCacher.findShopById(user.getShopId());

        // 根据当前用户店铺ID查询下级经销商信息
        Response<List<VegaShopExtra>> resp = vegaShopReadService.findShopByPidAndName(shop.getId(), name);
        if (!resp.isSuccess()) {
            log.error("failed to find vega shop extra by name like ({}), cause : {}",
                    name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        // 组装数据
        ShopSuggestion suggestion = null;
        List<VegaShopExtra> shopExtraList = resp.getResult();
        List<ShopSuggestion> suggestionList = Lists.newArrayListWithCapacity(shopExtraList.size());
        for (VegaShopExtra shopExtra : shopExtraList) {
            suggestion = new ShopSuggestion();
            suggestion.setId(shopExtra.getShopId());
            suggestion.setUserId(shopExtra.getUserId());
            suggestion.setName(shopExtra.getShopName());

            suggestionList.add(suggestion);
        }
        return suggestionList;
    }

    /**
     * 店铺对应的默认会员折扣信息
     */
    @Data
    private class RichShopMemberDiscount implements Serializable {

        private static final long serialVersionUID = -7797874052138349178L;
        // 店铺(经销商)信息
        VegaShop vegaShop;
        // 对应的折扣
        List<Rank> rankList;
        // 店铺的最低倍率
        Integer discountLowerLimit;
    }

    /**
     * 一级经销商创建二级经销商
     * steps:
     * 1. 创建关联用户
     * 2. 创建店铺
     *
     * @param vegaShop 店铺信息
     * @return 店铺ID
     */
    @RequestMapping(method = RequestMethod.POST)
    public Long createVegaShop(@RequestBody VegaShopUserDto vegaShop) {
        if (vegaShop.getShop() == null || vegaShop.getShopExtra() == null) {
            log.error("failed to create shop, cause shop and vegaShopExtra is null");
            throw new JsonResponseException(500, "shop.is.null");
        }
        // 店铺基本信息
        Shop shop = vegaShop.getShop();
        VegaShopExtra shopExtra = vegaShop.getShopExtra();

        // 店铺关联的用户信息
        User user = createUser(shop, vegaShop.getUserName(), vegaShop.getPassword(), vegaShop.getMobile());

        shop.setUserId(user.getId());
        shop.setUserName(user.getName());
        shop.setPhone(vegaShop.getMobile());

        // TODO 状态
        shop.setStatus(VegaShopStatus.WAIT.value());

        shopExtra = updateVegaShopExtra(shop, user, shopExtra);

        CreditAlterResume resume = null;
        if (shopExtra.getIsCreditAvailable()) {
            // TODO 一级经销商是否可以操作信用额度
            resume = createCreditAlterResume(shopExtra.getTotalCredit(), user);
        }

        Response<Long> shopResp = vegaShopWriteService.create(shop, shopExtra, resume);
        if (!shopResp.isSuccess()) {
            log.error("failed to create shop : ({}), cause : {}", vegaShop, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            eventBus.post(CreateSecondShopEvent.from(shopResp.getResult()));
        }
        return shopResp.getResult();
    }

    /**
     * 更新店铺信息
     *
     * @param vegaShop 店铺信息
     * @return 更新结果
     */
    @RequestMapping(method = RequestMethod.PUT)
    public Boolean updateVegaShop(@RequestBody VegaShopUserDto vegaShop) {
        if (vegaShop.getShop() == null || vegaShop.getShopExtra() == null) {
            log.error("failed to create shop, cause shop and vegaShopExtra is null");
            throw new JsonResponseException(500, "shop.is.null");
        }

        Shop shop = vegaShop.getShop();
        VegaShopExtra shopExtra = vegaShop.getShopExtra();
        VegaShopExtra originalShopExtra = findShopExtraByShopId(shop.getId());

        User user = new User();
        user.setId(shop.getUserId());
        user.setName(vegaShop.getUserName());
        user.setMobile(vegaShop.getMobile());

        shopExtra = updateEditVegaShopExtra(shop, user, shopExtra);

        Long currentShopId = ((ParanaUser)UserUtil.getCurrentUser()).getShopId();
        Shop parent = findShopById(currentShopId);
        shopExtra.setShopPid(parent.getId());
        shopExtra.setShopParentName(parent.getName());
        shop.setUserName(user.getName());
        shop.setPhone(user.getMobile());

        shop.setStatus(originalShopExtra.getShopStatus());
        if (Objects.equal(originalShopExtra.getShopStatus(), VegaShopStatus.NO_PASS.value())) {
            shop.setStatus(VegaShopStatus.WAIT.value());
            user.setStatus(UserStatus.FROZEN.value());
        }
        // 公司名修改需要审核
        if (!Objects.equal(originalShopExtra.getShopName(), shopExtra.getShopName())) {
            shop.setStatus(VegaShopStatus.WAIT.value());
            user.setStatus(UserStatus.FROZEN.value());
        }
        // 银行卡需要审核, 新旧卡号不为空且不相等
        if(!Objects.equal(shopExtra.getBankAccount(), originalShopExtra.getBankAccount())) {
            shop.setStatus(VegaShopStatus.WAIT.value());
            user.setStatus(UserStatus.FROZEN.value());
        }

        Response<Boolean> userResp = userWriteService.update(user);
        if (!userResp.isSuccess()) {
            log.error("failed to update user = ({}), cause : {}", user, userResp.getError());
            throw new JsonResponseException(500, userResp.getError());
        }

        CreditAlterResume resume = null;
        if (shopExtra.getIsCreditAvailable()) {
            ParanaUser currentUser = UserUtil.getCurrentUser();
            if (!Objects.equal(shopExtra.getTotalCredit(), originalShopExtra.getTotalCredit())) {
                resume = generateUpdateResume(currentUser, shopExtra, originalShopExtra);
                shopExtra.setAvailableCredit(resume.getAvailableCredit());
            }
        }

        Response<Boolean> resp = vegaShopWriteService.update(shop, shopExtra, resume);
        if (!resp.isSuccess()) {
            log.error("failed to update shop : ({}), cause : {}", vegaShop, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }


    /**
     * 根据公司名称查询公司是否存在
     *
     * @param shopName 公司名称
     * @return 存在: true, 不存在: false
     */
    @RequestMapping(value = "/exists", method = RequestMethod.GET)
    public Boolean findShopByFullName(@RequestParam(value = "shopName") String shopName) {
        Response<Shop> resp = shopReadService.findByName(shopName);
        if (!resp.isSuccess()) {
            // 不存在
            log.error("shop shopName = ({}) not exists", shopName);
            return Boolean.FALSE;
        }
        // 存在
        return Boolean.TRUE;
    }

    /**
     * 判断用户名是否存在
     *
     * @param userName 用户名
     * @return 存在: true, 不存在: false
     */
    @RequestMapping(value = "/user_exists", method = RequestMethod.GET)
    public Boolean findUserExists(@RequestParam(value = "userName") String userName) {
        Response<User> resp = userReadService.findBy(userName, LoginType.NAME);
        if (!resp.isSuccess()) {
            // 不存在
            log.error("user userName = ({}) not exists", userName);
            return Boolean.FALSE;
        }
        // 存在
        return Boolean.TRUE;
    }

    /**
     * 判断用户名是否存在
     *
     * @param mobile 用户名
     * @return 存在: true, 不存在: false
     */
    @RequestMapping(value = "/mobile_exists", method = RequestMethod.GET)
    public Boolean findMobileExists(@RequestParam(value = "mobile") String mobile) {
        Response<User> resp = userReadService.findBy(mobile, LoginType.MOBILE);
        if (!resp.isSuccess()) {
            // 不存在
            log.error("user mobile = ({}) not exists", mobile);
            return Boolean.FALSE;
        }
        // 存在
        return Boolean.TRUE;
    }

    /**
     * 通过名称模糊
     *
     * @param name 名称
     * @return 信息列表
     */
    @RequestMapping(value = "/name-suggestion", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopSuggestion> findByName(@RequestParam(value = "name") String name) {
        Response<List<VegaShopExtra>> resp = vegaShopReadService.findSuggestionByName(name);
        if (!resp.isSuccess()) {
            log.error("failed to find vega shop extra by name like ({}), cause : {}",
                    name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSuggestion(resp.getResult());
    }

    /**
     * 分装suggestion
     *
     * @param shopExtraList 店铺信息
     * @return suggestion
     */
    private List<ShopSuggestion> generateSuggestion(List<VegaShopExtra> shopExtraList) {
        List<ShopSuggestion> suggestionList = Lists.newArrayListWithCapacity(shopExtraList.size());

        shopExtraList.forEach(shopExtra -> {
            ShopSuggestion suggestion = new ShopSuggestion();
            suggestion.setName(shopExtra.getShopName());
            suggestion.setId(shopExtra.getShopId());
            suggestion.setUserId(shopExtra.getUserId());

            suggestionList.add(suggestion);
        });

        return suggestionList;
    }

    /**
     * 生成编辑履历
     *
     * @param currentUser       当前用户
     * @param shopExtra         店铺信息
     * @param originalShopExtra 编辑前的店铺信息
     * @return 履历信息
     */
    private CreditAlterResume generateUpdateResume(ParanaUser currentUser,
                                                   VegaShopExtra shopExtra,
                                                   VegaShopExtra originalShopExtra) {
        // 原始总额度
        Long orgTotalCredit = originalShopExtra.getTotalCredit() == null ? 0 : originalShopExtra.getTotalCredit();
        // 原始可用额度
        Long orgAvailableCredit = originalShopExtra.getAvailableCredit() == null ? 0 : originalShopExtra.getAvailableCredit();
        // 编辑修改之后的总额度
        Long nowTotalCredit = shopExtra.getTotalCredit();
        // 编辑修改之后的可用额度
        Long nowAvailableCredit = 0L;

        CreditAlterResume resume = new CreditAlterResume();
        resume.setShopId(shopExtra.getShopId());
        resume.setShopName(shopExtra.getShopName());
        resume.setOperateName(currentUser.getName());
        resume.setOperateId(currentUser.getId());

        Long alterValue = nowTotalCredit - orgTotalCredit;
        // 计算新可用额度
        if (alterValue >= 0) {
            // 相对原有额度增加
            nowAvailableCredit = orgAvailableCredit + alterValue;
            resume.setAlterType(CreditAlterType.ADMIN_ADD.value());
        } else {
            Long remain = nowTotalCredit - orgAvailableCredit;
            if (remain >= 0) {
                nowAvailableCredit = orgAvailableCredit;
                resume.setAlterType(CreditAlterType.ADMIN_ADD.value());
            } else {
                nowAvailableCredit = orgAvailableCredit + alterValue;
                resume.setAlterType(CreditAlterType.ADMIN_REDUCE.value());
            }
        }

        resume.setAlterValue(alterValue);
        resume.setTotalCredit(nowTotalCredit);
        resume.setAvailableCredit(nowAvailableCredit);

        return resume;
    }

    /**
     * 通过店铺ID查找店铺信息
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private VegaShopExtra findShopExtraByShopId(Long shopId) {
        Response<VegaShopExtra> resp = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find vegaShop by shopId = ({}), cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 更新shopExtra信息
     *
     * @param shop      店铺
     * @param user      用户
     * @param shopExtra 店铺Extra
     * @return 更新后的信息
     */
    private VegaShopExtra updateEditVegaShopExtra(Shop shop, User user, VegaShopExtra shopExtra) {
        shopExtra.setUserId(user.getId());
        shopExtra.setShopId(shop.getId());
        shopExtra.setShopName(shop.getName());
        shopExtra.setUserName(user.getName());
        return shopExtra;
    }

    /**
     * 创建店铺的关联用户
     *
     * @param shop 店铺信息
     * @return 新建关联用户ID
     */
    private User createUser(Shop shop, String userName, String password, String mobile) {
        // 创建关联用户
        User user = new User();
        user.setStatus(UserStatus.FROZEN.value());
        user.setName(userName);
        user.setPassword(password);
        user.setMobile(mobile);

        // 二级经销商角色、类型
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            user.setType(UserType.NORMAL.value());
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.DEALER_SECOND.name()));
        }

        Response<Long> userResp = userWriteService.create(user);
        if (!userResp.isSuccess()) {
            log.error("failed to create an user userName = ({}) related to shop shopName = ({}), cause : {}",
                    shop.getName(), userResp.getError());
            throw new JsonResponseException(500, userResp.getError());
        }
        user.setId(userResp.getResult());

        return user;
    }

    /**
     * 修改shopExtra
     *
     * @param shop      店铺基本信息
     * @param user      用户信息
     * @param shopExtra 店铺额外信息
     * @return 修改后的信息
     */
    private VegaShopExtra updateVegaShopExtra(Shop shop, User user, VegaShopExtra shopExtra) {
        shopExtra.setShopName(shop.getName());
        shopExtra.setUserId(user.getId());
        shopExtra.setUserName(user.getName());
        shopExtra.setShopStatus(shop.getStatus());
        shopExtra.setShopType(shop.getType());
        if (shopExtra.getIsCreditAvailable()) {
            shopExtra.setAvailableCredit(shopExtra.getTotalCredit());
        } else {
            // 额度不可用, 总额度为空, 可用额度为空
            shopExtra.setTotalCredit(null);
            shopExtra.setAvailableCredit(null);
        }

        // TODO 倍率设置
        // 默认采购折扣
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            shopExtra.setPurchaseDiscount(DefaultDiscount.DEALER_SECOND_PURCHASE_DISCOUNT);
        }
        // 默认会员倍率
        if (!ShopTypeHelper.isSupplierShop(shop.getType())) {
            Response<List<Rank>> rankResp = rankReadService.findAll();
            if (!rankResp.isSuccess()) {
                log.error("failed to find all ranks, cause : {}", rankResp.getError());
                throw new JsonResponseException(500, rankResp.getError());
            }
            Map<String, Integer> rankDiscountMap = Maps.newHashMap();
            for (Rank rank : rankResp.getResult()) {
                rankDiscountMap.put(String.valueOf(rank.getId()), DefaultDiscount.MEMBER_RANK_DISCOUNT);
            }
            shopExtra.setMemberDiscount(rankDiscountMap);
        }

        Long currentShopId = ((ParanaUser)UserUtil.getCurrentUser()).getShopId();
        VegaShop parent = findVegaShopByShopId(currentShopId);
        shopExtra.setShopPid(parent.getShop().getId());
        shopExtra.setShopParentName(parent.getShop().getName());
        if (Objects.equal(parent.getShopExtra().getShopAuthorize(), VegaShopAuthorize.NOT_AUTHORIZE.value())) {
            shopExtra.setShopAuthorize(VegaShopAuthorize.NOT_AUTHORIZE.value());
        } else {
            shopExtra.setShopAuthorize(VegaShopAuthorize.AUTHORIZE.value());
        }

        return shopExtra;
    }

    /**
     * 通过ID查找店铺
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        Response<Shop> resp = shopReadService.findById(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by id = ({}), cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
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
            throw new JsonResponseException(500, shopResp.getError());
        }
        return shopResp.getResult();
    }

    /**
     * 额度履历
     *
     * @param totalCredit 总额度
     * @param user        用户信息
     * @return 履历信息
     */
    private CreditAlterResume createCreditAlterResume(Long totalCredit, User user) {
        // 变更之后的值不能小于0
        if (totalCredit < 0) {
            log.error("failed to create shop, cause shop credit could not less than zero");
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();

        ParanaUser currentUser = UserUtil.getCurrentUser();
        resume.setOperateId(currentUser.getId());
        resume.setOperateName(currentUser.getName());

        resume.setLastCredit(0L);
        resume.setAlterValue(totalCredit);
        resume.setAlterType(CreditAlterType.ADMIN_ADD.value());
        resume.setCreatedAt(new Date());
        resume.setNewestCredit(totalCredit);
        resume.setAvailableCredit(totalCredit);
        resume.setTotalCredit(totalCredit);
        resume.setIsPaymentComplete(true);

        return resume;
    }

    @RequestMapping("/test")
    public String testEvent() {
        return "success";
    }

}
