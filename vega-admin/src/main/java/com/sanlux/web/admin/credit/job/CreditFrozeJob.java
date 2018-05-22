/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.credit.job;

import com.google.common.collect.Lists;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.VegaShopWriteService;
import com.sanlux.user.service.ShopUserWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.sanlux.common.helper.DateHelper.format;

/**
 * 自动冻结信用额度
 *
 * @author : panxin
 */
@Slf4j
@Component
public class CreditFrozeJob {

    // 每天早上8点? TODO
    private static final String crontab = "0 0 0 * * *";

    @RpcConsumer
    private VegaShopWriteService vegaShopWriteService;
    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private ShopUserWriteService shopUserWriteService;

    @Scheduled(cron = crontab)
    public void frozeCredit() {
        // 信用额度规定时间内未还款
        List<CreditAlterResume> resumeList = findCreditShouldRepaymentToday();
        List<Long> shopIds = findShopIdByResume(resumeList);
        List<Long> userIds = findUserIdByResume(resumeList);

        // 经销商
        if (shopIds != null && shopIds.size() > 0) {
            doFrozeShopCredit(shopIds);
        } else {
            log.info("there is no any shop that had not repayment credit today to froze it's credit .");
        }

        // 专属会员
        if (userIds != null && userIds.size() > 0) {
            doFrozeUserCredit(userIds);
        } else {
            log.info("there is no any user that had not repayment credit today to froze it's credit .");
        }
    }

    /**
     * 冻结操作
     *
     * @param shopIds 店铺ID
     */
    private void doFrozeShopCredit(List<Long> shopIds) {
        Response<Boolean> resp = vegaShopWriteService.batchFrozeShopCredit(shopIds);
        if (!resp.isSuccess()) {
            log.error("[FAILED] failed to froze shop credit by shopIds = [{}], cause : {}", shopIds, resp.getError());
        } else {
            log.info("[SUCCEED] froze shop credit succeed by shopIds = [{}]", shopIds);
        }
    }

    /**
     * 专属会员信用额度冻结操作
     *
     * @param userIds 专属会员ID
     */
    private void doFrozeUserCredit(List<Long> userIds) {
        Response<Boolean> resp = shopUserWriteService.batchFrozeUserCredit(userIds);
        if (!resp.isSuccess()) {
            log.error("[FAILED] failed to froze user credit by userIds = [{}], cause : {}", userIds, resp.getError());
        } else {
            log.info("[SUCCEED] froze user credit succeed by userIds = [{}]", userIds);
        }
    }

    /**
     * 获得需要冻结的店铺IDs
     *
     * @param resumeList 履历信息
     * @return 店铺ID
     */
    private List<Long> findShopIdByResume(List<CreditAlterResume> resumeList) {
        List<Long> shopIds = Lists.newArrayList();
        resumeList.stream().filter(resume -> !shopIds.contains(resume.getShopId()) && !Arguments.isNull(resume.getShopId())).forEach(resume -> {
            shopIds.add(resume.getShopId());
        });
        return shopIds;
    }

    /**
     * 获得需要冻结的专属会员IDs
     *
     * @param resumeList 履历信息
     * @return 店铺ID
     */
    private List<Long> findUserIdByResume(List<CreditAlterResume> resumeList) {
        List<Long> userIds = Lists.newArrayList();
        resumeList.stream().filter(resume -> !userIds.contains(resume.getUserId()) && !Arguments.isNull(resume.getUserId())).forEach(resume -> {
            userIds.add(resume.getUserId());
        });
        return userIds;
    }

    /**
     * 查询还款日期为今天但未还款的履历信息
     *
     * @return 履历信息
     */
    private List<CreditAlterResume> findCreditShouldRepaymentToday() {
        // 冻结前一天的 如:15号冻结14号应还款还未还款的
        Date now = format(DateTime.now().minusDays(1).toDate());
        Response<List<CreditAlterResume>> resp = creditAlterResumeReadService.listShouldRepaymentShops(now);
        if (!resp.isSuccess()) {
            log.error("failed to find credit should repayment date = [{}], cause : {}", now, resp.getError());
            return Collections.emptyList();
        }
        return resp.getResult();
    }

}
