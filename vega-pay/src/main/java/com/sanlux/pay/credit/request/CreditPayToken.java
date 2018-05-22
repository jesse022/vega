/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.pay.credit.request;

import io.terminus.pay.model.Token;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author : panxin
 */
@Data
@ConfigurationProperties(prefix = "credit-pay")
public class CreditPayToken extends Token {

    /**
     * pid
     */
    private String pid;

    /**
     * 密钥
     */
    private String key;

    /**
     * 收款账户
     */
    private String account;

    /**
     * 移动端
     */
    private String wapReturnUrl;

    /**
     * PC
     */
    private String pcReturnUrl;

}
