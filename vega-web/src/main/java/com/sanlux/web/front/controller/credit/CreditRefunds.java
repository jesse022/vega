/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.credit;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.pay.credit.request.CreditPayToken;
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
import com.sanlux.shop.service.CreditRepaymentResumeWriteService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.service.ShopUserReadService;
import com.sanlux.user.service.ShopUserWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.NumberUtil;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.constants.Tokens;
import io.terminus.pay.service.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static com.sanlux.pay.credit.constants.CreditRefundParams.BATCH_NO;
import static com.sanlux.pay.credit.constants.CreditRefundParams.DETAIL_DATA;
import static com.sanlux.pay.credit.constants.CreditRefundParams.REFUND_NOTIFY_URL;
import static com.sanlux.pay.credit.constants.CreditRefundParams.SELLER_NO;

/**
 * @author : panxin
 */
@Slf4j
@Controller
@RequestMapping("/api/vega/credit")
public class CreditRefunds {

    private DateTimeFormatter DATE = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;
    @RpcConsumer
    private CreditRepaymentResumeWriteService creditRepaymentResumeWriteService;
    @RpcConsumer
    private CreditRepaymentResumeReadService creditRepaymentResumeReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private ShopUserReadService shopUserReadService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;
    @Autowired
    private TokenProvider<CreditPayToken> creditPayTokenProvider;
    @Autowired
    private ChannelRegistry channelRegistry;

    @RequestMapping(value = "/refund", method = RequestMethod.GET)
    @ResponseBody
    public String refund(HttpServletRequest request) {
        String xml = "";
        try {
            String refundNotifyUrl = request.getParameter(REFUND_NOTIFY_URL); // 异步通知地址
            String detailData = request.getParameter(DETAIL_DATA);
            String batchNo = request.getParameter(BATCH_NO); // 退款请求号
            String sellerNo = request.getParameter(SELLER_NO);

            // TODO seller信息
            log.info("sellerNo = [{}]", sellerNo);
            //userReadService.findById(Long.valueOf(sellerNo));

            // TODO 貌似不能拿到当前用户(因为属于类似第三方, 不能拿到调用者的信息?)
            ParanaUser user = UserUtil.getCurrentUser();
            log.info("current user = [{}]", user);

            // TODO: 9/2/16 数据怎么拆分
            String[] paymentData = detailData.split("\\^");
            String paymentCode = paymentData[0];
            String amount = paymentData[1];
            Long refundAmount = getRefundAmount(amount);

            // 验签
            PayChannel channel = channelRegistry.findChannel(CreditPayConstants.PAY_CHANNEL);
            channel.verify(request);

            // 生成退款履历
            generateCreditRefundResume(paymentCode, batchNo, refundAmount);

            Map<String, Object> params = Maps.newTreeMap();

            CreditPayToken creditPayToken = creditPayTokenProvider.findToken(Tokens.DEFAULT_ACCOUNT);

            params.put("payment_code", paymentCode);
            params.put("batch_no", batchNo);
            //params.put("detail_data", detailData);

            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(creditPayToken.getKey(), Charsets.UTF_8).hash().toString();
            params.put("sign", sign);
            params.put("sign_type", CreditPayConstants.SIGN_TYPE);

            // 请求参数用于签名校验
            String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
            // 异步通知 /api/vega/refund/notify/{channel}?{params}
            log.info("suffix = [{}]\n" +
                    "refundNotifyUrl = [{}]", suffix, refundNotifyUrl + "?" + suffix);
            String result = HttpRequest
                    .get(refundNotifyUrl + "?" +suffix)
                    .connectTimeout(10000)
                    .readTimeout(10000)
                    .body();

            log.info("[CreditPay Server] Async refund result notify " +
                    "\n\t url:   [{}]" +
                    "\n\t params:[{}]" +
                    "\n\t result:[{}]", refundNotifyUrl, params, result);

            // 给时间让前端响应?
            Thread.sleep(800);
            // // TODO: 9/2/16
            xml = "<credit_pay>\n" +
                    "   <is_success>T</is_success>\n" +
                    "</credit_pay>";
            return xml;
        } catch (Exception e) {
            log.error("failed to refund by credit-pay cause : {}", Throwables.getStackTraceAsString(e));
            return xml;
        }
    }

