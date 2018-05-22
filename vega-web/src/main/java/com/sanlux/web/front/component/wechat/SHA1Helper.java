package com.sanlux.web.front.component.wechat;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Copyright (c) 2015 杭州端点网络科技有限公司
 * Date:2016-01-20
 * Time:15:25
 * Author: 2015年 <a href="zhougl@terminus.io">周高磊</a>
 * Desc:
 */
public class SHA1Helper {
    private static Joiner BLANK = Joiner.on("").skipNulls();
    private static MessageDigest messageDigest = null;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String getSortedSHA1(String... parts) {
        return getSortedSHA1(null, parts);
    }

    public static String getSortedSHA1(String splitter, String[] parts) {
        Arrays.sort(parts);
        String toDigest;
        if (Strings.isNullOrEmpty(splitter)) {
            toDigest = BLANK.join(Arrays.asList(parts));
        } else {
            toDigest = Joiner.on(splitter).skipNulls().join(Arrays.asList(parts));
        }
        byte[] digest = messageDigest.digest(toDigest.getBytes());
        return BaseEncoding.base16().encode(digest).toLowerCase();
    }
}
