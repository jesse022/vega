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
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.pay.credit.request.CreditPayToken;
import com.sanlux.shop.enums.CreditAlterStatus;
import com.sanlux.shop.enums.CreditAlterType;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.CreditAlterResumeWriteService;
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
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.NumberUtil;
import io.terminus.parana.common.utils.UserUtil;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static com.sanlux.pay.credit.constants.CreditPayParams.NOTIFY_URL;
import static com.sanlux.pay.credit.constants.CreditPayParams.OUT_TRADE_NO;
import static com.sanlux.pay.credit.constants.CreditPayParams.RETURN_URL;
import static com.sanlux.pay.credit.constants.CreditPayParams.SELLER_NO;
import static com.sanlux.pay.credit.constants.CreditPayParams.TOTAL_FEE;


/**
 * @author : panxin
 */
@Slf4j
@Controller
@RequestMapping("/api/vega/credit")
public class CreditPayments {

    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private CreditAlterResumeWriteService creditAlterResumeWriteService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;
    @RpcConsumer
    private ShopUserReadService shopUserReadService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;

    @Autowired
    private TokenProvider<CreditPayToken> creditPayTokenProvider;
    @Autowired
    private ChannelRegistry channelRegistry;

    private DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private DateTimeFormatter DFT_TIME = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter DATE = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    /**
     * 信用额度支付请求
     *
     * @return 支付结果
     */
    @RequestMapping(value = "/pay", method = RequestMethod.GET)
    public String pay(HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("credit pay context:[{}]", request.getParameterMap());

            String notifyUrl = request.getParameter(NOTIFY_URL); // 异步通知地址
            String returnUrl = request.getParameter(RETURN_URL); // 返回url
            String outTradeNo = request.getParameter(OUT_TRADE_NO); // 支付请求号
            String fee = request.getParameter(TOTAL_FEE); // 支付金额
            String sellerNo = request.getParameter(SELLER_NO); // 商家编码

            Map<String, Object> params = Maps.newTreeMap();
            params.put("is_success", "T");
            params.put("out_trade_no", outTradeNo);
            params.put("trade_no", getRandomNo());
            params.put("trade_status", "TRADE_SUCCESS");
            params.put("notify_id", getRandomNo());
            params.put("sellerNo", sellerNo);
            params.put("gmt_payment", DFT_TIME.print(DateTime.now()));

            CreditPayToken creditPayToken = creditPayTokenProvider.findToken(Tokens.DEFAULT_ACCOUNT);

            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(creditPayToken.getKey(), Charsets.UTF_8).hash().toString();

            //验签
            PayChannel paymentChannel = channelRegistry.findChannel("credit-pay");
            paymentChannel.verify(request);

            String paymentCode = String.valueOf(params.get("trade_no"));
            // 查询信用额度 并创建消费类型的履历
            Long payAmount = getPaymentValue(fee);

            checkCreditValid(outTradeNo, paymentCode, payAmount);

            params.put("sign", sign);
            params.put("sign_type", CreditPayConstants.SIGN_TYPE);

            params.put("gmt_payment", URLEncoder.encode(params.get("gmt_payment").toString(), "utf-8"));
            String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);

            String result = HttpRequest.post(notifyUrl + "?" + suffix).connectTimeout(1000000).readTimeout(1000000).body();

            log.info("[Credit pay Server] Async pay result notify " +
                    "\n\t url:   [{}]" +
                    "\n\t params:[{}]" +
                    "\n\t result:[{}]", notifyUrl, params, result);