    /**
     * 生成履历信息
     */
    private void generateCreditRefundResume(String paymentCode, String refundNo, Long refundAmount) {
        // 已退款则不操作
        if (hadRefund(refundNo)) {
            log.info("refundNo = [{}] had refund.", refundNo);
            return;
        }

        // 查询交易信息
        CreditAlterResume orgAlterResume = findAlterResumeByPaymentCode(paymentCode);

        // 生成还款单(还款履历)
        CreditRepaymentResume repaymentResume = generateCreditRepaymentRefundResume(orgAlterResume, refundAmount);

        // 更新信用额度还款履历信息 TODO, 获取当前操作退款的用户信息
        VegaShopExtra orgShopExtra = null;
        ShopUser shopUser = null;

        ParanaUser currentUser = UserUtil.getCurrentUser();
        if (!Objects.isNull(orgAlterResume.getShopId())) {
            // 查找店铺信息
            orgShopExtra = findShopExtraByShopId(orgAlterResume.getShopId());
            orgAlterResume = updateAlterResume(currentUser, orgShopExtra, null, orgAlterResume, refundAmount);
        } else {
            // 查询专属会员
            shopUser = findShopUserByUserId(orgAlterResume.getUserId());
            orgAlterResume = updateAlterResume(currentUser, null, shopUser, orgAlterResume, refundAmount);
        }

        // 生成退款操作履历
        CreditAlterResume refundResume =
                generateCreditAlterRefundResume(currentUser, orgAlterResume, refundNo, refundAmount);

        if (!Objects.isNull(orgShopExtra)) {
            // 更新店铺信用额度信息
            orgShopExtra = updateShopExtra(orgShopExtra, orgAlterResume);
            doCreateRefundDetail(orgShopExtra, orgAlterResume, refundResume, repaymentResume, refundAmount, refundNo);
        } else {
            // 更新专属会员信用额度信息
            updateShopUser(shopUser, orgAlterResume);
            doCreateRefundDetail(null, orgAlterResume, refundResume, repaymentResume, refundAmount, refundNo);
        }
    }

