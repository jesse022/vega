package com.sanlux.pay.allinpay.request;

import com.allinpay.ets.client.RequestOrder;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-25 9:37 AM  <br>
 * Author: xiao
 */
@Slf4j
public class AllinpayRequest {
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    protected Map<String, Object> params = Maps.newTreeMap();
    protected AllinpayToken allinpayToken;
    protected final static JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();


    protected AllinpayRequest(AllinpayToken allinpayToken) {

        params.put("merchantId", allinpayToken.getMerchantId());
        params.put("key", allinpayToken.getKey());
        params.put("inputCharset", "1");
        params.put("orderCurrency", "0");//币种
        params.put("language", "1");


        this.allinpayToken = allinpayToken;
    }

    public Map<String, Object> param() {
        return params;
    }

    public String url() {
        sign();
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return allinpayToken.getGateway() + "?" + suffix;
    }

    /**
     * 退款接口参数
     */
    public Map<String, Object> paramRefund() {
        sign();
        return params;
    }

    /**
     * 退款接口url
     */
    public String urlRefund() {
        return allinpayToken.getGateway();
    }

    /**
     * 对参数列表进行签名
     */
    public void sign() {
        try {

            RequestOrder requestOrder = JSON_MAPPER.fromJson(JSON_MAPPER.toJson(params), RequestOrder.class);
            params.put("signMsg", requestOrder.doSign());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
