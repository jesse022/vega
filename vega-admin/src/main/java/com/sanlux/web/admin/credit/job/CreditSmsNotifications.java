/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.credit.job;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.helper.DateHelper;
import com.sanlux.shop.dto.CreditRepaymentNotification;
import com.sanlux.shop.enums.CreditSmsNodeEnum;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.web.front.core.sms.SmsNodeSwitchParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.msg.exception.MsgException;
import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.MsgContext;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 信用额度还款短信提醒
 *
 * @author : panxin
 */
@Slf4j
@Component
public class CreditSmsNotifications {

    // 默认提前3天
    @Setter
    @Getter
    @Value("${credit.repayment.after-days: 3}")
    private Integer afterDays; // 当前日期加上该天数

    // 每天早上九点查询信息发送短信通知
    private final String crontab = "0 0 9 * * *";
    private Integer smsCount = 0;
    private final Integer SMS_COUNT_LIMIT = 10;

    @Autowired
    private MsgService msgService;
    @Autowired
    private SmsNodeSwitchParser smsNodeSwitchParser;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;

    @Scheduled(cron = crontab)
    public void notifyCreditRepayment() {
        final String templateName = CreditSmsNodeEnum.REPAYMENT.getName(); // 经销商模板
        final String templateName2 = CreditSmsNodeEnum.USER_REPAYMENT.getName(); // 专属会员模板

        if (!nodeIsOpen(CreditSmsNodeEnum.REPAYMENT)) {
            log.warn("failed to send sms to users that should repayment credit, cause [CreditSmsNodeEnum.REPAYMENT] = " +
                    "[{}] sms node is not open.", CreditSmsNodeEnum.REPAYMENT);
            return;
        }

        // 获取所有经销商和专属会员的信用额度履历
        generateCreditNotify().forEach(notification -> {

            String receiver = notification.getMobile();

            if (Strings.isNullOrEmpty(receiver)) {
                if (!Arguments.isNull(notification.getShopId())) {
                    log.error("failed to send sms to shopId = [{}], cause mobile is not exists.", notification.getShopId());
                } else {
                    log.error("failed to send sms to userId = [{}], cause mobile is not exists.", notification.getUserId());
                }
            } else {
                MsgContext context = MsgContext.of(
                        "tradeNo", notification.getTradeNo() // 订单编号
                );

                log.info("send sms receiver = [{}], now : [{}], deadline = [{}]", receiver,
                        DateTime.now().toString(SystemConstant.DATE_PATTERN), notification.getShouldRepaymentDate());

                doSendSms(receiver, Arguments.isNull(notification.getShopId()) ? templateName2 : templateName, context);
                smsCount++;
                if (Objects.equal(smsCount, SMS_COUNT_LIMIT)) {
                    smsCount = 0;
                    sleepAwhile(500);
                }
            }
        });
    }

