/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.credit;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.helper.ShopHelper;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.shop.dto.CreditAlterResumeDto;
import com.sanlux.shop.dto.CreditRepaymentDetail;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.CreditAlterStatus;
import com.sanlux.shop.enums.CreditSmsNodeEnum;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
import com.sanlux.shop.service.CreditRepaymentResumeReadService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.service.ShopUserReadService;
import com.sanlux.web.front.core.sms.SmsNodeSwitchParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/credit")
public class CreditAlterResumes {

    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;
    @RpcConsumer
    private CreditRepaymentResumeReadService creditRepaymentResumeReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private ShopUserReadService shopUserReadService;
    @Autowired
    private MsgService msgService;
    @Autowired
    private SmsNodeSwitchParser smsNodeSwitchParser;

    /**
     * 信用额度履历分页
     *
     * @param pageNo   页码
     * @param pageSize 分页大小
     * @param type     查询类型(1:二级经销商  2:专属会员)
     * @return credit操作履历
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<CreditAlterResume> creditAlterResumePaging(Integer pageNo,
                                                             Integer pageSize,
                                                             @RequestParam(value = "tradeNo", required = false) String tradeNo,
                                                             @RequestParam(value = "alterType", required = false) Integer alterType,
                                                             @RequestParam(value = "startAt", required = false) String startAt,
                                                             @RequestParam(value = "endAt", required = false) String endAt,
                                                             @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
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
        Long shopId = null;

        if (Objects.equal(type, 2)) {
            // 专属会员查询
            criteria.put("userId", UserUtil.getUserId());
        } else {
            Response<Shop> shopResp = shopReadService.findByUserId(UserUtil.getUserId());
            if (!shopResp.isSuccess()) {
                log.error("failed to find shop by userId = {}, cause : {}", UserUtil.getUserId(), shopResp.getError());
                throw new JsonResponseException(500, shopResp.getError());
            }
            shopId = shopResp.getResult().getId();
            criteria.put("shopId", shopId);
        }

        if (!Strings.isNullOrEmpty(tradeNo)) {
            criteria.put("tradeNo", tradeNo);
        }
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
            if (Objects.equal(type, 2)) {
                log.error("failed to find credit alter resume by userId = {}, cause: {}",
                        UserUtil.getUserId(), resp.getError());
            } else {
                log.error("failed to find credit alter resume by shopId = {}, cause: {}",
                        shopId, resp.getError());
            }
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
    @RequestMapping(value = "/paging2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<CreditAlterResume> creditAlterResumePaging(Integer pageNo,
                                                             Integer pageSize,
                                                             @RequestParam(value = "shopId", required = false) Long shopId,
                                                             @RequestParam(value = "userId", required = false) Long userId,
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
        if (!Arguments.isNull(shopId)) {
            criteria.put("shopId", shopId);
        }
        if (!Arguments.isNull(userId)) {
            criteria.put("userId", userId);
        }
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
     * @param startAt 起止时间(每月第一天)
     * @param endAt   结束时间(每月最后一天)
     * @param type     查询类型(1:二级经销商  2:专属会员)
     * @return 履历信息
     */
    @RequestMapping(value = "/monthly", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditAlterResumeDto listMonthlyResume(Integer pageNo,
                                                  Integer pageSize,
                                                  @RequestParam(value = "tradeNo", required = false) String tradeNo,
                                                  @RequestParam(value = "startAt", required = false) String startAt,
                                                  @RequestParam(value = "endAt", required = false) String endAt,
                                                  @RequestParam(value = "status", required = false) Integer status,
                                                  @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
        ParanaUser user = UserUtil.getCurrentUser();
        if (!Strings.isNullOrEmpty(tradeNo)) {
            return generateCreditAlterResumeDto(findCreditAlterResumeByTradeNo(tradeNo));
        }
        if (Objects.equal(type, 2)) {
            // 专属会员
            Long userId = user.getId();
            Response<CreditAlterResumeDto> resp = creditAlterResumeReadService
                    .listMonthlyAlterResumeByShopIdOrUserId(null, userId, pageNo, pageSize, startDate(startAt), endDate(endAt), status);
            if (!resp.isSuccess()) {
                log.error("failed to find credit resume by userId = ({}), startAt = ({}), endAt = ({}), cause : {}",
                        userId, startAt, endAt, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
            return resp.getResult();
        } else {
            Long shopId = user.getShopId();
            Response<CreditAlterResumeDto> resp = creditAlterResumeReadService
                    .listMonthlyAlterResumeByShopIdOrUserId(shopId, null, pageNo, pageSize, startDate(startAt), endDate(endAt), status);
            if (!resp.isSuccess()) {
                log.error("failed to find credit resume by shopId = ({}), startAt = ({}), endAt = ({}), cause : {}",
                        shopId, startAt, endAt, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
            return resp.getResult();
        }
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
     * 对应专属会员每个月的额度履历
     *
     * @param userId  用户ID
     * @param startAt 起止时间(每月第一天)
     * @param endAt   结束时间(每月最后一天)
     * @return 履历信息
     */
    @RequestMapping(value = "/monthly/user/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditAlterResumeDto listMonthlyResumeByUserId(@PathVariable(value = "userId") Long userId,
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
                .listMonthlyAlterResumeByShopIdOrUserId(null, userId, pageNo, pageSize, startDate(startAt), endDate(endAt), status);
        if (!resp.isSuccess()) {
            log.error("failed to find credit resume by userId = ({}), startAt = ({}), endAt = ({}), cause : {}",
                    userId, startAt, endAt, resp.getError());
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
     * 获得经销商或专属会员的信用额度
     *
     * @return 数据
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaShopExtra findVegaShopExtra() {
        ParanaUser user = UserUtil.getCurrentUser();
        VegaShopExtra vegaShopExtra = new VegaShopExtra();
        if (!Arguments.isNull(user.getShopId())) {
            //店铺买家
            Response<VegaShopExtra> resp = vegaShopReadService.findVegaShopExtraByUserId(user.getId());
            if (!resp.isSuccess()) {
                log.error("failed to find credit by userId = {}, cause : {}", user.getId(), resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }
            vegaShopExtra = resp.getResult();
        } else {
            //专属会员买家
            Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(user.getId());
            if (!shopUserResponse.isSuccess()) {
                log.error("failed to find credit by userId = {}, cause : {}", user.getId(), shopUserResponse.getError());
                throw new JsonResponseException(500, shopUserResponse.getError());
            }
            Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
            if (shopUserOptional.isPresent()) {
                ShopUser shopUser = shopUserResponse.getResult().get();
                vegaShopExtra.setTotalCredit(shopUser.getTotalCredit());
                vegaShopExtra.setAvailableCredit(shopUser.getAvailableCredit());
                vegaShopExtra.setIsCreditAvailable(shopUser.getIsCreditAvailable());
            }
        }
        return  vegaShopExtra;
    }

    /**
     * 更新信用额度履历状态
     *
     * @param id 履历id
     * @return 结果
     */
    @RequestMapping(value = "/{id}/{tradeNo}", method = RequestMethod.PUT)
    public Boolean changeCreditAlterResumeStatus(@PathVariable("id") Long id,
                                                 @PathVariable("tradeNo") String tradeNo) {
        Integer status = CreditAlterStatus.WAIT_AUDIT.value();
        CreditAlterResume resume = new CreditAlterResume();
        resume.setId(id);
        resume.setAlterStatus(status);
        Response<Boolean> resp = creditAlterResumeWriteService.update(resume);
        if (!resp.isSuccess()) {
            log.error("failed to change credit alter resume status by id = {}, resume = {}, cause : {}",
                    id, status, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        // 发短信通知运营或经销商
        ParanaUser buyer = UserUtil.getCurrentUser();
        Optional<Shop> opt = findShopById(buyer.getShopId());
        if (opt.isPresent()) {
            resume.setTradeNo(tradeNo);
            callAdminToHandleRepayment(resume, opt.get());
        } else {
            // 专属会员发给对应经销商
            Optional<String> optPho = findMobileByUserId(buyer.getId());
            if (optPho.isPresent()) {
                resume.setTradeNo(tradeNo);
                callDealerToHandleRepayment(resume, optPho.get());
            }
        }
        return resp.getResult();
    }

    /**
     * 通过ID查找店铺(找不到不抛异常)
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Optional<Shop> findShopById(Long shopId) {
        Response<Shop> shopResp = shopReadService.findById(shopId);
        if (!shopResp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, shopResp.getError());
            return Optional.absent();
        }
        return Optional.fromNullable(shopResp.getResult());
    }

    /**
     * 通过ID查找店铺(找不到不抛异常)
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Optional<VegaShop> findParentShopById(Long shopId) {
        Response<Optional<VegaShop>> shopResp = vegaShopReadService.finParentShopById(shopId);
        if (!shopResp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, shopResp.getError());
            return Optional.absent();
        }
        return shopResp.getResult();
    }

    /**
     * 通知运营操作信用额度
     *
     * @param resume 还款履历
     * @param shop   买家
     */
    private void callAdminToHandleRepayment(CreditAlterResume resume, Shop shop) {
        if (!nodeIsOpen(CreditSmsNodeEnum.RECOVERY)) {
            log.warn("failed to send sms that call admin handle repayment, cause sms node is not open.");
            return;
        }

        String receiver = findAdminMobile();
        String template = CreditSmsNodeEnum.RECOVERY.getName();
        MsgContext context = MsgContext.of(
                "orderId", resume.getTradeNo(),
                "shopName", shop.getName()
        );

        doSendSms(receiver, template, context);
    }

    /**
     * 专属会员通知对应经销商操作信用额度
     *
     * @param resume 还款履历
     * @param mobile 买家手机号
     */
    private void callDealerToHandleRepayment(CreditAlterResume resume, String mobile) {
        if (!nodeIsOpen(CreditSmsNodeEnum.RECOVERY)) {
            // 共用运营短信控制节点
            log.warn("failed to send sms that call admin handle repayment, cause sms node is not open.");
            return;
        }
        String template = CreditSmsNodeEnum.USER_RECOVERY.getName();
        MsgContext context = MsgContext.of(
                "orderId", resume.getTradeNo(),
                "userName", mobile
        );

        doSendSms(mobile, template, context);
    }

    /**
     * 发短信
     */
    private void doSendSms(String receiver, String template, MsgContext context) {
        try {
            if (Strings.isNullOrEmpty(receiver)) {
                log.error("failed to send sms to admin, cause admin's mobile is not exists.");
                return;
            }

            String result = msgService.send(receiver, template, context, null);
            log.info("[credit repayment notification] sendSms result = {}, mobile = {}, message = {}",
                    result, receiver, context);
        } catch (MsgException e) {
            log.error("sms send failed, mobile = {}, cause : {}", receiver, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 判断节点是否打开
     *
     * @param nodeEnum 短信节点
     * @return 是否可用
     */
    private Boolean nodeIsOpen(CreditSmsNodeEnum nodeEnum) {
        Response<Boolean> isOpenRes = smsNodeSwitchParser.creidtIsOpen(nodeEnum);
        if (!isOpenRes.isSuccess()) {
            log.error("fail to parse sms node:{} is open,error:{}", nodeEnum.toString(), isOpenRes.getError());
            return false;
        }
        return isOpenRes.getResult();
    }

    /**
     * 查询运营手机号
     *
     * @return 手机号
     */
    private String findAdminMobile() {
        Long shopId = ((ParanaUser)UserUtil.getCurrentUser()).getShopId();
        Optional<Shop> opt = findShopById(shopId);
        if (!opt.isPresent()) {
            return null;
        }
        Shop shop = opt.get();

        // 二级经销商发短信给一级经销商, 一级发短信给运营
        if (ShopTypeHelper.isSecondDealerShop(shop.getType())) {
            Optional<VegaShop> optional = findParentShopById(shop.getId());
            if (!optional.isPresent()) {
                return null;
            }
            shop = optional.get().getShop();
        }else {
            Response<Shop> shopResp = shopReadService.findById(DefaultId.PLATFROM_SHOP_ID);
            if (!shopResp.isSuccess()) {
                log.error("failed to find admin by id = ({}), cause : {}", DefaultId.PLATFROM_SHOP_ID, shopResp.getError());
                return null;
            }
            shop = shopResp.getResult();
        }

        String phone = shop.getPhone();
        if (Strings.isNullOrEmpty(phone)) {
            Optional<String> optPho = findMobileByUserId(shop.getUserId());
            if (!optPho.isPresent()) {
                return null;
            }
            phone = optPho.get();
        }
        return phone;
    }

    /**
     * 差用户手机号
     * @param userId 用户ID
     * @return 用户手机号
     */
    private Optional<String> findMobileByUserId(Long userId) {
        Response<User> resp = userReadService.findById(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find user mobile by user cause : {}", resp.getError());
            return Optional.absent();
        }
        return Optional.fromNullable(resp.getResult().getMobile());
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
        Optional<Shop> opt = findShopById(DefaultId.PLATFROM_SHOP_ID);
        Shop shop = null;
        if (opt.isPresent()) {
            shop = opt.get();
        } else {
            log.error("failed to find shop by id = {}, cause shop not exists.", DefaultId.PLATFROM_SHOP_ID);
            throw new JsonResponseException("shop.find.failed");
        }

        CreditAlterResumeDto resumeDto = new CreditAlterResumeDto();
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
