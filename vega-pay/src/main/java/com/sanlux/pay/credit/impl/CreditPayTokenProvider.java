package com.sanlux.pay.credit.impl;

import com.sanlux.pay.credit.request.CreditPayToken;
import io.terminus.pay.component.MultiTokenProvider;
import io.terminus.pay.constants.Tokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DATE: 16/6/6 下午1:50 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Component
@Slf4j
public class CreditPayTokenProvider extends MultiTokenProvider<CreditPayToken> {

    @Autowired
    public CreditPayTokenProvider(CreditPayToken token){
        tokenMap.put(Tokens.DEFAULT_ACCOUNT, token);
    }
}