    /**
     * tired ~
     *
     * @param millis 毫秒
     */
    private void sleepAwhile(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("[SEND SMS] failed to sleep, cause something unknown error occurred, cause : {}",
                    Throwables.getStackTraceAsString(e));
        }
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
            log.info("[credit repayment notification] sendSms result = {}, mobile = {}, message = {}",
                    result, receiver, context);
        } catch (MsgException e) {
            log.error("sms send failed, mobile = {}, template = {}, cause : {}",
                    receiver, template, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 生成通知信息(经销商+专属会员)
     *
     * @return 所有通知信息
     */
    private List<CreditRepaymentNotification> generateCreditNotify() {
        List<CreditRepaymentNotification> NotifyList = Lists.newArrayList();
        List<CreditAlterResume> resumeList = findCreditAlterResumes(afterDays);

        List<CreditRepaymentNotification> shopNotifyList = generateCreditNotify(resumeList);
        List<CreditRepaymentNotification> UserNotifyList = generateUserCreditNotify(resumeList);
        NotifyList.addAll(shopNotifyList);
        NotifyList.addAll(UserNotifyList);

        return NotifyList;
    }

    /**
     * 生成通知信息
     *
     * @param resumeList 需要发短信通知的信用额度履历
     * @return 通知信息
     */
    private List<CreditRepaymentNotification> generateCreditNotify(List<CreditAlterResume> resumeList) {
        // 拿到所有店铺ID
        List<Long> shopIds = Lists.newArrayList();

        resumeList.stream().filter(resume -> !shopIds.contains(resume.getShopId()) && !Arguments.isNull(resume.getShopId())).forEach(resume -> {
            shopIds.add(resume.getShopId());
        });

        List<Shop> shopList = findShopsByIds(shopIds);

        return generateCreditNotify(resumeList, shopList, shopIds);
    }

    /**
     * 生成专属会员通知信息
     *
     * @param resumeList 需要发短信通知的信用额度履历
     * @return 通知信息
     */
    private List<CreditRepaymentNotification> generateUserCreditNotify(List<CreditAlterResume> resumeList) {
        // 拿到所有用户ID
        List<Long> userIds = Lists.newArrayList();

        resumeList.stream().filter(resume -> !userIds.contains(resume.getUserId()) && !Arguments.isNull(resume.getUserId())).forEach(resume -> {
            userIds.add(resume.getUserId());
        });

        List<User> userList = findUsersByIds(userIds);

        return generateUserCreditNotify(resumeList, userList, userIds);
    }

    /**
     * 生成通知信息
     *
     * @param resumeList 欠款履历信息
     * @param shopList   店铺信息
     * @param shopIds    店铺IDs
     * @return 通知信息
     */
    private List<CreditRepaymentNotification> generateCreditNotify(List<CreditAlterResume> resumeList,
                                                                   List<Shop> shopList,
                                                                   List<Long> shopIds) {
        List<CreditRepaymentNotification> notificationList = Lists.newArrayList();

        Map<Long, Shop> shopMap = Maps.uniqueIndex(shopList, Shop::getId);

        // foreach resumeList and filter the shop in resumeList which shopId contains in shopIds
        resumeList.stream().filter(resume -> shopIds.contains(resume.getShopId())).forEach(resume -> {
            Shop shop = shopMap.get(resume.getShopId());
            notificationList.add(generateCreditNotify(shop, resume));
        });
        return notificationList;
    }

    /**
     * 生成专属会员通知信息
     *
     * @param resumeList 欠款履历信息
     * @param userList   会员信息
     * @param userIds    会员Ds
     * @return 通知信息
     */
    private List<CreditRepaymentNotification> generateUserCreditNotify(List<CreditAlterResume> resumeList,
                                                                   List<User> userList,
                                                                   List<Long> userIds) {
        List<CreditRepaymentNotification> notificationList = Lists.newArrayList();

        Map<Long, User> userMap = Maps.uniqueIndex(userList, User::getId);

        // foreach resumeList and filter the user in resumeList which userId contains in userIds
        resumeList.stream().filter(resume -> userIds.contains(resume.getUserId())).forEach(resume -> {
            User user = userMap.get(resume.getUserId());
            notificationList.add(generateUserCreditNotify(user, resume));
        });
        return notificationList;
    }

    /**
     * 生成通知信息
     *
     * @param shop   店铺信息
     * @param resume 额度履历信息
     * @return 通知信息
     */
    private CreditRepaymentNotification generateCreditNotify(Shop shop, CreditAlterResume resume) {
        CreditRepaymentNotification notification = new CreditRepaymentNotification();

        // 消息接收者信息
        notification.setShopId(shop.getId());
        notification.setMobile(findMobileByShop(shop));

        // 消息上下文信息
        notification.setTradeNo(resume.getTradeNo());
        notification.setShouldRepaymentAmount(Math.abs(resume.getAlterValue()));
        notification.setRemainRepayment(resume.getRemainPayment());
        notification.setFineAmount(resume.getFineAmount() == null ? 0 : resume.getFineAmount());
        notification.setShouldRepaymentDate(DateHelper.formatDate(resume.getShouldRepaymentDate()));

        return notification;
    }

    /**
     * 生成专属会员通知信息
     *
     * @param user   会员信息
     * @param resume 额度履历信息
     * @return 通知信息
     */
    private CreditRepaymentNotification generateUserCreditNotify(User user, CreditAlterResume resume) {
        CreditRepaymentNotification notification = new CreditRepaymentNotification();

        // 消息接收者信息
        notification.setUserId(user.getId());
        notification.setMobile(findMobileByUser(user));

        // 消息上下文信息
        notification.setTradeNo(resume.getTradeNo());
        notification.setShouldRepaymentAmount(Math.abs(resume.getAlterValue()));
        notification.setRemainRepayment(resume.getRemainPayment());
        notification.setFineAmount(resume.getFineAmount() == null ? 0 : resume.getFineAmount());
        notification.setShouldRepaymentDate(DateHelper.formatDate(resume.getShouldRepaymentDate()));

        return notification;
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
     * 查找会员手机号
     *
     * @param user 会员信息
     * @return     会员手机号
     */
    private String findMobileByUser(User user) {
        return Strings.isNullOrEmpty(user.getMobile()) ? findUserById(user.getId()).getMobile() : user.getMobile();
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
     * 通过店铺ID查询店铺信息
     *
     * @param shopIds 店铺IDs
     * @return 店铺信息
     */
    private List<Shop> findShopsByIds(List<Long> shopIds) {
        Response<List<Shop>> resp = shopReadService.findByIds(shopIds);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by ids = [{}], cause : {}", shopIds, resp.getError());
            return Collections.emptyList();
        }
        return resp.getResult();
    }

    /**
     * 通过用户ID查询用户信息
     *
     * @param userIds 用户IDs
     * @return 用户信息
     */
    private List<User> findUsersByIds(List<Long> userIds) {
        Response<List<User>> resp = userReadService.findByIds(userIds);
        if (!resp.isSuccess()) {
            log.error("failed to find user by ids = [{}], cause : {}", userIds, resp.getError());
            return Collections.emptyList();
        }
        return resp.getResult();
    }

    /**
     * 根据需要提前多少天还款查询需要还款的履历信息
     *
     * @param afterDays 提前天数
     * @return 履历信息
     */
    private List<CreditAlterResume> findCreditAlterResumes(Integer afterDays) {
        // 需还款日期
        String source = DateTime.now().plusDays(afterDays).toString(SystemConstant.DATE_PATTERN);
        Date shouldRepaymentDate = DateTime.parse(source).toDate();
        Response<List<CreditAlterResume>> resp =
                creditAlterResumeReadService.listShouldRepaymentShops(shouldRepaymentDate);
        if (!resp.isSuccess()) {
            log.error("failed to find credit alter that should repayment before Date = [{}], cause : {}",
                    shouldRepaymentDate, resp.getError());
            return Collections.emptyList();
        }
        return resp.getResult();
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

}
