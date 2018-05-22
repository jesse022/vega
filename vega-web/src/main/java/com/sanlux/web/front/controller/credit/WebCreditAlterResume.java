package com.sanlux.web.front.controller.credit;

import com.google.common.base.*;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.helper.ShopHelper;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.CreditAlterStatus;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.enums.CreditRepaymentStatus;
import com.sanlux.shop.enums.CreditRepaymentType;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.CreditRepaymentResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
import com.sanlux.shop.service.CreditRepaymentResumeReadService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.service.ShopUserReadService;
import com.sanlux.user.service.ShopUserWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.msg.exception.MsgException;
import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.MsgContext;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created Date : 16/8/17
 * Author : wujianwei
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/credit")
public class WebCreditAlterResume {

    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;
    @RpcConsumer
    private CreditRepaymentResumeReadService creditRepaymentResumeReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @Autowired
    private MsgService msgService;
    @RpcConsumer
    private PaymentReadService paymentReadService;
    @RpcConsumer
    private ShopUserReadService shopUserReadService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;

    /**
     * 修改经销商/专属会员信用额度
     *
     * @param shopId     店铺(经销商)ID
     * @param userId     用户Id
     * @param alterValue 变更额度
     * @return 修改结果
     */
    @RequestMapping(method = RequestMethod.PUT)
    public Boolean updateCredit(@RequestParam(value = "shopId", required = false) Long shopId,
                                @RequestParam(value = "userId", required = false) Long userId,
                                @RequestParam(value = "alterValue") Long alterValue) {
        if (Arguments.isNull(shopId) && Arguments.isNull(userId)) {
            log.error("failed to update credit by shopId = ({}) or userId = ({}), cause the shopId and userId  all is null.");
            throw new JsonResponseException(500, "shopId.and.userId.all.is.null");
        }
        VegaShopExtra shopExtra = null;
        ShopUser shopUser = null;
        if (!Arguments.isNull(shopId)) {
            // 店铺信息
            shopExtra = getVegaShopExtra(shopId);
        } else {
            // 专属会员信息
            shopUser = getShopUserInfo(userId);
        }

        // 操作人信息
        ParanaUser user = UserUtil.getCurrentUser();
        Integer alterType = alterValue >= 0 ? CreditAlterType.ADMIN_ADD.value() : CreditAlterType.ADMIN_REDUCE.value();

        CreditAlterResume resume = null;
        if (!Arguments.isNull(shopExtra)) {
            resume = generateCreditAlterResume(user, shopExtra, null, alterType, alterValue);

            // 所属店铺更新额度信息
            VegaShopExtra toUpdateVegaShopExtra = new VegaShopExtra();
            Long originalDebtValue = shopExtra.getTotalCredit() - shopExtra.getAvailableCredit();
            BeanMapper.copy(shopExtra, toUpdateVegaShopExtra);
            // 新的总额度
            toUpdateVegaShopExtra.setTotalCredit(resume.getNewestCredit());
            // 可用额度如果是增则增, 如果是减则根据当前可用额度判断是否需要减少到当前最高额度
            Long nowAvailable = toUpdateVegaShopExtra.getAvailableCredit();
            Long newAvailable = nowAvailable + (alterValue < 0 ? 0 : alterValue);
            Long totalCredit = toUpdateVegaShopExtra.getTotalCredit();
            if (newAvailable > totalCredit) {
                // 总金额减去原始欠款金额
                newAvailable = totalCredit - originalDebtValue;
            }
            toUpdateVegaShopExtra.setAvailableCredit(newAvailable);

            resume.setAvailableCredit(toUpdateVegaShopExtra.getAvailableCredit());
            resume.setTotalCredit(toUpdateVegaShopExtra.getTotalCredit());

            Response<Boolean> resp = creditAlterResumeWriteService.create(resume, toUpdateVegaShopExtra);
            if (!resp.isSuccess()) {
                log.error("failed to update credit by shopId= {}, alterValue = {}. cause : {}",
                        shopId, alterValue, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
            return resp.getResult();
        }

        if (!Arguments.isNull(shopUser)) {
            resume = generateCreditAlterResume(user, null, shopUser, alterType, alterValue);
            // 所属专属会员更新额度信息
            ShopUser toUpdateShopUser = new ShopUser();
            Long originalDebtValue = shopUser.getTotalCredit() - shopUser.getAvailableCredit();
            BeanMapper.copy(shopUser, toUpdateShopUser);
            // 新的总额度
            toUpdateShopUser.setTotalCredit(resume.getNewestCredit());
            // 可用额度如果是增则增, 如果是减则根据当前可用额度判断是否需要减少到当前最高额度
            Long nowAvailable = toUpdateShopUser.getAvailableCredit();
            Long newAvailable = nowAvailable + (alterValue < 0 ? 0 : alterValue);
            Long totalCredit = toUpdateShopUser.getTotalCredit();
            if (newAvailable > totalCredit) {
                // 总金额减去原始欠款金额
                newAvailable = totalCredit - originalDebtValue;
            }
            toUpdateShopUser.setAvailableCredit(newAvailable);

            resume.setAvailableCredit(toUpdateShopUser.getAvailableCredit());
            resume.setTotalCredit(toUpdateShopUser.getTotalCredit());

            Response<Long> resp = creditAlterResumeWriteService.create(resume);
            if (!resp.isSuccess()) {
                log.error("failed to update credit by userId= {}, alterValue = {}. cause : {}",
                        userId, alterValue, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }

            Response<Boolean> booleanResponse = shopUserWriteService.updateShopUserCreditByUserId(toUpdateShopUser.getUserId(),
                    toUpdateShopUser.getAvailableCredit(), toUpdateShopUser.getTotalCredit());
            if (!booleanResponse.isSuccess()) {
                log.error("failed to update shop user available credit by userID = ({}), availableCredit = {}, " +
                        "cause : {}", toUpdateShopUser.getUserId(), toUpdateShopUser.getAvailableCredit(), booleanResponse.getError());
                throw new JsonResponseException(500, booleanResponse.getError());
            }
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 计算罚息
     *
     * @param debtAmount          欠款金额
     * @param interest            罚款利息
     * @param shouldRepaymentDate 应还款时间
     * @param actualRepaymentDate 实际还款时间
     * @return 值...
     */
    @RequestMapping(value = "/fine-amount", method = RequestMethod.GET)
    public Long calculateFineAmount(@RequestParam("debtAmount") Long debtAmount,
                                    @RequestParam("interest") Integer interest,
                                    @RequestParam("shouldRepaymentDate") String shouldRepaymentDate,
                                    @RequestParam("actualRepaymentDate") String actualRepaymentDate) {
        Date shouldDate = DateTime.parse(shouldRepaymentDate).toDate();
        Date actualDate = DateTime.parse(actualRepaymentDate).toDate();
        return ShopHelper.calculateFineAmount(debtAmount, shouldDate, actualDate, interest);
    }

    /**
     * 根据ID查对应信用额度修改信息
     *
     * @param id 额度履历ID
     * @return 信息
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public CreditAlterResume findById(@PathVariable(value = "id") Long id) {
        Response<CreditAlterResume> readScopeResp = creditAlterResumeReadService.findById(id);
        if (!readScopeResp.isSuccess()) {
            log.error("failed to find credit resume by id = ({}), cause: {}", id, readScopeResp.getError());
            throw new JsonResponseException(readScopeResp.getError());
        }
        return readScopeResp.getResult();
    }

    /**
     * 修改信用额度状态, (可用、不可用)
     *
     * @param shopId      店铺ID
     * @param isAvailable 是否可用
     * @return 修改结果
     */
    @RequestMapping(value = "/credit_available/{shopId}", method = RequestMethod.PUT)
    public Boolean changeCreditStatus(@PathVariable(value = "shopId") Long shopId,
                                      @RequestParam(value = "isAvailable") Boolean isAvailable) {
        VegaShop vegaShop = findVegaShopByShopId(shopId);
        VegaShopExtra shopExtra = vegaShop.getShopExtra();
        // 账龄是否已添加
        if (shopExtra.getCreditPaymentDays() == null) {
            log.error("failed to update credit by shopId = ({}), cause the credit payment days is null.");
            throw new JsonResponseException(500, "credit.payment.days.is.null");
        }
        Response<Boolean> resp = vegaShopWriteService.changeCreditStatusByShopId(shopId, isAvailable);
        if (!resp.isSuccess()) {
            log.error("failed to change isCreditAvailable = ({}) by shopId = {}, cause : {}",
                    isAvailable, shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 修改专属会员信用额度状态, (可用、不可用)
     *
     * @param userId      用户ID
     * @param isAvailable 是否可用
     * @return 修改结果
     */
    @RequestMapping(value = "/credit_available/user/{userId}", method = RequestMethod.PUT)
    public Boolean changeCreditStatusByShopUser(@PathVariable(value = "userId") Long userId,
                                      @RequestParam(value = "isAvailable") Boolean isAvailable) {
        Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(userId);
        if (!shopUserResponse.isSuccess()) {
            log.error("failed to find credit by userId = {}, cause : {}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
        if (!shopUserOptional.isPresent()) {
            log.error("failed to find credit by userId = {}, cause : {}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        // 账龄是否已添加
        if (shopUserOptional.get().getCreditPaymentDays() == null) {
            log.error("failed to update credit by userId = ({}), cause the credit payment days is null.");
            throw new JsonResponseException(500, "shop.user.credit.payment.days.is.null");
        }
        Response<Boolean> resp = shopUserWriteService.changeCreditStatusByUserId(userId, isAvailable);
        if (!resp.isSuccess()) {
            log.error("failed to change isCreditAvailable = ({}) by userId = {}, cause : {}",
                    isAvailable, userId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 经销商操作还款审核(下级经销商或专属会员还信用额度)
     *
     * @param id             履历ID
     * @param status         转款状态
     * @param repaymentValue 还款金额
     * @param paymentDate    还款时间
     * @param type           还款类型(1:二级经销商还款  2:专属会员还款)
     * @return 操作结果
     */
    @RequestMapping(value = "/add_repayment/{id}", method = RequestMethod.PUT)
    public Boolean repayment(@PathVariable(value = "id") Long id,
                             @RequestParam(value = "status", required = false) Integer status,
                             @RequestParam(value = "repaymentValue", required = false) Long repaymentValue,
                             @RequestParam(value = "paymentDate", required = false) String paymentDate,
                             @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
        // 审核不通过, 只改状态
        if (!isAuditPass(status)) {
            return changeCreditAlterResumeStatus(id, status);
        }
        checkNotNull(repaymentValue, "repayment.value.is.null");
        checkNotNull(paymentDate, "repayment.date.is.null");
        // 履历信息
        CreditAlterResume originalAlterResume = findCreditAlterResumeById(id);

        VegaShop vegaShop = null;
        Shop shop = null;
        User user = null;
        VegaShopExtra originalShopExtra = null;
        ShopUser shopUser = null;

        if (Objects.equal(type, 2)) {
            // 专属会员信息
            shopUser = findShopUserByUserId(originalAlterResume.getUserId());
            Response<User> response = userReadService.findById(originalAlterResume.getUserId());
            if (!response.isSuccess()) {
                log.error("user find by id fail ,userId {}, cause {}", originalAlterResume.getUserId(), response.getError());
                throw new JsonResponseException(500, response.getError());
            }
            user = response.getResult();
            // 判断信用额度是否可用, 需要先将信用额度置为可用才能修改
            if (shopUser == null || !shopUser.getIsCreditAvailable()) {
                log.error("failed to update credit by userId = ({}), cause the credit of this user is not available.");
                throw new JsonResponseException(500, "credit.user.not.available");
            }
        } else {
            // 店铺信息
            vegaShop = findVegaShopByShopId(originalAlterResume.getShopId());
            shop = vegaShop.getShop();
            originalShopExtra = vegaShop.getShopExtra();

            // 判断信用额度是否可用, 需要先将信用额度置为可用才能修改
            if (!originalShopExtra.getIsCreditAvailable()) {
                log.error("failed to update credit by shopId = ({}), cause the credit of this shop is not available.");
                throw new JsonResponseException(500, "credit.not.available");
            }
        }

        // 实际还款日期
        Date actualPaymentDate = DateTime.parse(paymentDate).toDate();
        Long lastDebtAmount = originalAlterResume.getRemainPayment();
        Long lastAvailableCredit = originalAlterResume.getAvailableCredit(); // TODO

        // 1. 根据还款值修改履历信息
        originalAlterResume = updateCreditAlterResume(originalAlterResume, originalShopExtra, shopUser,
                repaymentValue, actualPaymentDate);

        // 2. 记录一条还款履历
        CreditRepaymentResume repaymentResume = createRepaymentResumeObject(originalAlterResume,
                lastDebtAmount, repaymentValue);

        // 3. 更新店铺或专属会员信用额度
        VegaShopExtra toUpdateShopExtra = null;
        ShopUser toUpdateShopUser = null;
        if (Objects.equal(type, 2)) {
            // 专属会员
            toUpdateShopUser = updateShopUserCredit(originalAlterResume);
        } else {
            toUpdateShopExtra = updateShopExtraCredit(originalShopExtra, originalAlterResume);
        }

        // 4. 新增一条运营操作履历, 类型为还款?
        ParanaUser currentUser = UserUtil.getCurrentUser();
        Integer alterType = CreditAlterType.PERSONAL_REPAYMENT.value();
        CreditAlterResume operatedByAdmin = generateCreditAlterRepaymentResume(currentUser, toUpdateShopExtra, toUpdateShopUser,
                alterType, lastAvailableCredit, repaymentValue);

        // 创建和更新信息
        return doCreateRepaymentResume(shop, user, toUpdateShopExtra, toUpdateShopUser, originalAlterResume, operatedByAdmin, repaymentResume);
    }

    /**
     * 更新信用额度履历状态
     *
     * @param id     履历id
     * @param status 履历状态
     * @return 结果
     */
    private Boolean changeCreditAlterResumeStatus(Long id, Integer status) {
        CreditAlterResume resume = new CreditAlterResume();
        resume.setId(id);
        resume.setAlterStatus(status);
        Response<Boolean> resp = creditAlterResumeWriteService.update(resume);
        if (!resp.isSuccess()) {
            log.error("failed to change credit alter resume status by id = {}, resume = {}, cause : {}",
                    id, status, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 判断审核类型
     *
     * @param status 状态
     * @return 结果
     */
    private boolean isAuditPass(Integer status) {
        return status == null;
    }

    /**
     * 创建履历信息
     *
     * @param user       操作人
     * @param shopExtra  操作店铺
     * @param shopUser   操作专属会员
     * @param alterValue 操作值
     * @return 履历信息
     */
    private CreditAlterResume generateCreditAlterResume(ParanaUser user, VegaShopExtra shopExtra, ShopUser shopUser, Integer alterType, Long alterValue) {
        Long orgTotalCredit = null;
        Long orgAvailableCredit = null;
        if (!Arguments.isNull(shopExtra)) {
            orgTotalCredit = shopExtra.getTotalCredit();
            orgAvailableCredit = shopExtra.getAvailableCredit();
        } else {
            orgTotalCredit = shopUser.getTotalCredit();
            orgAvailableCredit = shopUser.getAvailableCredit();
        }
        Long newestCredit = orgTotalCredit + alterValue;
        // 变更之后的值不能小于0
        if (newestCredit < 0) {
            if (!Arguments.isNull(shopExtra)) {
                log.error("failed to change credit by shopId = {}, to update credit value = {}, cause the credit " +
                        "could not less than zero.", shopExtra.getShopId(), alterValue);
            } else {
                log.error("failed to change credit by userId = {}, to update credit value = {}, cause the credit " +
                        "could not less than zero.", shopUser.getUserId(), alterValue);
            }
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();

        if (Arguments.isNull(shopExtra)) {
            // 专属会员
            resume.setUserId(shopUser.getUserId());
            resume.setUserName(shopUser.getUserName());
        } else {
            // 店铺
            resume.setShopId(shopExtra.getShopId());
            resume.setShopName(shopExtra.getShopName());
        }
        resume.setOperateId(user.getId());
        resume.setOperateName(user.getName());

        resume.setLastCredit(orgAvailableCredit);
        resume.setAlterValue(alterValue);
        resume.setAlterType(alterType);
        resume.setCreatedAt(new Date());
        resume.setNewestCredit(newestCredit);

        return resume;
    }

    /**
     * 创建还款履历信息
     *
     * @param user       操作人
     * @param shopExtra  操作店铺
     * @param shopUser   操作专属会员
     * @param alterValue 操作值
     * @return 履历信息
     */
    private CreditAlterResume generateCreditAlterRepaymentResume(ParanaUser user,
                                                                 VegaShopExtra shopExtra,
                                                                 ShopUser shopUser,
                                                                 Integer alterType,
                                                                 Long lastAvailableCredit,
                                                                 Long alterValue) {
        // 总额度不变
        Long totalCredit = 0L;
        if (!Arguments.isNull(shopExtra)) {
            totalCredit = shopExtra.getTotalCredit();
        } else {
            totalCredit = shopUser.getTotalCredit();
        }
        // 变更之后的值不能小于0
        if (lastAvailableCredit < 0) {
            if (Arguments.isNull(shopExtra)) {
                log.error("failed to change credit by userId = {}, to update credit value = {}, cause the credit " +
                        "could not less than zero.", shopUser.getUserId(), alterValue);
            } else {
                log.error("failed to change credit by shopId = {}, to update credit value = {}, cause the credit " +
                        "could not less than zero.", shopExtra.getShopId(), alterValue);
            }
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();

        if (!Arguments.isNull(shopExtra)) {
            resume.setShopId(shopExtra.getShopId());
            resume.setShopName(shopExtra.getShopName());
        } else {
            resume.setUserName(shopUser.getUserName());
            resume.setUserId(shopUser.getUserId());
        }
        resume.setOperateId(user.getId());
        resume.setOperateName(user.getName());

        resume.setLastCredit(lastAvailableCredit);
        resume.setAlterValue(alterValue);
        resume.setAlterType(alterType);
        resume.setCreatedAt(new Date());
        resume.setNewestCredit(lastAvailableCredit);
        resume.setTotalCredit(totalCredit);
        resume.setAvailableCredit(Arguments.isNull(shopExtra) ? shopUser.getAvailableCredit() : shopExtra.getAvailableCredit());

        return resume;
    }

    /**
     * 更新店铺信用额度
     *
     * @param originalAlterResume 信用额度履历
     * @return 更新后的信息
     */
    private VegaShopExtra updateShopExtraCredit(VegaShopExtra orgShopExtra, CreditAlterResume originalAlterResume) {
        VegaShopExtra toUpdateShopExtra = new VegaShopExtra();

        toUpdateShopExtra.setShopId(originalAlterResume.getShopId());
        toUpdateShopExtra.setTotalCredit(originalAlterResume.getTotalCredit());
        toUpdateShopExtra.setAvailableCredit(originalAlterResume.getNewestCredit());

        return toUpdateShopExtra;
    }

    /**
     * 更新专属会员信用额度
     *
     * @param originalAlterResume 信用额度履历
     * @return 更新后的信息
     */
    private ShopUser updateShopUserCredit(CreditAlterResume originalAlterResume) {
        ShopUser toUpdateShopUser = new ShopUser();

        toUpdateShopUser.setUserId(originalAlterResume.getUserId());
        toUpdateShopUser.setTotalCredit(originalAlterResume.getTotalCredit());
        toUpdateShopUser.setAvailableCredit(originalAlterResume.getNewestCredit());
        return toUpdateShopUser;
    }

    /**
     * 创建还款履历信息
     *
     * @param originalAlterResume 信用额度履历信息
     * @param lastDebtAmount      还款额度
     * @param repaymentValue      还款额度
     * @return 还款履历信息
     */
    private CreditRepaymentResume createRepaymentResumeObject(CreditAlterResume originalAlterResume,
                                                              Long lastDebtAmount,
                                                              Long repaymentValue) {
        Long beyondAmount = 0L; // 超出还款金额
        Long fineAmount = originalAlterResume.getFineAmount(); // 罚息
        Long remainAmount = originalAlterResume.getRemainPayment(); // 剩余未还款金额

        // 超出的还款金额 = 本次还的 - 上次还欠的
        if (lastDebtAmount != null && repaymentValue - lastDebtAmount > 0) {
            if (fineAmount > 0) {
                // 有罚息, 则超出金额 = 本次还的 - (上次欠的 + 罚息)
                beyondAmount = repaymentValue - (lastDebtAmount + fineAmount);
            } else {
                beyondAmount = repaymentValue - lastDebtAmount;
            }
        }

        CreditRepaymentResume resume = new CreditRepaymentResume();

        resume.setAlterResumeId(originalAlterResume.getId());
        resume.setType(CreditRepaymentType.REPAYMENT.value());
        resume.setStatus(CreditRepaymentStatus.PASS.value());

        resume.setFee(repaymentValue); // 本次还款金额
        resume.setRemainAmount(remainAmount);
        resume.setLastDebtAmount(lastDebtAmount == null ? 0L : lastDebtAmount);
        resume.setBeyondAmount(beyondAmount);
        resume.setFineAmount(fineAmount);

        return resume;
    }

    /**
     * 更新额度履历信息
     *
     * @param resume            原始额度履历信息
     * @param originalShopExtra 店铺信息
     * @param shopUser 专属会员信息
     * @param repaymentValue    变更额度
     * @return 变更后的额度履历信息
     */
    private CreditAlterResume updateCreditAlterResume(CreditAlterResume resume,
                                                      VegaShopExtra originalShopExtra,
                                                      ShopUser shopUser,
                                                      Long repaymentValue,
                                                      Date repaymentDate) {
        // 最新信用额度, 原始剩余还款值, 最新剩余还款值, 是否还款完成, 罚息, 已还金额, 总金额
        // 如果最新剩余还款值小于等于0, 则表示还款完成(小于0表示多还了)
        Long newestCredit = 0L;
        Long remainPayment = 0L;
        Long newestRemainPayment = 0L;
        Boolean isRepaymentComplete = false;
        Long fineAmount = 0L;
        Long alreadyRepayment = 0L;
        Long totalCredit = 0L;
        Long availableCredit = 0L;
        if (!Arguments.isNull(originalShopExtra)) {
            totalCredit = originalShopExtra.getTotalCredit();
            availableCredit = originalShopExtra.getAvailableCredit(); // 最新可用额度
        } else {
            availableCredit = shopUser.getAvailableCredit(); // 最新可用额度
            totalCredit = shopUser.getTotalCredit();
        }
        Long totalDebt = Math.abs(resume.getAlterValue());

        Shop adminShop = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Integer rate = Integer.valueOf(adminShop.getTags().get(SystemConstant.CREDIT_INTEREST));

        remainPayment = resume.getRemainPayment();
        alreadyRepayment = resume.getAlreadyPayment();
        Date shouldRepaymentDate = resume.getShouldRepaymentDate();
        fineAmount = ShopHelper.calculateFineAmount(remainPayment, shouldRepaymentDate, repaymentDate, rate);

        // 当有罚息时，不允许部分还款，只允许全部还款
        if (fineAmount > 0) {
            remainPayment = resume.getRemainPayment() + fineAmount;
            if (repaymentValue - remainPayment < 0) {
                log.error("failed to update credit alter resume, cause fineAmount > 0 and repaymentValue is not " +
                        "enough to pay for the total debt.");
                throw new JsonResponseException(500, "repayment.is.not.enough");
            }
        }

        alreadyRepayment += repaymentValue;
        newestCredit = availableCredit + repaymentValue - fineAmount;
        newestRemainPayment = remainPayment - repaymentValue;
        isRepaymentComplete = newestRemainPayment <= 0;

        resume.setRemainPayment(isRepaymentComplete ? 0 : newestRemainPayment);
        resume.setIsPaymentComplete(isRepaymentComplete);
        if (!Arguments.isNull(originalShopExtra)) {
            resume.setLastCredit(originalShopExtra.getAvailableCredit());
        } else {
            resume.setLastCredit(shopUser.getAvailableCredit());
        }
        resume.setNewestCredit(newestCredit);
        resume.setAlterStatus(validCreditStatus(isRepaymentComplete));
        resume.setActualRepaymentDate(repaymentDate);
        resume.setAlreadyPayment(alreadyRepayment);
        resume.setTotalCredit(totalCredit);
        resume.setFineAmount(fineAmount);

        return resume;
    }

    /**
     * 创建还款履历, 更新信用额度变更值, 更新店铺额度值
     *
     * @param toUpdateShopExtra   店铺信息
     * @param toUpdateShopUser    专属会员信息
     * @param originalAlterResume 额度变更履历
     * @param operatedByAdmin     额度变更履历(类型为运营操作的还款履历信息)
     * @param repaymentResume     还款履历
     * @return 执行结果
     */
    private Boolean doCreateRepaymentResume(Shop shop,
                                            User user,
                                            VegaShopExtra toUpdateShopExtra,
                                            ShopUser toUpdateShopUser,
                                            CreditAlterResume originalAlterResume,
                                            CreditAlterResume operatedByAdmin,
                                            CreditRepaymentResume repaymentResume) {
        Response<Boolean> resp;
        if (!Arguments.isNull(toUpdateShopUser)) {
            Response<Boolean> booleanResponse = shopUserWriteService.updateShopUserCreditByUserId(toUpdateShopUser.getUserId(), originalAlterResume.getNewestCredit(),
                    toUpdateShopUser.getTotalCredit());
            if (!booleanResponse.isSuccess()) {
                log.error("failed to update shop user available credit by userID = ({}), availableCredit = {}, " +
                        "cause : {}", toUpdateShopUser.getUserId(), originalAlterResume.getNewestCredit(), booleanResponse.getError());
                throw new JsonResponseException(500, booleanResponse.getError());
            }
            resp = creditAlterResumeWriteService.
                    createRepaymentResume(originalAlterResume, operatedByAdmin, repaymentResume);
        } else {
            resp = creditAlterResumeWriteService.
                    createRepaymentResume(toUpdateShopExtra, originalAlterResume, operatedByAdmin, repaymentResume);
        }
        if (!resp.isSuccess()) {
            log.error("failed to create credit repayment resume by creditAlterResumeId = ({}), alterValue = ({}), " +
                    "cause : {}", originalAlterResume.getId(), repaymentResume, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        // 短信通知已还款
        sendSmsNotification(shop, user, originalAlterResume);

        return resp.getResult();
    }

    /**
     * 发短信通知
     *
     * @param shop 店铺信息
     * @param user 专属会员信息
     * @param alterResume 履历信息
     */
    private void sendSmsNotification(Shop shop, User user, CreditAlterResume alterResume) {
        String receiver = null;
        if (!Arguments.isNull(shop)) {
            receiver = findMobileByShop(shop);
        } else {
            receiver = user.getMobile();
        }
        String template = "already.repayment.credit2"; // 专属会员共用同一个模板
        MsgContext context = MsgContext.of(
                "orderId", alterResume.getTradeNo()
        );

        // do send sms
        if (Strings.isNullOrEmpty(receiver)) {
            log.error("failed to send sms to shopId = [{}], cause mobile is not exists.", shop.getId());
            return;
        }
        doSendSms(receiver, template, context);
    }

    /**
     * 发送短信
     *
     * @param receiver 短信接受者
     * @param template 短信模板
     * @param context  上下文
     */
    private void doSendSms(String receiver, String template, MsgContext context) {
        try {
            String result = msgService.send(receiver, template, context, null);
            log.info("[credit already repayment notification] sendSms result = {}, mobile = {}, message = {}",
                    result, receiver, context);
        } catch (MsgException e) {
            log.error("sms send failed, mobile = {}, template = {}, cause : {}",
                    receiver, template, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 查找店铺手机号
     *
     * @param shop 店铺信息
     * @return     店铺手机号
     */
    private String findMobileByShop(Shop shop) {
        return Strings.isNullOrEmpty(shop.getPhone()) ? findUserById(shop.getUserId()).getMobile() : shop.getPhone();
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
            return new User(); // let it go.
        }
        return resp.getResult();
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
     * 通过用户ID查询专属会员信息信息
     *
     * @param userId 用户Id
     * @return 信息
     */
    private ShopUser findShopUserByUserId(Long userId) {
        Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(userId);
        if (!shopUserResponse.isSuccess()) {
            log.error("failed to find credit by userId = {}, cause : {}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
        if (!shopUserOptional.isPresent()) {
            return null;
        }
        return shopUserOptional.get();
    }


    /**
     * 通过订单ID查询
     *
     * @param tradeNo 订单ID
     * @return 信息
     */
    private CreditAlterResume findCreditAlterResumeByTradeNo(String tradeNo) {
        Response<CreditAlterResume> resp = creditAlterResumeReadService.findByTradeNo(tradeNo);
        if (!resp.isSuccess()) {
            log.error("failed to find credit alter resume by tradeNo = {}, cause : {}",
                    tradeNo, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 通过ID获取履历信息
     *
     * @param id 履历ID
     * @return 履历信息
     */
    private CreditAlterResume findCreditAlterResumeById(Long id) {
        Response<CreditAlterResume> resumeResp = creditAlterResumeReadService.findById(id);
        if (!resumeResp.isSuccess()) {
            log.error("failed to find credit resume by id = ({}), cause : {}", id, resumeResp.getError());
            throw new JsonResponseException(500, resumeResp.getError());
        }
        return resumeResp.getResult();
    }

    /**
     * 根据是否还款完成获取信用额度履历状态
     *
     * @param isRepaymentComplete 是否还款完成
     * @return 状态值
     */
    private Integer validCreditStatus(Boolean isRepaymentComplete) {
        return isRepaymentComplete ?
                CreditAlterStatus.COMPLETE.value() : CreditAlterStatus.PART_REPAYMENT.value();
    }


    /**
     * 获取店铺信息,只用于修改信用额度
     *
     * @param  shopId 店铺Id
     * @return 返回的店铺信息
     */
    private VegaShopExtra getVegaShopExtra(Long shopId){
        // 店铺需要存在
        Response<VegaShop> shopResponse = vegaShopReadService.findByShopId(shopId);
        if (!shopResponse.isSuccess()) {
            log.error("failed to find shop and VegaShopExtra by shopId = ({}), cause : {}",
                    shopId, shopResponse.getError());
            throw new JsonResponseException(500, shopResponse.getError());
        }
        VegaShopExtra shopExtra = shopResponse.getResult().getShopExtra();
        // 判断信用额度是否可用
        if (shopExtra.getIsCreditAvailable() == null) {
            log.error("failed to update credit by shopId = ({}), cause the credit of this shop is not available.");
            throw new JsonResponseException(500, "credit.not.setup");
        }
        if (!shopExtra.getIsCreditAvailable()) {
            log.error("failed to update credit by shopId = ({}), cause the credit of this shop is not available.");
            throw new JsonResponseException(500, "credit.not.available");
        }

        shopExtra.setTotalCredit(MoreObjects.firstNonNull(shopExtra.getTotalCredit(), 0L));
        shopExtra.setAvailableCredit(MoreObjects.firstNonNull(shopExtra.getAvailableCredit(), 0L));

        return shopExtra;
    }

    /**
     * 获取专属会员信息,只用于修改信用额度
     *
     * @param  userId 用户Id
     * @return 返回的专属会员信息
     */
    private ShopUser getShopUserInfo(Long userId){
        Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(userId);
        if (!shopUserResponse.isSuccess()) {
            log.error("failed to find shop user by userId = ({}), cause : {}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
        if (!shopUserOptional.isPresent()) {
            log.error("failed to find shop user by userId = ({}), cause : {}", userId, shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }

        ShopUser shopUser = shopUserOptional.get();
        // 判断信用额度是否可用
        if (shopUser.getIsCreditAvailable() == null) {
            log.error("failed to update credit by userId = ({}), cause the credit of this user is not available.");
            throw new JsonResponseException(500, "credit.user.not.setup");
        }
        if (!shopUser.getIsCreditAvailable()) {
            log.error("failed to update credit by userId = ({}), cause the credit of this user is not available.");
            throw new JsonResponseException(500, "credit.user.not.available");
        }

        shopUser.setTotalCredit(MoreObjects.firstNonNull(shopUser.getTotalCredit(), 0L));
        shopUser.setAvailableCredit(MoreObjects.firstNonNull(shopUser.getAvailableCredit(), 0L));

        return shopUser;
    }


}
