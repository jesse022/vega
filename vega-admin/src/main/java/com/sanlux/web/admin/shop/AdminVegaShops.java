/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.shop;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SettleConstants;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.item.dto.RichShopSku;
import com.sanlux.item.dto.VegaItemStockDto;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.service.ShopItemReadService;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.ShopSuggestion;
import com.sanlux.shop.dto.SmsNode;
import com.sanlux.shop.dto.SmsNodeDto;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.dto.VegaShopUserDto;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.enums.CreditSmsNodeEnum;
import com.sanlux.shop.enums.VegaShopAuthorize;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.settle.enums.CommissionRate;
import com.sanlux.user.model.DeliveryScope;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.DeliveryScopeReadService;
import com.sanlux.user.service.DeliveryScopeWriteService;
import com.sanlux.user.service.RankReadService;
import com.sanlux.web.front.core.events.CreateSecondShopEvent;
import com.sanlux.web.front.core.events.DeliveryScopeUpdateEvent;
import com.sanlux.web.front.core.events.ShopCategoryAuthEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserStatus;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.AdminShopWriteService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import io.terminus.parana.web.core.events.user.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/admin/shops")
public class AdminVegaShops {

    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private AdminShopWriteService adminShopWriteService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private ShopWriteService shopWriteService;

    @RpcConsumer
    private UserWriteService<User> userWriteService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private RankReadService rankReadService;

    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;

    @RpcConsumer
    private ShopItemReadService shopItemReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private DeliveryScopeReadService deliveryScopeReadService;

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private DeliveryScopeWriteService deliveryScopeWriteService;

    @Autowired
    private EventBus eventBus;


    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();