    /**
     * 生成退款详情
     *
     * @param orgShopExtra    店铺信息
     * @param orgAlterResume  履历信息
     * @param refundResume    退款信息
     * @param repaymentResume 退款信息
     * @param refundAmount    退款金额
     * @param refundNo        退款交易流水号
     */
    private void doCreateRefundDetail(VegaShopExtra orgShopExtra,
                                      CreditAlterResume orgAlterResume,
                                      CreditAlterResume refundResume,
                                      CreditRepaymentResume repaymentResume,
                                      Long refundAmount,
                                      String refundNo) {
        Response<Boolean> resp;
        if (!Objects.isNull(orgShopExtra)) {
             resp = creditAlterResumeWriteService.createRepaymentResume(orgShopExtra, orgAlterResume,
                    refundResume, repaymentResume);
        } else {
            resp = creditAlterResumeWriteService.createRepaymentResume(orgAlterResume, refundResume, repaymentResume);
        }
        if (!resp.isSuccess()) {
            log.error("failed to find create credit repayment resume by refundNo = ({}), refundAmount = ({}), " +
                    "cause : {}.", refundNo, refundAmount, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
    }

    /**
     * 通过店铺ID查询店铺信息
     *
     * @param shopId 店铺ID
     * @return 信息
     */
    private VegaShopExtra findShopExtraByShopId(Long shopId) {
        Response<VegaShopExtra> shopExtraResp = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!shopExtraResp.isSuccess()) {
            log.error("failed to find shop extra by user shopId = ({}), cause : {}", shopId, shopExtraResp.getError());
            throw new JsonResponseException(500, shopExtraResp.getError());
        }
        return shopExtraResp.getResult();
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
        if (shopUserOptional.isPresent()) {
            return shopUserOptional.get();
        }
        return null;
    }

    /**
     * 通过交易流水号查询账单信息
     *
     * @param paymentCode 交易流水号
     * @return 信息
     */
    private CreditAlterResume findAlterResumeByPaymentCode(String paymentCode) {
        Response<CreditAlterResume> paymentResp = creditAlterResumeReadService.findByPaymentCode(paymentCode);
        if (!paymentResp.isSuccess() || paymentResp.getResult() == null) {
            log.error("failed to find credit alter resume by paymentCode = ({}), cause = {}",
                    paymentCode, paymentResp.getError());
            throw new JsonResponseException(500, paymentResp.getError());
        }
        return paymentResp.getResult();
    }

    /**
     * 判断是否已退款
     *
     * @param refundNo 退款单号
     * @return 结果
     */
    private boolean hadRefund(String refundNo) {
        Response<CreditAlterResume> alterResumeResp = creditAlterResumeReadService.findByRefundNo(refundNo);
        if (!alterResumeResp.isSuccess()) {
            log.error("failed to find credit alter resume by refundNo = ({}), cause = {}",
                    refundNo, alterResumeResp.getError());
            throw new JsonResponseException(500, alterResumeResp.getError());
        }
        return alterResumeResp.getResult() != null;
    }

    /**
     * 创建额度退款履历信息
     *
     * @param operator       操作人
     * @param orgAlterResume 原始履历信息
     * @param refundNo       退款单号
     * @param refundAmount   退款金额
     * @return 履历信息
     */
    private CreditAlterResume generateCreditAlterRefundResume(ParanaUser operator,
                                                              CreditAlterResume orgAlterResume,
                                                              String refundNo,
                                                              Long refundAmount) {
        CreditAlterResume resume = new CreditAlterResume();

        resume.setRefundNo(refundNo);  // 退款单
        resume.setRefundCode(getRandomNo()); // 信用额度退款单
        resume.setAlterValue(refundAmount); // 退款金额
        resume.setAlterType(CreditAlterType.PERSONAL_REFUND.value()); // 交易类型 -> 个人退款

        resume.setNewestCredit(orgAlterResume.getNewestCredit());
        resume.setTotalCredit(orgAlterResume.getTotalCredit());
        resume.setAvailableCredit(orgAlterResume.getNewestCredit());
        resume.setFineAmount(0L);
        resume.setOperateId(operator == null ? -1L : operator.getId());
        resume.setOperateName(operator == null ? "系统退款" : operator.getName());
        resume.setAlreadyPayment(orgAlterResume.getAlreadyPayment());
        resume.setRemainPayment(orgAlterResume.getRemainPayment());
        resume.setIsPaymentComplete(orgAlterResume.getIsPaymentComplete());
        resume.setAlterStatus(CreditAlterStatus.COMPLETE.value());
        resume.setShopId(orgAlterResume.getShopId());
        resume.setShopName(orgAlterResume.getShopName());
        resume.setUserId(orgAlterResume.getUserId());
        resume.setUserName(orgAlterResume.getUserName());

        return resume;
    }

    /**
     * 更新店铺信息
     *
     * @param orgShop     需要更新的店铺
     * @param alterResume 履历信息
     * @return 更新之后的信息
     */
    private VegaShopExtra updateShopExtra(VegaShopExtra orgShop, CreditAlterResume alterResume) {
        orgShop.setId(orgShop.getId());
        orgShop.setShopId(orgShop.getShopId());

        // totalCredit
        Long totalCredit = orgShop.getTotalCredit();
        Long availableCredit = 0L; // orgShop.getAvailableCredit();
//        if (alterResume.getIsPaymentComplete()) {
//            availableCredit = totalCredit; // 若已还款完成则可用额度全部恢复
//        }else {
        availableCredit = alterResume.getNewestCredit(); // 若未全部退款, 则可用额度为履历的最新信用额度
//        }
        orgShop.setTotalCredit(totalCredit);
        orgShop.setAvailableCredit(availableCredit);

        return orgShop;
    }

    /**
     * 更新专属会员信息
     *
     * @param shopUser    需要更新的专属会员信息
     * @param alterResume 履历信息
     */
    private void updateShopUser(ShopUser shopUser, CreditAlterResume alterResume) {
        Response<Boolean> booleanResponse = shopUserWriteService.updateShopUserCreditByUserId(shopUser.getUserId(), alterResume.getNewestCredit(), null);
        if (!booleanResponse.isSuccess()) {
            log.error("failed to update shop user available credit by userID = ({}), availableCredit = {}, " +
                    "cause : {}", shopUser.getUserId(), alterResume.getNewestCredit(), booleanResponse.getError());
            throw new JsonResponseException(500, booleanResponse.getError());
        }
    }

    /**
     * 更新信用额度履历信息
     *
     * @param user           用户信息
     * @param shopExtra      店铺信息
     * @param shopUser       专属会员信息
     * @param orgAlterResume 原始履历信息
     * @param refundAmount   退款金额
     * @return 更新后的履历信息
     */
    private CreditAlterResume updateAlterResume(ParanaUser user,
                                                VegaShopExtra shopExtra,
                                                ShopUser shopUser,
                                                CreditAlterResume orgAlterResume,
                                                Long refundAmount) {
        Long totalDebt = Math.abs(orgAlterResume.getRemainPayment()); // 总的欠款金额
        Long alreadyPayment = orgAlterResume.getAlreadyPayment(); // 已还款金额
        // Long newestCredit = orgAlterResume.getNewestCredit(); // 最新信用额度
        Long newestCredit;
        if (!Objects.isNull(shopExtra)) {
            newestCredit = shopExtra.getAvailableCredit(); // 最新信用额度应该以店铺为准, 因为可能被运营修改过。
            orgAlterResume.setTotalCredit(shopExtra.getTotalCredit());
        } else {
            newestCredit = shopUser.getAvailableCredit(); // 最新信用额度应该以专属会员信息为准, 因为可能被经销商修改过。
            orgAlterResume.setTotalCredit(shopUser.getTotalCredit());
        }

        Boolean isRepaymentComplete = false; // 是否还款完成
        Long remainPayment = 0L; // 剩余欠款金额

        totalDebt = totalDebt - refundAmount; // 新的总的欠款金额 = 原始欠的金额 - 退款金额
        if (totalDebt <= 0) {
            isRepaymentComplete = true;
            remainPayment = 0L; // 剩余的钱都退了, 则剩余未还款金额 = 0
        } else {
            isRepaymentComplete = false;
            remainPayment = totalDebt - alreadyPayment; // 剩余未还款金额 = 新的总欠款金额 - 已还款金额
        }
        // 不管什么情况, 可用额度都加上去
        newestCredit += refundAmount;

        orgAlterResume.setNewestCredit(newestCredit);
        orgAlterResume.setRemainPayment(remainPayment);
        orgAlterResume.setIsPaymentComplete(isRepaymentComplete);
        orgAlterResume.setAlterStatus(getAlterStatus(isRepaymentComplete));

        return orgAlterResume;
    }

    /**
     * 根据还款状态获取操作类型
     *
     * @param isRepaymentComplete 状态
     * @return 类型
     */
    private Integer getAlterStatus(Boolean isRepaymentComplete) {
        return isRepaymentComplete ?
                CreditAlterStatus.COMPLETE.value() : CreditAlterStatus.PART_REPAYMENT.value();
    }

    /**
     * 生成还款履历(退款)
     *
     * @param alterResume  信用额度履历
     * @param refundAmount 还款金额
     * @return 还款履历信息
     */
    private CreditRepaymentResume generateCreditRepaymentRefundResume(CreditAlterResume alterResume,
                                                                      Long refundAmount) {
        CreditRepaymentResume resume = new CreditRepaymentResume();
        resume.setAlterResumeId(alterResume.getId());
        resume.setFee(refundAmount);
        resume.setType(CreditRepaymentType.REFUND.value());
        resume.setStatus(CreditRepaymentStatus.PASS.value());

        Long remainAmount = alterResume.getRemainPayment();
        Long fineAmount = 0L;
        Long beyondAmount = 0L;

        log.info("remain = [{}], refund = [{}], remainAmount - refundAmount = [{}]", remainAmount, refundAmount,
                remainAmount - refundAmount);

        // TODO 拿到之前的超出额度, 再加上这次的超出额度, 才是最新的超出额度
        CreditRepaymentResume lastRepayment = findLastRepaymentResumeByAlterId(alterResume.getId());
        if (remainAmount - refundAmount <= 0) {
            beyondAmount = Math.abs(refundAmount - remainAmount);
            beyondAmount += lastRepayment.getBeyondAmount();
            remainAmount = 0L;
        } else {
            remainAmount = remainAmount - refundAmount;
        }

        resume.setLastDebtAmount(alterResume.getRemainPayment());
        resume.setRemainAmount(remainAmount);
        resume.setBeyondAmount(beyondAmount);
        resume.setFineAmount(fineAmount);

        return resume;
    }

    /**
     * 查询最新一次还款的超出额度
     *
     * @param id 额度履历ID
     * @return 还款履历ID
     */
    private CreditRepaymentResume findLastRepaymentResumeByAlterId(Long id) {
        Response<Optional<CreditRepaymentResume>> optResp =
                creditRepaymentResumeReadService.findLastRepaymentByAlterResumeId(id);
        if (!optResp.isSuccess()) {
            log.error("failed to find repayment by alterResumeId = {}, cause : {}", id, optResp.getError());
            throw new JsonResponseException(500, optResp.getError());
        }

        CreditRepaymentResume resume = null;
        Optional<CreditRepaymentResume> opt = optResp.getResult();
        if (opt.isPresent()) {
            resume = opt.get();
        } else {
            resume = new CreditRepaymentResume();
            resume.setBeyondAmount(0L);
        }
        return resume;
    }

    /**
     * 根据参数计算价格
     *
     * @param fee 参数
     * @return 价格
     */
    private Long getRefundAmount(String fee) {
        return NumberUtil.doubleMultipleByBase(fee, 100);
    }

    /**
     * 生成交易流水号
     * eg: 2014062108830107
     *
     * @return 流水号
     */
    private String getRandomNo() {
        String prefix = DATE.print(DateTime.now());
        String suffix = "0000000" + new Random().nextInt(100000);
        suffix = suffix.substring(suffix.length() - 7, suffix.length());
        return prefix + "0" + suffix;
    }


}
