/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.user.util;

import java.util.UUID;

/**
 * @author : panxin
 */
public final class PasswordGenerator {

    /**
     * 获取默认密码(未加密)
     *
     * @return 默认密码: 123456
     */
    public static String defaultPassword() {
        return "123456";
    }

    /**
     * 随机密码(未加密)
     *
     * @return 随机密码
     */
    public static String generateDynamicPassword() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

}
