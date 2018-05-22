package com.sanlux.pay.allinpay.token;

import io.terminus.pay.model.Token;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 5:05 PM
 */
@Data
public class AllinpayToken extends Token implements Serializable{
    private static final long serialVersionUID = 5462784650561689965L;

    /**
     * 商户号
     */
    private String merchantId;

    /**
     * 密钥
     */
    private String key;

    /**
     * 证书路径
     */
    private String certPath;


    /**
     * wap支付网关
     */
    private String wapPayGateway;




    public AllinpayToken(){
        /**
         * 网关host
         * 测试 ceshi.allinpay.com
         */
        String gatewayHost ="service.allinpay.com";
        String gateway="https://"+gatewayHost+"/gateway/index.do";
        String wapPayGateway ="https://"+gatewayHost+"/mobilepayment/mobile/SaveMchtOrderServlet.action";
        String queryRefundGateway="https://"+gatewayHost+"/mchtoq/refundQuery";
        String billGateway="https://"+gatewayHost+"/member/checkfiledown/CheckFileDownLoad/checkfileDownLoad.do";
        setGateway(gateway);
        setQueryTradeGateway(gateway);
        setRefundGateway(gateway);
        setQueryRefundGateway(queryRefundGateway);
        setBillGateway(billGateway);
        setWapPayGateway(wapPayGateway);
    }

}
