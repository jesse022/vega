package com.sanlux.pay.allinpay.paychannel;

import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.token.AppAllinpayToken;
import com.sanlux.pay.allinpay.token.WapAllinpayToken;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.service.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DATE: 16/8/22 下午2:26 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class AppAllinpayChannel extends AllinpayChannel<AppAllinpayToken> implements PayChannel {

    @Autowired
    public AppAllinpayChannel(TokenProvider<AppAllinpayToken> tokenProvider) {
        super(tokenProvider, AllinpayChannels.WAP);
    }
}
