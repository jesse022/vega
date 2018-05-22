package com.sanlux.web.front.controller.user;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.sanlux.common.enums.ServiceManagerType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.dto.UserDetailPageDto;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.dto.criteria.ShopUserCriteria;
import com.sanlux.user.dto.scope.ShopUserDto;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.model.ShopUserExtras;
import com.sanlux.user.service.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Created by liangfujie on 16/8/9
 */
@RestController
@Slf4j
@RequestMapping("/api/shop/user")
public class ShopUsers {

    @RpcConsumer
    private ShopUserReadService shopUserReadService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;
    @RpcConsumer
    private UserRankWriteService userRankWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;
    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private ShopUserExtrasReadService shopUserExtrasReadService;
    @RpcConsumer
    private UserRankReadService userRankReadService;
    @RpcConsumer
    private ServiceManagerUserReadService serviceManagerUserReadService;

    /**
     * 添加新的会员到经销商用户表
     *
     * @param shopUserDto 专属会员信息
     * @return 是否添加成功
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean addShopUser(@RequestBody ShopUserDto shopUserDto) {
        if ( Arguments.isNull(shopUserDto.getMobile()) ||
                Arguments.isNull(shopUserDto.getShopUser()) ||
                Arguments.isNull(shopUserDto.getShopUserExtras()) ) {
            log.error("failed to add shop user, cause mobile or shopUser or ShopUserExtra is null");
            throw new JsonResponseException(500, "shop.user.add.fail");
        }
        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();
        shopUserDto.setShopId(shop.getId());
        shopUserDto.setShopName(shop.getName());

        Response<Boolean> response = shopUserWriteService.addShopUser(shopUserDto);
        if (!response.isSuccess()) {
            log.error("add shop user fail,mobile{},shopId{},shopName{},error{}", shopUserDto.getMobile(), shop.getId(), shop.getName(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/add-by-h5", method = RequestMethod.POST)
    public Boolean addServiceManagerUser(@RequestParam("mobile") String mobile,
                                         @RequestParam("userId") Long userId) {
        Response<Shop> resp = shopReadService.findByUserId(userId);
        if (!resp.isSuccess() || Arguments.isNull(resp.getResult())) {
            log.error(" shop find by userId fail,userId={},error={}", UserUtil.getUserId(), resp.getError());
            log.warn("The shop adds members through two-dimensional code fail, because:[shop find by userId fail] ");
            return Boolean.FALSE;
        }
        Shop shop = resp.getResult();
        ShopUser shopUser = new ShopUser();
        ShopUserExtras shopUserExtras = new ShopUserExtras();
        ShopUserDto shopUserDto = new ShopUserDto();
        shopUserDto.setShopId(shop.getId());
        shopUserDto.setShopName(shop.getName());
        shopUserDto.setMobile(mobile);

        shopUser.setMobile(mobile);
        shopUserDto.setShopUser(shopUser);

        shopUserExtras.setMobile(mobile);
        shopUserDto.setShopUserExtras(shopUserExtras);

        //检查用户是否已添加
        if (!checkShopUserExists(mobile, shop)) {
            return Boolean.FALSE;
        }

        Response<Boolean> response = shopUserWriteService.addShopUser(shopUserDto);
        if (!response.isSuccess()) {
            log.error("add shop user fail,mobile={},shopId={},shopName={},error{}", shopUserDto.getMobile(), shop.getId(), shop.getName(), response.getError());
            log.warn("The shop adds members through two-dimensional code fail, because:[{}] ", response.getError());
            return Boolean.FALSE;
        }
        return response.getResult();
    }

    /**
     * 会员信息修改
     *
     * @param shopUserDto 专属会员信息
     * @return 是否成功
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateShopUser(@RequestBody ShopUserDto shopUserDto) {
        if ( Arguments.isNull(shopUserDto.getMobile()) ||
                Arguments.isNull(shopUserDto.getShopUser()) ||
                Arguments.isNull(shopUserDto.getShopUserExtras()) ) {
            log.error("failed to add shop user, cause mobile or shopUser or ShopUserExtra is null");
            throw new JsonResponseException(500, "shop.user.add.fail");
        }
        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();
        ShopUser shopUser = findShopUserByMobile(shopUserDto.getMobile());
        if (!Objects.equal(shopUser.getShopId(), shop.getId())) {
            log.error(" this shop user(userId = {}) not belong to this shop(shopId ={}),userId{},error{}", shopUser.getUserId(), shop.getId());
            throw new JsonResponseException("shop.user.not.belong.to.dealer");
        }
        shopUserDto.setShopId(shop.getId());
        shopUserDto.setShopName(shop.getName());
        shopUserDto.getShopUser().setId(shopUser.getId());

        Response<Boolean> response = shopUserWriteService.updateShopUser(shopUserDto);
        if (!response.isSuccess()) {
            log.error("add shop user fail,mobile{},shopId{},shopName{},error{}", shopUserDto.getMobile(), shop.getId(), shop.getName(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/find", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShopUserDto> getShopUser(@RequestParam("userId") Long userId) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        ShopUserDto shopUserDto =new ShopUserDto();

        // 会员基础信息
        Response<Optional<ShopUser>> resp = shopUserReadService.findShopUserByUserId(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop user by userId = {}, cause : {}", userId, resp.getError());
            throw new JsonResponseException("shop.user.find.fail");
        }
        if (!resp.getResult().isPresent()) {
            log.error("find shop user fail,userId:{}", userId);
            return Response.fail("shop.user.find.fail");
        }
        if (Arguments.isNull(shopId) || !Objects.equal(shopId, resp.getResult().get().getShopId())) {
            log.error(" this shop user(userId = {}) not belong to this shop(shopId ={}),userId{},error{}", userId, shopId);
            throw new JsonResponseException("shop.user.not.belong.to.dealer");
        }

        shopUserDto.setShopUser(resp.getResult().get());

        // 会员扩展信息
        Response<Optional<ShopUserExtras>> response = shopUserExtrasReadService.findByUserId(userId);
        if (response.isSuccess()) {
            if (response.getResult().isPresent()) {
                shopUserDto.setShopUserExtras(response.getResult().get());
            } else {
                log.error("find shop user extra fail,userId:{}", userId);
            }
        } else {
            log.error("failed to find shop user extra by userId = {}, cause : {}", userId, response.getError());
            // 不抛异常
        }

        // 会员等级信息
        UserRank userRank = findShopUserRand(userId);
        if (! Arguments.isNull(userRank)) {
            shopUserDto.setRankId(userRank.getRankId());
            shopUserDto.setRankName(userRank.getRankName());
        }

        // 会员所属业务经理信息
        ServiceManagerUser serviceManagerUser = findShopUserServiceManager(userId);
        if (! Arguments.isNull(serviceManagerUser)) {
            shopUserDto.setServiceManagerId(serviceManagerUser.getServiceManagerId());
            shopUserDto.setServiceManagerName(serviceManagerUser.getServiceManagerName());
        }

        return Response.ok(shopUserDto);

    }

    /**
     * 根据手机号判断会员是否已添加或用户是否合法
     *
     * @param mobile 用户手机号
     * @return 是否存在
     */
    @RequestMapping(value = "/exists", method = RequestMethod.POST)
    public Boolean checkShopUserExists(@RequestParam("mobile") String mobile) {
        // 1.判断用户是否合法
        Response<User> response = userReadService.findBy(mobile, LoginType.MOBILE);
        if (!response.isSuccess()) {
            log.error("find user by mobile failed, mobile{},cause{}", mobile, response.getError());
            throw new JsonResponseException("user.not.exist.fail");
        }
        User user = response.getResult();

        if (!Arguments.notNull(user)) {
            throw new JsonResponseException("user.not.exist.fail");
        }
        List<String> roles = user.getRoles();
        //判断用户是否为普通用户
        if (!Objects.equal(user.getType(), 2)) {
            throw new JsonResponseException("shop.user.not.ordinary.fail");
        }
        if(!roles.contains(VegaUserRole.SUPPLIER.name())) {
            //买家是供应商时作为普通用户处理,不做判断
            if (!(Objects.equal(roles.size(), 1) &&
                    roles.get(0).equals(UserRole.BUYER.name()))) {
                throw new JsonResponseException("shop.user.not.ordinary.fail");
            }
        }



        // 2.判断用户是否已被添加
        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();
        //检查用户是否被经销商添加
        Response<ShopUser> shopUserResponse = shopUserReadService.findByMobile(mobile);
        if (shopUserResponse.isSuccess()) {
            ShopUser shopUser = shopUserResponse.getResult();
            if ( Objects.equal(shopUser.getShopId(), shop.getId())) {
                //被自己添加的提示
                log.error("add shop user fail because shop user exist ,mobile{},shopId{},shopName{},error{}", mobile, shop.getId(), shop.getName(), "shop user exist");
                throw new JsonResponseException("shop.user.exist.fail");
            } else {
                //被其他经销商添加并提示该经销商信息
                log.error("add shop user fail because shop user exist ,mobile{},shopId{},shopName{},error{}", mobile, shop.getId(), shop.getName(), "shop user exist");
                throw new InvalidException(500, "{0}.shop.user.exist.fail", shopUser.getShopName());
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 更新用户的等级信息
     *
     * @param userId 用户ID
     * @param rankId 欲更新的等级ID
     * @return 是否更新成功
     */
    @RequestMapping(value = "/update-rank", method = RequestMethod.PUT)
    public Boolean updateUserRank(@RequestParam("userId") Long userId, @RequestParam("rankId") Long rankId) {
        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop  find fail,operateId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();
        Response<Boolean> response = userRankWriteService.updateUserRank(userId, rankId, shop.getId(), shop.getName());
        if (!response.isSuccess()) {
            log.error("update shop user rank fail,userId{},rankId{},operateId{},operateName{},error{}", userId, rankId, shop.getId(), shop.getName(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }

    /**
     * 新增或更新专属会员信用额度
     *
     * @param userId            用户ID
     * @param isCreditAvailable 信用额度是否可用
     * @param totalCredit       信用额度(可用于支付)
     * @param creditPaymentDays 账龄(还款日期)
     * @return 是否更新成功
    */
    @RequestMapping(value = "/update-resume", method = RequestMethod.PUT)
    public Response<Boolean> updateUserResume(@RequestParam("userId") Long userId,
                                              @RequestParam("isCreditAvailable") Boolean isCreditAvailable,
                                              @RequestParam("totalCredit") Long totalCredit,
                                              @RequestParam("creditPaymentDays") Integer creditPaymentDays) {
        Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(userId);
        if (!shopUserResponse.isSuccess()) {
            log.error("find shop user fail,userId:{},cause:{}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        if (!shopUserResponse.getResult().isPresent()) {
            log.error("find shop user fail,userId:{}", userId);
            return Response.fail("shop.user.find.fail");
        }
        ShopUser originalShopUser = shopUserResponse.getResult().get();
        User user = findUserById(userId);

        CreditAlterResume resume = null;

        if (isCreditAvailable) {
            // 更新信用额度履历信息
            ParanaUser currentUser = UserUtil.getCurrentUser();
            if (Arguments.isNull(originalShopUser.getTotalCredit())) {
                // 新增
                resume = createCreditAlterResume(totalCredit, user);
                originalShopUser.setAvailableCredit(totalCredit);
            } else {
                // 修改
                if (!Objects.equal(totalCredit, originalShopUser.getTotalCredit())) {
                    resume = generateUpdateResume(currentUser, totalCredit, user, originalShopUser);
                    originalShopUser.setAvailableCredit(resume.getAvailableCredit());
                }
            }
        }
        originalShopUser.setIsCreditAvailable(isCreditAvailable);
        originalShopUser.setTotalCredit(totalCredit);
        originalShopUser.setCreditPaymentDays(creditPaymentDays);

        if (!Arguments.isNull(resume)) {
            Response<Long> resp = creditAlterResumeWriteService.create(resume);
            if (!resp.isSuccess()) {
                log.error("failed to create credit alter resume by user = ({}), resume = {} " +
                        "cause : {}", user, resume, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
        }

        Response<Boolean> booleanResponse = shopUserWriteService.AddOrUpdateShopUserCreditByUserId(userId, originalShopUser);
        if (!booleanResponse.isSuccess()) {
            log.error("failed to add or update shop user credit by userID = ({}), isCreditAvailable = {}, " +
                            "totalCredit = {}, availableCredit = {}, creditPaymentDays = {}, cause : {}",
                    userId, isCreditAvailable, totalCredit, totalCredit, creditPaymentDays, booleanResponse.getError());
            throw new JsonResponseException(500, booleanResponse.getError());
        }

        return Response.ok(booleanResponse.getResult());
    }


    /**
     * 经销商用户详细信息分页
     *
     * @param shopUserCriteria 查询条件
     * @return 用户详细信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDetailPageDto pagingShopUsers(ShopUserCriteria shopUserCriteria) {
        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop  find fail,operateId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();
        Response<VegaShopExtra> res = vegaShopReadService.findVegaShopExtraByShopId(shop.getId());
        if (!res.isSuccess()) {
            log.error(" shop extra find fail,operateId{},error{}", shop.getId(), res.getError());
            throw new JsonResponseException(resp.getError());
        }
        VegaShopExtra shopExtra = res.getResult();
        Integer shopNowDiscount = shopExtra.getPurchaseDiscount();//经销商得默认折扣
        shopUserCriteria.setShopId(shop.getId());
        Response<UserDetailPageDto> response = shopUserReadService.pagingShopUser(shopUserCriteria, shopNowDiscount);
        if (!response.isSuccess()) {
            log.error(" shop user paging fail,shopUserCriteria{},error{}", shopUserCriteria, response.getError());
            throw new JsonResponseException("shop.user.paging.fail");
        }
        return response.getResult();

    }

    /**
     * 经销商设定用户特定折扣,暂时没有使用此方法了,先保留
     *
     * @param userId   用户Id
     * @param discount 折扣值
     * @return 是否设置成功
     */
    @RequestMapping(value = "/update-discount", method = RequestMethod.PUT)
    public Boolean updateShopUserDiscount(@RequestParam("userId") Long userId, @RequestParam("discount") Integer discount) {

        Response<Shop> resp = shopReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" shop  find fail,operateId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Shop shop = resp.getResult();

        Response<Boolean> response = shopUserWriteService.updateShopUserDiscount(shop.getId(), userId, discount);
        if (!response.isSuccess()) {
            log.error("update shop user discount fail,shopId{},userId{},discount{},error{}", shop.getId(), userId, discount, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 根据指定的ID删除会员
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @RequestMapping(value = "/delete", method = RequestMethod.PUT)
    public Boolean deleteShopUser(@RequestParam("userId") Long userId) {
        Response<Boolean> booleanResponse = creditAlterResumeReadService.checkWaitRepaymentCountByUserId(userId);
        if (booleanResponse.isSuccess() && booleanResponse.getResult()) {
            log.info("delete shop user fail,userId{},because user credit has wait repayment", userId);
            throw new JsonResponseException("shop.user.credit.has.wait.repayment.delete.fail");
        }
        Response<Boolean> response = shopUserWriteService.deleteShopUserById(userId);
        if (!response.isSuccess()) {
            log.error("delete shop user  fail,userId{},error{}", userId, response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();
    }

    /**
     * 同步经销商用户信息,让用户表和经销商用户表信息同步
     *
     * @param userId 用户ID
     * @return 同步是否成功
     */

    @RequestMapping(value = "/refresh", method = RequestMethod.PUT)
    public Boolean refreshShopUser(@RequestParam("userId") Long userId) {
        Response<Boolean> response = shopUserWriteService.refreshShopUserByUserId(userId);
        if (!response.isSuccess()) {
            log.error("refresh shop user  fail,userId{},error{}", userId, response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();

    }

    /**
     * 新增时封装额度履历信息
     *
     * @param totalCredit 总额度
     * @param user        用户信息
     * @return 履历信息
     */
    private CreditAlterResume createCreditAlterResume(Long totalCredit, User user) {
        CreditAlterResume resume = new CreditAlterResume();
        ParanaUser currentUser = UserUtil.getCurrentUser();
        // 变更之后的值不能小于0
        if (totalCredit < 0) {
            log.error("failed to create shop, cause shop credit could not less than zero");
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        resume.setUserId(user.getId());
        resume.setUserName(user.getName());
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
     * 修改是封装额度履历信息
     *
     * @param currentUser       当前用户
     * @param nowTotalCredit    编辑修改之后的总额度
     * @param user              专属会员信息
     * @param originalShopUser  编辑前的专属会员信息
     * @return 履历信息
     */
    private CreditAlterResume generateUpdateResume(ParanaUser currentUser,
                                                   Long nowTotalCredit,
                                                   User user,
                                                   ShopUser originalShopUser) {
        // 原始总额度
        Long orgTotalCredit = originalShopUser.getTotalCredit() == null ? 0L : originalShopUser.getTotalCredit();
        // 原始可用额度
        Long orgAvailableCredit = originalShopUser.getAvailableCredit() == null ? 0L : originalShopUser.getAvailableCredit();
        // 编辑修改之后的可用额度
        Long nowAvailableCredit = 0L;

        CreditAlterResume resume = new CreditAlterResume();
        resume.setUserId(user.getId());
        resume.setUserName(user.getName());
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
                resume.setAlterType(CreditAlterType.ADMIN_REDUCE.value());
                nowAvailableCredit = orgAvailableCredit + alterValue;
            }
        }

        resume.setAlterValue(alterValue);
        resume.setTotalCredit(nowTotalCredit);
        resume.setAvailableCredit(nowAvailableCredit);
        resume.setLastCredit(orgAvailableCredit);//后来增加
        resume.setNewestCredit(nowAvailableCredit);//后来增加

        return resume;
    }

    /**
     * 查找用户信息
     *
     * @param userId 用户ID
     * @return       用户信息
     */
    private User findUserById(Long userId) {
        Response<User> resp = userReadService.findById(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
            throw new JsonResponseException(500, "user.id.null");
        }
        return resp.getResult();
    }

    /**
     * 根据手机号码查找会员基本信息信息
     *
     * @param mobile 手机号码
     * @return 会员信息
     */
    private ShopUser findShopUserByMobile(String mobile) {
        Response<ShopUser> resp = shopUserReadService.findByMobile(mobile);
        if (!resp.isSuccess()) {
            log.error("failed to find shop user by mobile = {}, cause : {}", mobile, resp.getError());
            throw new JsonResponseException("shop.user.find.fail");
        }
        return resp.getResult();
    }

    /**
     * 根据用户Id获取会员等级信息
     * @param userId 会员用户Id
     * @return 会员等级信息
     */
    private UserRank findShopUserRand(Long userId) {
        Response<UserRank> userRankRes = userRankReadService.findUserRankByUserId(userId);
        if (!userRankRes.isSuccess()) {
            log.error("find shop user rank by user id: ({}) fail ,error:{}", userId, userRankRes.getError());
            return null;
        }
       return userRankRes.getResult();
    }

    /**
     * 根据用户Id获取会员所属业务经理信息
     * @param userId 会员用户Id
     * @return 所属业务经理信息
     */
    private ServiceManagerUser findShopUserServiceManager(Long userId) {
        Response<List<ServiceManagerUser>> serviceManagerUserResponse = serviceManagerUserReadService.findByUserId(userId);
        if (!serviceManagerUserResponse.isSuccess()) {
            log.error("find shop user service manager by user id: ({}) fail ,error:{}", userId, serviceManagerUserResponse.getError());
            return null;
        }
        for (ServiceManagerUser serviceManagerUser : serviceManagerUserResponse.getResult()) {
            if (!Objects.equal(serviceManagerUser.getType(), ServiceManagerType.PLATFORM.value())) {
                return serviceManagerUser;
            }
        }
        return null;
    }

    /**
     * 通过经销商分享的二维码添加会员信息时合法性验证
     *
     * @param mobile 会员手机号
     * @param shop   经销商店铺信息
     * @return 是否
     */
    private Boolean checkShopUserExists(String mobile, Shop shop) {
        // 1.判断用户是否合法
        Response<User> response = userReadService.findBy(mobile, LoginType.MOBILE);
        if (!response.isSuccess()) {
            log.error("find user by mobile failed, mobile={},cause={}", mobile, response.getError());
            log.warn("The shop adds members through two-dimensional code fail, because:[user not exist]");
            return Boolean.FALSE;
        }

        User user = response.getResult();
        if (!Arguments.notNull(user)) {
            log.warn("The shop adds members through two-dimensional code fail, because:[user not exist]");
            return Boolean.FALSE;
        }
        List<String> roles = user.getRoles();
        //判断用户是否为普通用户
        if (!Objects.equal(user.getType(), 2)) {
            log.warn("The shop adds members through two-dimensional code fail, because:[shop.user.not.ordinary.fail]");
            return Boolean.FALSE;
        }
        if(!roles.contains(VegaUserRole.SUPPLIER.name())) {
            //买家是供应商时作为普通用户处理,不做判断
            if (!(Objects.equal(roles.size(), 1) &&
                    roles.get(0).equals(UserRole.BUYER.name()))) {
                log.warn("The shop adds members through two-dimensional code fail, because:[shop.user.not.ordinary.fail]");
                return Boolean.FALSE;
            }
        }

        // 2.判断用户是否已被添加
        Response<ShopUser> shopUserResponse = shopUserReadService.findByMobile(mobile);
        if (shopUserResponse.isSuccess()) {
            ShopUser shopUser = shopUserResponse.getResult();
            if ( Objects.equal(shopUser.getShopId(), shop.getId())) {
                //被自己添加的提示
                log.error("add shop user fail because shop user exist ,mobile={},shopId={},shopName={},error={}", mobile, shop.getId(), shop.getName(), "shop user exist");
                log.warn("The shop adds members through two-dimensional code fail, because:[Members already added] ");
                return Boolean.FALSE;
            } else {
                //被其他经销商添加并提示该经销商信息
                log.error("add shop user fail because shop user exist ,mobile={},shopId={},shopName={},error{}", mobile, shop.getId(), shop.getName(), "shop user exist");
                log.warn("The shop adds members through two-dimensional code fail, because:[Members have been other shop:{} added] ", shopUser.getShopName());
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }


}
