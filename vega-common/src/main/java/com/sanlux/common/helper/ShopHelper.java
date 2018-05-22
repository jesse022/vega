/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.common.helper;

import com.sanlux.common.constants.SystemConstant;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author : panxin
 */
public class ShopHelper {

    /**
     * 计算罚息  剩余未还款金额 * 天数 * 利息比例(eg : 5/10000 万分之五)
     *
     * @param remain 剩余未还款金额
     * @param shouldPayCompleteDate 应还款日期
     * @param actualPaymentDate 实际还款日期
     * @param interest 罚息比率
     * @return 罚息
     */
    public static Long calculateFineAmount(Long remain,
                                              Date shouldPayCompleteDate,
                                              Date actualPaymentDate,
                                              Integer interest) {
        checkNotNull(interest, "interest.is.null");
        checkNotNull(remain, "remain.is.null");
        checkNotNull(actualPaymentDate, "actualPaymentDate.is.null");
        checkNotNull(shouldPayCompleteDate, "shouldPayCompleteDate.is.null");

        LocalDate currentDate = LocalDate.fromDateFields(actualPaymentDate); // 实际还款日期
        LocalDate startDate = LocalDate.fromDateFields(shouldPayCompleteDate); // 应还款日期
        Integer debtDays = Days.daysBetween(startDate, currentDate).getDays(); // 欠款天数

        // 还款日期未超出应还款日期则罚息为0
        return debtDays <= 0 ? 0 : (remain * debtDays * interest / SystemConstant.FINE_RATE);
    }

}
