package com.sanlux.pay.allinpay.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 6:47 PM
 */
@ConfigurationProperties(prefix = "pay.allinpay.app.token")
@Data
public class AppAllinpayToken extends AllinpayToken {

    private static final long serialVersionUID = 7332099685332923905L;
}
