package com.sanlux.pay.allinpay.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 6:47 PM
 */
@ConfigurationProperties(prefix = "pay.allinpay.wap.token")
@Data
public class WapAllinpayToken extends AllinpayToken {

    private static final long serialVersionUID = -5760883233216974413L;
}
