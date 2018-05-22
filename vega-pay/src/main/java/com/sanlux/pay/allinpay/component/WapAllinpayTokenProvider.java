package com.sanlux.pay.allinpay.component;

import com.sanlux.pay.allinpay.token.WapAllinpayToken;
import io.terminus.pay.component.MultiTokenProvider;
import io.terminus.pay.constants.Tokens;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DATE: 16/9/5 下午4:54 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Component
public class WapAllinpayTokenProvider extends MultiTokenProvider<WapAllinpayToken> {

    @Autowired
    public WapAllinpayTokenProvider(WapAllinpayToken token){

            tokenMap.put(Tokens.DEFAULT_ACCOUNT, token);
    }

}