            // TODO: 8/31/16 how to handle the response, cause redirect failed "redirect:http://xxx.xxx.xxx/xxx/xx"
            if ("fail".equals(result)) {
                return result;
            } else {
                response.sendRedirect(returnUrl);
                return "success";
            }
        } catch (Exception e) {
            log.error("credit pay failed, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "credit.pay.failed");
        }
    }

    /**
     * 根据参数计算价格
     *
     * @param fee 参数
     * @return 价格
     */
    private Long getPaymentValue(String fee) {
        return NumberUtil.doubleMultipleByBase(fee, 100);
    }

    /**
     * 数据校验, 生成信用额度履历信息
     *
     * @param tradeNo     交易流水号
     * @param paymentCode 第三方交易流水号
     * @param creditValue 消费的信用额度
     */
    private void checkCreditValid(String tradeNo, String paymentCode, Long creditValue) {
        Response<CreditAlterResume> tradeNoResp = creditAlterResumeReadService.findByTradeNo(tradeNo);
        // 判断是否已记录改支付单信息相关的信用额度履历
        if (tradeNoResp.isSuccess() && tradeNoResp.getResult() != null) {
            log.warn("could not create credit alter resume, cause tradeNo = ({}) already exists.", tradeNo);
            return;
        }

        ParanaUser user = UserUtil.getCurrentUser();
        if (!Arguments.isNull(user.getShopId())) {
            //店铺买家
            checkCreditValidByShop(tradeNo, paymentCode, creditValue, user);
        } else {
            //普通专属会员买家
            checkCreditValidByUser(tradeNo, paymentCode, creditValue, user);
        }
    }

    /**
     * 店铺买家
     *
     * @param tradeNo     交易流水号
     * @param paymentCode 第三方交易流水号
     * @param creditValue 消费的信用额度
     * @param user 用户信息
     */
    private void checkCreditValidByShop(String tradeNo, String paymentCode, Long creditValue, ParanaUser user) {
        // 查找店铺信息
        Response<VegaShopExtra> shopExtraResp = vegaShopReadService.findVegaShopExtraByUserId(user.getId());
        if (!shopExtraResp.isSuccess()) {
            log.error("failed to find shop extra by current user userId = ({}), cause : {}", user.getId(), shopExtraResp.getError());
            throw new JsonResponseException(500, shopExtraResp.getError());
        }

        // creditValue *= 100;
        // 设置履历信息
        VegaShopExtra shopExtra = shopExtraResp.getResult();
        CreditAlterResume resume = generateAlterResume(user, shopExtra, null, tradeNo, paymentCode, creditValue);

        // 所属店铺更新额度信息
        VegaShopExtra toUpdateVegaShopExtra = new VegaShopExtra();
        BeanMapper.copy(shopExtra, toUpdateVegaShopExtra);
        toUpdateVegaShopExtra.setAvailableCredit(resume.getNewestCredit());

        resume.setAvailableCredit(toUpdateVegaShopExtra.getAvailableCredit());
        resume.setTotalCredit(toUpdateVegaShopExtra.getTotalCredit());

        Response<Boolean> resp = creditAlterResumeWriteService.create(resume, toUpdateVegaShopExtra);
        if (!resp.isSuccess()) {
            log.error("failed to create credit alter resume by user = ({}), tradeNo = {}, creditValue = {}, " +
                    "cause : {}", user, tradeNo, creditValue, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
    }

    /**
     * 专属会员买家
     *
     * @param tradeNo     交易流水号
     * @param paymentCode 第三方交易流水号
     * @param creditValue 消费的信用额度
     * @param user 用户信息
     */
    private void checkCreditValidByUser(String tradeNo, String paymentCode, Long creditValue, ParanaUser user) {
        Response<Optional<ShopUser>> shopUserResponse = shopUserReadService.findShopUserByUserId(user.getId());
        if (!shopUserResponse.isSuccess()) {
            log.error("find shop user  fail,userId:{},cause:{}", user.getId(), shopUserResponse.getError());
            throw new JsonResponseException(500, shopUserResponse.getError());
        }
        ShopUser shopUser = null;
        Optional<ShopUser> shopUserOptional = shopUserResponse.getResult();
        if (shopUserOptional.isPresent()) {
            shopUser = shopUserResponse.getResult().get();
        }
        CreditAlterResume resume = generateAlterResume(user, null, shopUser, tradeNo, paymentCode, creditValue);

        // 所属会员更新额度信息
        ShopUser toUpdateShopUser = new ShopUser();
        BeanMapper.copy(shopUser, toUpdateShopUser);
        toUpdateShopUser.setAvailableCredit(resume.getNewestCredit());

        resume.setAvailableCredit(toUpdateShopUser.getAvailableCredit());
        resume.setTotalCredit(toUpdateShopUser.getTotalCredit());

        Response<Long> resp = creditAlterResumeWriteService.create(resume);
        if (!resp.isSuccess()) {
            log.error("failed to create credit alter resume by user = ({}), tradeNo = {}, creditValue = {}, " +
                    "cause : {}", user, tradeNo, creditValue, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        Response<Boolean> booleanResponse = shopUserWriteService.updateShopUserCreditByUserId(toUpdateShopUser.getUserId(), toUpdateShopUser.getAvailableCredit(), null);
        if (!booleanResponse.isSuccess()) {
            log.error("failed to update shop user available credit by userID = ({}), availableCredit = {}, " +
                    "cause : {}", toUpdateShopUser.getUserId(), toUpdateShopUser.getAvailableCredit(), booleanResponse.getError());
            throw new JsonResponseException(500, booleanResponse.getError());
        }
    }

    /**
     * 生成额度履历信息
     *
     * @param user        当前登录用户
     * @param shopExtra   店铺信息
     * @param shopUser    专属会员信息
     * @param tradeNo     交易流水号
     * @param paymentCode 第三方交易流水号
     * @param creditValue 变更额度
     * @return 履历信息
     */
    private CreditAlterResume generateAlterResume(ParanaUser user,
                                                  VegaShopExtra shopExtra,
                                                  ShopUser shopUser,
                                                  String tradeNo,
                                                  String paymentCode,
                                                  Long creditValue) {
        // 消费前的额度信息
        Long originalCredit;
        if (!Objects.isNull(shopExtra)) {
            originalCredit = shopExtra.getAvailableCredit();
        } else {
            originalCredit = shopUser.getAvailableCredit();
        }
        // 消费后的额度信息
        Long newestCredit = originalCredit - creditValue;
        if (newestCredit < 0) {
            log.error("failed to change credit by shopId = {}, to update credit value = {}, cause the credit could not" +
                    " less than zero.", shopExtra.getShopId(), creditValue);
            throw new JsonResponseException(500, "credit.value.less.than.zero");
        }

        CreditAlterResume resume = new CreditAlterResume();
        // 操作人
        if (!Objects.isNull(shopExtra)) {
            resume.setShopId(shopExtra.getShopId());
            resume.setShopName(shopExtra.getShopName());
        } else {
            resume.setUserId(shopUser.getUserId());
            resume.setUserName(shopUser.getUserName());
        }
        resume.setOperateId(user.getId());
        resume.setOperateName(user.getName());

        // 操作信息, 待还款金额就是消费金额
        resume.setTradeNo(tradeNo);
        resume.setRemainPayment(creditValue);

        // 个人消费则变更额度为负值
        resume.setAlterValue(-creditValue);
        resume.setAlterType(CreditAlterType.PERSONAL_CONSUME.value());
        resume.setAlterStatus(CreditAlterStatus.WAIT.value());
        resume.setIsPaymentComplete(false);

        // 最新额度信息
        resume.setLastCredit(originalCredit);
        resume.setNewestCredit(newestCredit);

        // 交易信息
        resume.setPaymentCode(paymentCode);
        if (!Objects.isNull(shopExtra)) {
            resume.setShouldRepaymentDate(calculateShouldRepaymentDate(shopExtra));
        } else {
            resume.setShouldRepaymentDate(calculateShouldRepaymentDate(shopUser));
        }
        resume.setAlreadyPayment(0L);

        return resume;
    }

    /**
     * 计算应还款日期
     *
     * @return 还款日期
     */
    private Date calculateShouldRepaymentDate(VegaShopExtra shopExtra) {
        DateTime now = DateTime.now();
        Integer repaymentDays = shopExtra.getCreditPaymentDays();
        return DateTime.parse(now.plusDays(repaymentDays).toString(SystemConstant.DATE_PATTERN)).toDate();
    }

    /**
     * 计算普通会员应还款日期
     *
     * @return 还款日期
     */
    private Date calculateShouldRepaymentDate(ShopUser shopUser) {
        DateTime now = DateTime.now();
        Integer repaymentDays = shopUser.getCreditPaymentDays();
        return DateTime.parse(now.plusDays(repaymentDays).toString(SystemConstant.DATE_PATTERN)).toDate();
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