    /**
     * 创建店铺(经销商)
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

        //新增的(授权)一级经销商,待维护完供货区域和授权类目之后再解冻
        if (ShopTypeHelper.isFirstDealerShop(shop.getType()) &&
                !Objects.equal(shopExtra.getShopAuthorize(), VegaShopAuthorize.NOT_AUTHORIZE.value())) {
            shop.setStatus(VegaShopStatus.FROZEN.value());
        } else {
            shop.setStatus(VegaShopStatus.NORMAL.value());

        }

        shopExtra = updateVegaShopExtra(shop, user, shopExtra);

        CreditAlterResume resume = null;
        // 非供应商且信用额度可用
        if (!ShopTypeHelper.isSupplierShop(shop.getType())) {
            if (shopExtra.getIsCreditAvailable()) {
                resume = createCreditAlterResume(shopExtra.getTotalCredit(), user);
            }
        }

        Response<Long> shopResp = vegaShopWriteService.create(shop, shopExtra, resume);
        if (!shopResp.isSuccess()) {
            log.error("failed to create shop : ({}), cause : {}", vegaShop, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            eventBus.post(CreateSecondShopEvent.from(shopResp.getResult()));
        }
        if (ShopTypeHelper.isSupplierShop(shop.getType())) {
            //新建供应商账户时,作为普通买家身份需要初始化会员等级信息
            eventBus.post(new UserRegisteredEvent(user.getId()));
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

        User user = new User();
        user.setId(shop.getUserId());
        user.setName(vegaShop.getUserName());
        user.setMobile(vegaShop.getMobile());
        Response<Boolean> userResp = userWriteService.update(user);
        if (!userResp.isSuccess()) {
            log.error("failed to update user = ({}), cause : {}", user, userResp.getError());
            throw new JsonResponseException(500, userResp.getError());
        }

        shopExtra = updateEditVegaShopExtra(shop, user, shopExtra);
        setShopParentInfo(shop, shopExtra);
        shop.setUserName(user.getName());
        shop.setPhone(user.getMobile());

        VegaShopExtra originalShopExtra = findShopExtraByShopId(shop.getId());

        CreditAlterResume resume = null;
        if (!ShopTypeHelper.isSupplierShop(shop.getType())) {
            if (shopExtra.getIsCreditAvailable()) {
                ParanaUser currentUser = UserUtil.getCurrentUser();
                if (!Objects.equal(shopExtra.getTotalCredit(), originalShopExtra.getTotalCredit())) {
                    resume = generateUpdateResume(currentUser, shopExtra, originalShopExtra);
                    shopExtra.setAvailableCredit(resume.getAvailableCredit());
                }
            }
        }

        //修改为未授权的一级经销商时,需清空店铺的供货区域信息
        if (ShopTypeHelper.isFirstDealerShop(shop.getType()) &&
                Objects.equal(shopExtra.getShopAuthorize(), VegaShopAuthorize.NOT_AUTHORIZE.value())) {
            deleteFirstShopDeliveryScope(shop.getId());
        }

        Response<Boolean> resp = vegaShopWriteService.update(shop, shopExtra, resume);
        if (!resp.isSuccess()) {
            log.error("failed to update shop : ({}), cause : {}", vegaShop, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 信息分页
     *
     * @param criteria 分页查询条件
     * @return 信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<VegaShopUserDto> vegaShopPaging(VegaShopCriteria criteria) {
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
        Response<Paging<VegaShop>> resp = vegaShopReadService.paging(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging vegaShop by criteria : ({}), cause : {}", criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        return packageData(resp.getResult());
    }

    /**
     * 根据店铺ID查找
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    @RequestMapping(value = "/{shopId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaShop findByShopId(@PathVariable(value = "shopId") Long shopId) {
        Response<VegaShop> resp = vegaShopReadService.findByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find vegaShop by shopId = ({}), cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 通过名称模糊查询一级经销商信息, 没有数据返回空List集合
     *
     * @param name 名称
     * @return 信息列表
     */
    @RequestMapping(value = "/first-dealer/find-by-name", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopSuggestion> findShopsByName(@RequestParam(value = "name") String name) {
        Response<List<VegaShopExtra>> resp = vegaShopReadService.findFirstLevelShopByName(name);
        if (!resp.isSuccess()) {
            log.error("failed to find vega shop extra by name like ({}), cause : {}",
                    name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSuggestion(resp.getResult());
    }

    /**
     * 通过名称模糊查询一级经销商信息, 没有数据返回空List集合
     *
     * @param name 名称
     * @return 信息列表
     */
    @RequestMapping(value = "/supplier", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopSuggestion> findSupplierByName(@RequestParam(value = "name") String name) {
        Response<List<VegaShopExtra>> resp = vegaShopReadService.findSupplierByName(name);
        if (!resp.isSuccess()) {
            log.error("failed to find vega shop extra by name like ({}), cause : {}",
                    name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSuggestion(resp.getResult());
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
     * 封装suggestion
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
     * 根据公司名称查询一级经销商公司是否存在
     *
     * @param shopName 公司名称
     * @return 存在: true, 不存在: false
     */
    @RequestMapping(value = "/first-level-exists", method = RequestMethod.GET)
    public Boolean findShopByName(@RequestParam(value = "shopName") String shopName) {
        Response<Shop> resp = shopReadService.findByName(shopName);

        // 店铺不存在
        if (!resp.isSuccess()) {
            log.error("failed to find shop by name  = {}, cause : {}", shopName, resp.getError());
            return Boolean.FALSE;
        }
        Shop shop = resp.getResult();

        // 店铺存在, 必须是一级经销商店铺
        if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            return Boolean.TRUE;
        }

        // 非一级经销商店铺
        log.error("result shop shopName = ({}) exists, but it's not the first level of dealer", shopName);
        return Boolean.FALSE;
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
     * 修改公司状态
     *
     * @param shopId       公司ID
     * @param userId       关联用户ID
     * @param status       状态
     * @param noPassReason 审核不通过的原因
     * @return 修改结果, true: 成功, false: 失败
     */
    @RequestMapping(value = "/change_status/{shopId}/{userId}/{status}", method = RequestMethod.PUT)
    public Boolean changeShopStatus(@PathVariable(value = "shopId") Long shopId,
                                    @PathVariable(value = "userId") Long userId,
                                    @PathVariable(value = "status") Integer status,
                                    @RequestParam(value = "noPassReason", required = false) String noPassReason) {
        Response<Boolean> shopResp = vegaShopWriteService.changeShopStatusById(shopId, status, noPassReason);
        if (!shopResp.isSuccess()) {
            log.error("failed to change shop status, target shopId = {}, status = {}, cause : {}",
                    shopId, status, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }

        if (Objects.equal(status, VegaShopStatus.NO_PASS.value())) {
            return Boolean.TRUE;
        }

        // 冻结用户状态
        User user = new User();
        user.setId(userId);
        if (Objects.equal(status, VegaShopStatus.FROZEN.value())) {
            user.setStatus(UserStatus.FROZEN.value());
        }
        if (Objects.equal(status, VegaShopStatus.NORMAL.value())) {
            user.setStatus(UserStatus.NORMAL.value());
        }

        Response<Boolean> userResp = userWriteService.update(user);
        if (!userResp.isSuccess()) {
            log.error("failed to change user status by  userId = {}, status = {}, cause : {}",
                    userId, UserStatus.FROZEN.value(), userResp.getError());
            throw new JsonResponseException(500, userResp.getError());
        }
        return userResp.getResult();
    }

    /**
     * 修改授权状态
     * @param shopId shopId
     * @param authorize 授权状态
     * @return 是否修改成功
     */
    @RequestMapping(value = "/change_authorize/{shopId}/{authorize}", method = RequestMethod.GET)
    public Boolean changeShopAuthorize(@PathVariable(value = "shopId") Long shopId,
                                    @PathVariable(value = "authorize") Integer authorize) {
        Response<Boolean> shopResp = vegaShopWriteService.changeShopAuthorizeById(shopId, authorize);
        if (!shopResp.isSuccess()) {
            log.error("failed to change shop authorize, target shopId = {}, Authorize = {}, cause : {}",
                    shopId, authorize, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }
        if(Objects.equal(authorize,VegaShopAuthorize.NOT_AUTHORIZE.value())){
            deleteFirstShopDeliveryScope(shopId);
        }
        return shopResp.getResult();
    }

    /**
     * 修改是否老会员状态
     * @param shopId shopId
     * @param status 是否老会员标记值
     * @return 是否修改成功
     */
    @RequestMapping(value = "/change_member_type/{shopId}/{status}", method = RequestMethod.GET)
    public Boolean changeShopMemberType(@PathVariable(value = "shopId") Long shopId,
                                        @PathVariable(value = "status") Integer status) {
        if (!Objects.equal(status, DefaultId.DEFAULT_TRUE_ID) && !Objects.equal(status, DefaultId.DEFAULT_FALSE_ID)) {
            log.error("failed to change shop is old member, target shopId = {}, status = {}, cause : {}",
                    shopId, status, "shop.member.type.status.undefined");
            throw new IllegalArgumentException("shop.member.type.status.undefined");
        }
        Response<Boolean> shopResp = vegaShopWriteService.changeShopMemberTypeById(shopId, status);
        if (!shopResp.isSuccess()) {
            log.error("failed to change shop is old member, target shopId = {}, status = {}, cause : {}",
                    shopId, status, shopResp.getError());
            throw new JsonResponseException(500, shopResp.getError());
        }
        return shopResp.getResult();
    }



    /**
     * 修改倍率下限
     *
     * @param shopId             店铺ID
     * @param discountLowerLimit 下限值
     * @return 修改结果
     */
    @RequestMapping("/discount-lower-limit")
    public Boolean changeDiscountLowerLimit(@RequestParam("shopId") Long shopId,
                                            @RequestParam("discountLowerLimit") Integer discountLowerLimit) {
        Response<Boolean> resp = vegaShopWriteService.changeDiscountLowerLimit(shopId, discountLowerLimit);
        if (!resp.isSuccess()) {
            log.error("failed to change discountLowerLimit by shopId = ({}), cause : {}",
                    shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 消费金额与积分比例信息
     *
     * @return 信息
     */
    @RequestMapping(value = "/integral/scale", method = RequestMethod.GET)
    public String getIntegralScale() {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (CollectionUtils.isEmpty(shop.getTags())) {
            return "";
        }
        return shop.getTags().get(SystemConstant.INTEGRAL_SCALE);

    }


    /**
     * 消费金额与成长值比例信息
     *
     * @return 信息
     */
    @RequestMapping(value = "/growth/value", method = RequestMethod.GET)
    public String getGrowthValue() {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (CollectionUtils.isEmpty(shop.getTags())) {
            return "";
        }
        return shop.getTags().get(SystemConstant.GROWTH_VALUE);

    }

    /**
     * 消费金额与积分比例信息
     *
     * @return 信息
     */
    @RequestMapping(value = "/integral/scale", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateIntegralScale(@RequestParam String value) {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SystemConstant.INTEGRAL_SCALE, value);
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }

    /**
     * 消费金额与成长值比例信息
     *
     * @return 信息
     */
    @RequestMapping(value = "/growth/value", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateGrowthValue(@RequestParam String value) {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SystemConstant.GROWTH_VALUE, value);
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }


    /**
     * 获取信用额度利息
     *
     * @return 信息
     */
    @RequestMapping(value = "/credit/interest", method = RequestMethod.GET)
    public String getCreditInterest() {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (CollectionUtils.isEmpty(shop.getTags())) {
            return "";
        }
        return shop.getTags().get(SystemConstant.CREDIT_INTEREST);

    }

    /**
     * 更新信用额度利息
     *
     * @return 信息
     */
    @RequestMapping(value = "/credit/interest", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateCreditInterest(@RequestParam String value) {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SystemConstant.CREDIT_INTEREST, value);
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }


    /**
     * 获取短信配置节点
     *
     * @return 信息
     */
    @RequestMapping(value = "/sms/node", method = RequestMethod.GET)
    public SmsNodeDto getSmsNode() {

        SmsNodeDto dto = new SmsNodeDto();
        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (CollectionUtils.isEmpty(shop.getTags())) {
            return getDefaultNode(dto);
        }

        if (!shop.getTags().containsKey(SystemConstant.CREDIT_SMS_NODE) ||
                !shop.getTags().containsKey(SystemConstant.TRADE_SMS_NODE)) {
            return getDefaultNode(dto);
        }
        dto.setCreditNodes(getSmsNodeFromData(shop.getTags().get(SystemConstant.CREDIT_SMS_NODE)));
        dto.setTradeNodes(getSmsNodeFromData(shop.getTags().get(SystemConstant.TRADE_SMS_NODE)));

        return dto;

    }

    /**
     * 配置短信配置节点
     *
     * @return 信息
     */
    @RequestMapping(value = "/sms/node", method = RequestMethod.PUT)
    public Boolean setSmsNode(@RequestParam String creditSmsNode, @RequestParam String tradeSmsNode) {

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SystemConstant.CREDIT_SMS_NODE, creditSmsNode);
        tags.put(SystemConstant.TRADE_SMS_NODE, tradeSmsNode);
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }

    /**
     * 平台抽佣率
     * @param commissionRate 抽佣率
     * @return 结果
     */
    @RequestMapping(value = "/commission-rate/platform", method = RequestMethod.PUT)
    public Boolean setPlatformCommissionRate(@RequestParam("commissionRate") Integer commissionRate) {
        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SettleConstants.PLATFORM_COMMISSION_RATE, String.valueOf(commissionRate));
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }

    /**
     * 设置一级经销商抽佣率
     * @param commissionRate 抽佣率
     * @return 结果
     */
    @RequestMapping(value = "/commission-rate/first-dealer", method = RequestMethod.PUT)
    public Boolean setFirstDealerCommissionRate(@RequestParam("commissionRate") Integer commissionRate) {
        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();
        tags.put(SettleConstants.FIRST_DEALER_COMMISSION_RATE, String.valueOf(commissionRate));
        return updateShopTags(DefaultId.PLATFROM_SHOP_ID, tags);
    }

    /**
     * 获取佣金费率
     * @return 信息
     */
    @RequestMapping(value = "/commission-rate", method = RequestMethod.GET)
    public Map<String, String> getCommissionRate() {
        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        Map<String, String> tags = shop.getTags();

        String dealerCommission = String.valueOf(MoreObjects.firstNonNull(tags.get(CommissionRate.FIRST_DEALER_COMMISSION_RATE.value()), 0));
        String platformCommission = String.valueOf(MoreObjects.firstNonNull(tags.get(CommissionRate.PLATFORM_COMMISSION_RATE.value()), 0));

        return ImmutableMap.of(
                CommissionRate.PLATFORM_COMMISSION_RATE.value(), platformCommission,
                CommissionRate.FIRST_DEALER_COMMISSION_RATE.value(), dealerCommission
                );
    }

    /**
     * 获取佣金费率类型
     * @return 信息
     */
    @RequestMapping(value = "/commission-rate-type", method = RequestMethod.GET)
    public Map<String, String> getCommissionRateType() {
        Map<String, String> tags = Maps.newHashMap();
        for (CommissionRate rate : CommissionRate.values()) {
            tags.put(rate.value(),  rate.toString());
        }
        return tags;
    }

    private SmsNodeDto getDefaultNode(SmsNodeDto dto) {
        List<SmsNode> creditNodeDtos = Lists.newArrayList();
        for (CreditSmsNodeEnum nodeEnum : CreditSmsNodeEnum.values()) {

            creditNodeDtos.add(initSmsNode(nodeEnum.getName(), nodeEnum.toString()));
        }

        List<SmsNode> tradeNodeDtos = Lists.newArrayList();
        for (TradeSmsNodeEnum nodeEnum : TradeSmsNodeEnum.values()) {

            tradeNodeDtos.add(initSmsNode(nodeEnum.getName(), nodeEnum.toString()));
        }

        dto.setCreditNodes(creditNodeDtos);
        dto.setTradeNodes(tradeNodeDtos);
        return dto;

    }

    private SmsNode initSmsNode(String name, String desc) {
        SmsNode node = new SmsNode();
        node.setIsChecked(0);
        node.setNodeDesc(desc);
        node.setNodeName(name);

        return node;
    }


    private Shop getShopById(Long shopId) {
        Response<Shop> shopResponse = shopReadService.findById(shopId);
        if (!shopResponse.isSuccess()) {
            log.error("failed to find shop by id: ({}) ,error:{}", shopId, shopResponse.getError());
            throw new JsonResponseException(500, shopResponse.getError());
        }

        return shopResponse.getResult();

    }

    private Boolean updateShopTags(Long shopId, Map<String, String> tags) {
        Response<Boolean> updateRes = adminShopWriteService.updateTags(shopId, tags);
        if (!updateRes.isSuccess()) {
            log.error("fail update  shop  id: ({}) tags: {},error:{}", shopId, tags, updateRes.getError());
            throw new JsonResponseException(500, updateRes.getError());
        }

        return updateRes.getResult();
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
     * 设置店铺(经销商)上级信息
     *
     * @param shop      店铺基本信息
     * @param shopExtra 店铺extra信息
     */
    private void setShopParentInfo(Shop shop, VegaShopExtra shopExtra) {
        // 二级经销商, 设置pid
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            Response<Shop> shopResponse = shopReadService.findByName(shopExtra.getShopParentName());
            if (!shopResponse.isSuccess()) {
                log.error("failed to create shop : ({}), cause parent shop shopName = ({}) not exists.", shop, shopExtra.getShopParentName());
                throw new JsonResponseException(500, "parent.shop.not.exists");
            }
            shopExtra.setShopPid(shopResponse.getResult().getId());
            shopExtra.setShopParentName(shopResponse.getResult().getName());
        }
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
        user.setStatus(UserStatus.NORMAL.value());
        user.setName(userName);
        user.setPassword(password);
        user.setMobile(mobile);

        // 一级经销商角色、类型
        if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            user.setType(UserType.NORMAL.value());
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.DEALER_FIRST.name()));
        }
        // 二级经销商角色、类型
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            user.setType(UserType.NORMAL.value());
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.DEALER_SECOND.name()));
        }
        // 供应商角色、类型
        if (ShopTypeHelper.isSupplierShop(shop.getType())) {
            user.setType(UserType.NORMAL.value());
            user.setRoles(Arrays.asList(UserRole.BUYER.name(), UserRole.SELLER.name(), VegaUserRole.SUPPLIER.name()));
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
        if (!ShopTypeHelper.isSupplierShop(shop.getType())) {
            if (shopExtra.getIsCreditAvailable()) {
                shopExtra.setAvailableCredit(shopExtra.getTotalCredit());
            } else {
                // 额度不可用, 总额度为空, 可用额度为空
                shopExtra.setTotalCredit(null);
                shopExtra.setAvailableCredit(null);
            }
        }

        // 默认采购折扣
        if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
            shopExtra.setPurchaseDiscount(DefaultDiscount.NOT_FIND_DISCOUNT);
        }
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
            if (ShopTypeHelper.isFirstDealerShop(shop.getType())) {
                rankDiscountMap.put(String.valueOf(DefaultId.SECOND_SHOP_RANK_ID), DefaultDiscount.MEMBER_RANK_DISCOUNT);
            }
            shopExtra.setMemberDiscount(rankDiscountMap);
        }

        setShopParentInfo(shop, shopExtra);

        return shopExtra;
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

    private List<SmsNode> getSmsNodeFromData(String data) {
        try {
            return JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(List.class, SmsNode.class));
        } catch (Exception e) {
            log.error("fail to get sms node info from data={},cause:{}", data, Throwables.getStackTraceAsString(e));
        }
        throw new JsonResponseException("get.sms.node.info.fail");
    }


    @RequestMapping(value = "/stock-manage", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<VegaItemStockDto> pagingShopItemsByShopId(@RequestParam(value = "itemId", required = false) Long itemId,
                                                            @RequestParam(value = "itemName", required = false) String itemName,
                                                            @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                            @RequestParam(required = true) Long shopId) {

        Response<Paging<ShopItem>> findResp = shopItemReadService.findBy(shopId, itemId, itemName, pageNo, pageSize);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop items by shopId={},itemId={},itemName={},pageNo={},pageSize={},cause:{}",
                    shopId, itemId, itemName, pageNo, pageSize, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        List<ShopItem> shopItems = findResp.getResult().getData();
        Long total =findResp.getResult().getTotal();
        List<VegaItemStockDto> vegaItemStockDtos = Lists.newArrayList();
        for (ShopItem shopItem : shopItems) {
            VegaItemStockDto vegaItemStockDto =new VegaItemStockDto();
            vegaItemStockDto.setShopItem(shopItem);
            Response<List<RichShopSku>> response = shopSkuReadService.findShopSkuDetail(shopId, shopItem.getItemId());
            if (!findResp.isSuccess()) {
                log.error("fail to find shop sku detail by shopId={},itemId={},cause:{}",
                        shopId, itemId, findResp.getError());
                throw new JsonResponseException(response.getError());
            }
            List<RichShopSku> richShopSkus = response.getResult();
            Integer stockQuantity =0;
            for (RichShopSku richShopSku : richShopSkus){
                stockQuantity+=richShopSku.getShopSku().getStockQuantity();
            }
            vegaItemStockDto.setItemStockQuantity(stockQuantity);
            vegaItemStockDtos.add(vegaItemStockDto);
        }
        Paging<VegaItemStockDto> paging =new Paging<VegaItemStockDto>(total,vegaItemStockDtos);
        return paging;

    }


    @RequestMapping(value = "/shop-item/{itemId}/skus", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RichShopSku> findShopSkuDetail(@PathVariable("itemId") Long itemId,@RequestParam(required = true) Long shopId) {
        Response<List<RichShopSku>> findResp = shopSkuReadService.findShopSkuDetail(shopId, itemId);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop sku detail by shopId={},itemId={},cause:{}",
                    shopId, itemId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    /**
     * 一级经销商上架商品手工同步接口,根据类目授权自动进行商品信息更新
     * 考虑性能问题,暂时不开放批量店铺进行同步
     * 后台异步操作,直接返回true
     *
     * @param shopId 店铺ID
     * @return true
     */
    @RequestMapping(value = "/shop-item/{shopId}/sync", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean firstDealerShopItemSync(@PathVariable("shopId") Long shopId) {
        eventBus.post(ShopCategoryAuthEvent.from(shopId));//根据类目授权更新经销商商品信息
        return Boolean.TRUE;
    }

    private Boolean canNotUnFrozeShop (Long shopId) {
        Response<Shop> shopResp = shopReadService.findById(shopId);
        if (!shopResp.isSuccess()) {
            log.error("find shop by id:{} fail, cause:{}", shopId, shopResp.getError());
            throw new JsonResponseException(shopResp.getError());
        }
        if (!ShopTypeHelper.isFirstDealerShop(shopResp.getResult().getType())) {
            return Boolean.FALSE;
        }
        if (hasCreateDeliveryScope(shopId) && hasCreateCategoryAuth(shopId)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    private Boolean hasCreateDeliveryScope (Long shopId) {

        Response<Optional<DeliveryScope>> response = deliveryScopeReadService.findDeliveryScopeByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find delivery scope by shopId:{} fail, cause:{}", shopId, response.getError());
            return Boolean.FALSE;
        }
        if (response.getResult().isPresent() && Arguments.notNull(response.getResult().get())
                && !Strings.isNullOrEmpty(response.getResult().get().getScope())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    private Boolean hasCreateCategoryAuth (Long shopId) {

        Response<Optional<CategoryAuthe>> response = categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!response.isSuccess()) {
            log.error("find category auth by shopId:{} fail, cause:{}", shopId, response.getError());
            return Boolean.FALSE;
        }
        if (response.getResult().isPresent() && Arguments.notNull(response.getResult().get())
                && !Strings.isNullOrEmpty(response.getResult().get().getCategoryDiscountList())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     *
     * 根据店铺Id删除供货区域信息
     *
     * @param shopId 店铺Id
     */
    public void deleteFirstShopDeliveryScope(Long shopId){
        Response<Optional<DeliveryScope>> readScopeResp = deliveryScopeReadService.findDeliveryScopeByShopId(shopId);
        if (!readScopeResp.isSuccess()) {
            log.error("read scope fail, shopId:{}, cause:{}", shopId, readScopeResp.getError());
            return;
        }
        if(readScopeResp.getResult().isPresent()) {
            //失效店铺的供货区域信息
            eventBus.post(DeliveryScopeUpdateEvent.from(shopId));
            //删除供货区域
            Response<Boolean> resp = deliveryScopeWriteService.deleteDeliveryScopeById(readScopeResp.getResult().get().getId());
            if (!resp.isSuccess()) {
                log.error("delete scope fail, shopId:{}, cause:{}", shopId, resp.getError());
                return;
            }
        } else {
            log.debug("delivery scope is empty, shopId:{}", shopId);
            return;
        }
    }

}
