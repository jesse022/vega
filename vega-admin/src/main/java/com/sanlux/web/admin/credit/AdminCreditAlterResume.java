package com.sanlux.web.admin.credit;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.helper.ShopHelper;
import com.sanlux.shop.dto.CreditAlterResumeDto;
import com.sanlux.shop.dto.CreditRepaymentDetail;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.CreditAlterStatus;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.enums.CreditRepaymentStatus;
import com.sanlux.shop.enums.CreditRepaymentType;
import com.sanlux.shop.enums.CreditSmsNodeEnum;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.CreditRepaymentResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
import com.sanlux.shop.service.CreditRepaymentResumeReadService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.msg.exception.MsgException;
import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.MsgContext;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created Date : 16/8/17
 * Author : wujianwei
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/credit")
public class AdminCreditAlterResume {

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

    /**
     * 修改经销商信用额度
     *
     * @param shopId     店铺(经销商)ID
     * @param alterValue 变更额度
     * @return 修改结果
     */
    @RequestMapping(method = RequestMethod.PUT)
    public Boolean updateCredit(@RequestParam(value = "shopId") Long shopId,
                                @RequestParam(value = "alterValue") Long alterValue) {
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
        // 判断信用额度是否可用
        if (!shopExtra.getIsCreditAvailable()) {
            log.error("failed to update credit by shopId = ({}), cause the credit of this shop is not available.");
            throw new JsonResponseException(500, "credit.not.available");
        }

        shopExtra.setTotalCredit(MoreObjects.firstNonNull(shopExtra.getTotalCredit(), 0L));
        shopExtra.setAvailableCredit(MoreObjects.firstNonNull(shopExtra.getAvailableCredit(), 0L));

        // 操作人信息
        ParanaUser user = UserUtil.getCurrentUser();
        Integer alterType = alterValue >= 0 ? CreditAlterType.ADMIN_ADD.value() : CreditAlterType.ADMIN_REDUCE.value();
        CreditAlterResume resume = generateCreditAlterResume(user, shopExtra, alterType, alterValue);

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

    /**
     * 信用额度履历分页
     *
     * @param pageNo   页码
     * @param pageSize 分页大小
     * @param shopId   店铺ID
     * @return credit操作履历
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<CreditAlterResume> creditAlterResumePaging(Integer pageNo,
                                                             Integer pageSize,
                                                             @RequestParam("shopId") Long shopId,
                                                             @RequestParam(value = "tradeNo", required = false) String tradeNo,
                                                             @RequestParam(value = "alterType", required = false) Integer alterType,
                                                             @RequestParam(value = "startAt", required = false) String startAt,
                                                             @RequestParam(value = "endAt", required = false) String endAt) {
        // tradeNo
        if (!Strings.isNullOrEmpty(tradeNo)) {
            CreditAlterResume resume = findCreditAlterResumeByTradeNo(tradeNo);
            if (resume == null) {
                return Paging.empty();
            }
            List<CreditAlterResume> resumeList = ImmutableList.of(resume);
            return new Paging<>(1L, resumeList);
        }

        // query condition
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("shopId", shopId);
        if (alterType != null) {
            criteria.put("alterType", alterType);
        }
        if (!Strings.isNullOrEmpty(startAt)) {
            criteria.put("startAt", startDate(startAt));
        }
        if (!Strings.isNullOrEmpty(endAt)) {
            criteria.put("endAt", endDate(endAt));
        }

        Response<Paging<CreditAlterResume>> resp = creditAlterResumeReadService.paging(pageNo, pageSize, criteria);
        if (!resp.isSuccess()) {
            log.error("failed to find credit alter resume by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 对应店铺每个月的额度履历
     *
     * @param shopId  店铺ID
     * @param startAt 起止时间(每月第一天)
     * @param endAt   结束时间(每月最后一天)
     * @return 履历信息
     */
    @RequestMapping(value = "/monthly/{shopId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditAlterResumeDto listMonthlyResume(@PathVariable(value = "shopId") Long shopId,
                                                  Integer pageNo,
                                                  Integer pageSize,
                                                  @RequestParam(value = "tradeNo", required = false) String tradeNo,
                                                  @RequestParam(value = "startAt", required = false) String startAt,
                                                  @RequestParam(value = "endAt", required = false) String endAt,
                                                  @RequestParam(value = "status", required = false) Integer status) {
        if (!Strings.isNullOrEmpty(tradeNo)) {
            CreditAlterResume resume = findCreditAlterResumeByTradeNo(tradeNo);
            return generateCreditAlterResumeDto(resume);
        }

        Response<CreditAlterResumeDto> resp = creditAlterResumeReadService
                .listMonthlyAlterResumeByShopIdOrUserId(shopId, null, pageNo, pageSize, startDate(startAt), endDate(endAt), status);
        if (!resp.isSuccess()) {
            log.error("failed to find credit resume by shopId = ({}), startAt = ({}), endAt = ({}), cause : {}",
                    shopId, startAt, endAt, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 查看还款履历详情
     *
     * @param id 信用额度履历ID
     * @return 履历详情
     */
    @RequestMapping(value = "/repayment_detail/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditRepaymentDetail findRepaymentDetailByResumeId(@PathVariable(value = "id") Long id) {
        Response<CreditRepaymentDetail> resp = creditRepaymentResumeReadService.findByAlterResumeId(id);
        if (!resp.isSuccess()) {
            log.error("failed to find repayment detail by resume id = {}, cause : {}", id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
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
     * 运营操作还款(还信用额度)
     *
     * @param id             履历ID
     * @param repaymentValue 还款金额
     * @return 操作结果
     */
    @RequestMapping(value = "/add_repayment/{id}", method = RequestMethod.PUT)
    public Boolean repayment(@PathVariable(value = "id") Long id,
                             @RequestParam(value = "status", required = false) Integer status,
                             @RequestParam(value = "repaymentValue", required = false) Long repaymentValue,
                             @RequestParam(value = "paymentDate", required = false) String paymentDate) {
        // 审核不通过, 只改状态
        if (!isAuditPass(status)) {
            return changeCreditAlterResumeStatus(id, status);
        }
        checkNotNull(repaymentValue, "repayment.value.is.null");
        checkNotNull(paymentDate, "repayment.date.is.null");
        // 履历信息
        CreditAlterResume originalAlterResume = findCreditAlterResumeById(id);

        // 店铺信息
        VegaShop vegaShop = findVegaShopByShopId(originalAlterResume.getShopId());
        Shop shop = vegaShop.getShop();
        VegaShopExtra originalShopExtra = vegaShop.getShopExtra();

        // 判断信用额度是否可用, 需要先将信用额度置为可用才能修改
        if (!originalShopExtra.getIsCreditAvailable()) {
            log.error("failed to update credit by shopId = ({}), cause the credit of this shop is not available.");
            throw new JsonResponseException(500, "credit.not.available");
        }

        // 实际还款日期
        Date actualPaymentDate = DateTime.parse(paymentDate).toDate();
        Long lastDebtAmount = originalAlterResume.getRemainPayment();
        Long lastAvailableCredit = originalAlterResume.getAvailableCredit(); // TODO

        // 1. 根据还款值修改履历信息
        originalAlterResume = updateCreditAlterResume(originalAlterResume, originalShopExtra,
                repaymentValue, actualPaymentDate);

        // 2. 记录一条还款履历
        CreditRepaymentResume repaymentResume = createRepaymentResumeObject(originalAlterResume,
                lastDebtAmount, repaymentValue);

        // 3. 更新店铺信用额度
        VegaShopExtra toUpdateShopExtra = updateShopExtraCredit(originalShopExtra, originalAlterResume);

        // 4. 新增一条运营操作履历, 类型为还款?
        ParanaUser currentUser = UserUtil.getCurrentUser();
        Integer alterType = CreditAlterType.PERSONAL_REPAYMENT.value();
        CreditAlterResume operatedByAdmin = generateCreditAlterRepaymentResume(currentUser, toUpdateShopExtra,
                alterType, lastAvailableCredit, repaymentValue);

        // 创建和更新信息
        return doCreateRepaymentResume(shop, toUpdateShopExtra, originalAlterResume, operatedByAdmin, repaymentResume);
    }

    /**
     * 获取一级经销商还款待审核数据接口
     * add by lujm on 2017/2/15
     * @return Paging<VegaShop>
     */
    @RequestMapping(value = "/pagingPayment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<VegaShop> creditAlterResumePagingPayment(Integer pageNo,
                                                           Integer pageSize) {
        Map<String, Object> criteria = Maps.newHashMap();
        Long total=0L;
        Response<Paging<CreditAlterResume>> resp = creditAlterResumeReadService.pagingDistinctShopID(pageNo, pageSize, criteria);
        if (!resp.isSuccess()) {
            log.error("failed to find credit alter resume by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        List<VegaShop> vegaShops = fromCreditToShop(resp.getResult().getData());
        if (!CollectionUtils.isEmpty(vegaShops)) {
            total=(long)vegaShops.size();
        }
        return new Paging<>(total, vegaShops);
    }

    /**
     * 根据店铺IDs获取一级经销商信息
     * add by lujm on 2017/2/15
     * @param creditAlterResumes
     * @return
     */
    private List<VegaShop> fromCreditToShop(List<CreditAlterResume> creditAlterResumes) {
        List<Long> ShopIds = Lists.transform(creditAlterResumes, CreditAlterResume::getShopId);
        Response<List<VegaShop>> vegaShopListByShopIds = vegaShopReadService.findFirstDealerByShopIds(ShopIds);
        if (!vegaShopListByShopIds.isSuccess()) {
            log.error("fromCreditToShop fail, creditAlterResumes:{}, cause:{}",
                    creditAlterResumes, vegaShopListByShopIds.getError());
            return Collections.emptyList();
        }
        return vegaShopListByShopIds.getResult();
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
     * @param alterValue 操作值
     * @return 履历信息
     */
    private CreditAlterResume generateCreditAlterResume(ParanaUser user, VegaShopExtra shopExtra, Integer alterType, Long alterValue) {
        Long orgTotalCredit = shopExtra.getTotalCredit();
        Long orgAvailableCredit = shopExtra.getAvailableCredit();
        Long newestCredit = orgTotalCredit + alterValue;
        // 变更之后的值不能小于0
        if (newestCredit < 0) {
            log.error("failed to change credit by shopId = {}, to update credit value = {}, cause the credit " +
                    "could not less than zero.", shopExtra.getShopId(), alterValue);
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();

        resume.setShopId(shopExtra.getShopId());
        resume.setShopName(shopExtra.getShopName());
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
     * @param alterValue 操作值
     * @return 履历信息
     */
    private CreditAlterResume generateCreditAlterRepaymentResume(ParanaUser user,
                                                                 VegaShopExtra shopExtra,
                                                                 Integer alterType,
                                                                 Long lastAvailableCredit,
                                                                 Long alterValue) {
        // 总额度不变
        Long totalCredit = shopExtra.getTotalCredit();
        // 变更之后的值不能小于0
        if (lastAvailableCredit < 0) {
            log.error("failed to change credit by shopId = {}, to update credit value = {}, cause the credit " +
                    "could not less than zero.", shopExtra.getShopId(), alterValue);
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();

        resume.setShopId(shopExtra.getShopId());
        resume.setShopName(shopExtra.getShopName());
        resume.setOperateId(user.getId());
        resume.setOperateName(user.getName());

        resume.setLastCredit(lastAvailableCredit);
        resume.setAlterValue(alterValue);
        resume.setAlterType(alterType);
        resume.setCreatedAt(new Date());
        resume.setNewestCredit(lastAvailableCredit);
        resume.setTotalCredit(totalCredit);
        resume.setAvailableCredit(shopExtra.getAvailableCredit());

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

        // 还款金额 + 剩余信用额度 > 总的信用额度  则: 信用额度需要累加
//        if (originalAlterResume.getNewestCredit() > orgShopExtra.getTotalCredit()) {
//            toUpdateShopExtra.setTotalCredit(originalAlterResume.getNewestCredit());
//        }

        return toUpdateShopExtra;
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
     * @param repaymentValue    变更额度
     * @return 变更后的额度履历信息
     */
    private CreditAlterResume updateCreditAlterResume(CreditAlterResume resume,
                                                      VegaShopExtra originalShopExtra,
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
        Long totalCredit = originalShopExtra.getTotalCredit();
        Long availableCredit = originalShopExtra.getAvailableCredit(); // 最新可用额度
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
        // 最新可用额度 = 之前可用额度 + 还款额度 - 罚款额度
        // newestCredit = resume.getNewestCredit() + repaymentValue - fineAmount;
        newestCredit = availableCredit + repaymentValue - fineAmount;
        // 如果 已还款金额 > 总欠款金额
        // Long moreValue = alreadyRepayment - totalDebt - fineAmount;
        // if (moreValue > 0) {
        // 最新总额度 = 已还金额 - 欠款金额 - 罚款金额
        // totalCredit = moreValue;
        // }
        newestRemainPayment = remainPayment - repaymentValue;
        isRepaymentComplete = newestRemainPayment <= 0;

        resume.setRemainPayment(isRepaymentComplete ? 0 : newestRemainPayment);
        resume.setIsPaymentComplete(isRepaymentComplete);
        resume.setLastCredit(originalShopExtra.getAvailableCredit());
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
     * @param originalAlterResume 额度变更履历
     * @param operatedByAdmin     额度变更履历(类型为运营操作的还款履历信息)
     * @param repaymentResume     还款履历
     * @return 执行结果
     */
    private Boolean doCreateRepaymentResume(Shop shop,
                                            VegaShopExtra toUpdateShopExtra,
                                            CreditAlterResume originalAlterResume,
                                            CreditAlterResume operatedByAdmin,
                                            CreditRepaymentResume repaymentResume) {
        Response<Boolean> resp = creditAlterResumeWriteService.
                createRepaymentResume(toUpdateShopExtra, originalAlterResume, operatedByAdmin, repaymentResume);
        if (!resp.isSuccess()) {
            log.error("failed to create credit repayment resume by creditAlterResumeId = ({}), alterValue = ({}), " +
                    "cause : {}", originalAlterResume.getId(), repaymentResume, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        // 短信通知已还款
        sendSmsNotification(shop, originalAlterResume);

        return resp.getResult();
    }

    /**
     * 发短信通知
     *
     * @param shop 店铺信息
     * @param alterResume 履历信息
     */
    private void sendSmsNotification(Shop shop, CreditAlterResume alterResume) {
        String receiver = findMobileByShop(shop);
        String template = CreditSmsNodeEnum.ALREADY_REPAYMENT.getName();
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
     * @param shipId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shipId) {
        Response<Shop> resp = shopReadService.findById(shipId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by id = ({}), cause : {}", shipId, resp.getError());
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
     * 生成信息
     *
     * @param resume 信息
     * @return 信息DTO
     */
    private CreditAlterResumeDto generateCreditAlterResumeDto(CreditAlterResume resume) {
        CreditAlterResumeDto resumeDto = new CreditAlterResumeDto();
        Shop shop = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Integer interest = Integer.valueOf(shop.getTags().get(SystemConstant.CREDIT_INTEREST));
        if (resume == null) {
            resumeDto.setResumePaging(Paging.empty());
            resumeDto.setTotalDebt(0L);
            resumeDto.setCreditInterest(interest);
            resumeDto.setFineRate(SystemConstant.FINE_RATE);
            return resumeDto;
        }
        // 已付款完成, 不记录
        if (!resume.getIsPaymentComplete()) {
            Long fineAmount = resume.getFineAmount();
            // 如果没有罚息则另外计算, 有则表示已经设置过罚息
            if (fineAmount == null || fineAmount == 0) {
                // 计算罚息 (剩余未还款金额 * 天数 * 利息比例(eg : 5/10000 万分之五))
                Long remain = resume.getRemainPayment();
                Date shouldPayCompleteDate = resume.getShouldRepaymentDate();
                fineAmount = ShopHelper.calculateFineAmount(remain, shouldPayCompleteDate, new Date(), interest);
                resume.setFineAmount(fineAmount);
            }
        }
        resumeDto.setResumePaging(new Paging<>(1L, ImmutableList.of(resume)));
        resumeDto.setTotalDebt(resume.getRemainPayment());
        resumeDto.setCreditInterest(interest);
        resumeDto.setFineRate(SystemConstant.FINE_RATE);
        return resumeDto;
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
     * 截止时间
     *
     * @param endAt 截止时间
     * @return date
     */
    private Date endDate(String endAt) {
        return Strings.isNullOrEmpty(endAt) ? null : DateTime.parse(endAt).plusDays(1).toDate();
    }

    /**
     * 起始时间
     *
     * @param startAt 起始时间
     * @return date
     */
    private Date startDate(String startAt) {
        return Strings.isNullOrEmpty(startAt) ? null : DateTime.parse(startAt).toDate();
    }

}
