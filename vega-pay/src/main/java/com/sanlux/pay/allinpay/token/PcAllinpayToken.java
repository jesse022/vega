package com.sanlux.pay.allinpay.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 6:47 PM
 */
@ConfigurationProperties(prefix = "pay.allinpay.pc.token")
@Data
public class PcAllinpayToken extends AllinpayToken {

    private static final long serialVersionUID = -3429698812811940246L;
}
