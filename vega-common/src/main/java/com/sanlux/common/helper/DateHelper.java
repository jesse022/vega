/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.common.helper;

import com.google.common.base.Strings;
import com.sanlux.common.constants.SystemConstant;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * @author : panxin
 */
public class DateHelper {

    /**
     * 当前月的第一天
     * @return 年月信息 eg: 2016-8-1
     */
    public static String firstDayOfCurrentMonth() {
        DateTime now = DateTime.now();
        return now.getYear() + "-" + now.getMonthOfYear() + "-1";
    }

    /**
     * 下个月的第一天
     * @return 年月信息 eg: 2016-9-1
     */
    public static String firstDayOfTheNextMonth() {
        DateTime now = DateTime.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthOfYear();
        if (currentMonth == 12) {
            currentMonth = 0;
            currentYear += 1;
        }
        return currentYear + "-" + (currentMonth + 1) + "-1";
    }

    /**
     * 当前月的第一天
     * @return 年月信息 date
     */
    public static Date firstDateOfCurrentMonth() {
        return DateTime.parse(firstDayOfCurrentMonth()).toDate();
    }

    /**
     * 下个月的第一天
     * @return 年月信息 date
     */
    public static Date firstDateOfTheNextMonth() {
        return DateTime.parse(firstDayOfTheNextMonth()).toDate();
    }

    /**
     * 格式化时间 默认 yyyy-MM-dd
     * @param date 时间
     * @return date str
     */
    public static String formatDate(Date date) {
        return LocalDate.fromDateFields(date).toString(SystemConstant.DATE_PATTERN);
    }

    /**
     * 自定义格式格式化时间 默认 yyyy-MM-dd
     * @param date 时间
     * @param pattern 自定义时间格式
     * @return date str
     */
    public static String formatDate(Date date, @Nullable String pattern) {
        LocalDate target = LocalDate.fromDateFields(date);
        if (!Strings.isNullOrEmpty(pattern)) {
            return target.toString(pattern);
        }
        return target.toString(SystemConstant.DATE_PATTERN);
    }

    /**
     * 默认格式化时间
     * @param date 时间
     * @return 格式化之后的时间
     */
    public static Date format(Date date) {
        return DateTime.parse(formatDate(date)).toDate();
    }

    /**
     * 格式化时间 默认格式 yyyy-MM-dd
     * @param date 时间
     * @param pattern 需要的格式
     * @return 格式化后的时间
     */
    public static Date format(Date date, @Nullable String pattern) {
        return DateTime.parse(formatDate(date, pattern)).toDate();
    }

}
