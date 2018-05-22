/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.sanlux.pay.credit.request;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-25 9:37 AM  <br>
 * Author: xiao
 */
public class Request {
    private final static Logger log = LoggerFactory.getLogger(Request.class);
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");
    protected Map<String, Object> params = Maps.newTreeMap();
    protected CreditPayToken creditPayToken;

    private static XStream xStream;

    static {
        xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(CreditPaySyncResponse.class);
    }

    protected Request(CreditPayToken creditPayToken) {
        params.put("_input_charset", "utf-8");
        this.creditPayToken = creditPayToken;
    }

    public Map<String, Object> param() {
        return params;
    }

    public String url() {
        sign();
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return creditPayToken.getGateway() + "?" + suffix;
    }

    public String refundUrl() {
        sign();
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return creditPayToken.getRefundGateway() + "?" + suffix;
    }

    /**
     * 将支付宝请求返回报文转换为java对象
     * @param body xml返回报文
     * @return 是否请求成功
     */
    protected Boolean convertToResponse(String body) {
        checkState(!Strings.isNullOrEmpty(body), "creditPay.refund.fail");

        CreditPaySyncResponse refundResponse = (CreditPaySyncResponse)xStream.fromXML(body);
        if (refundResponse.isSuccess()) {
            return Boolean.TRUE;
        } else {
            log.error("refund raise fail: {}", refundResponse.getError());
            return Boolean.FALSE;
        }
    }


    /**
     * 对参数列表进行签名
     */
    public void sign() {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);

            log.info("request params toVerify = [{}]", toVerify);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(creditPayToken.getKey(), Charsets.UTF_8)
                    .hash()
                    .toString();

            log.info("request sign result = [{}]", sign);

            params.put("sign", sign);
            params.put("sign_type", "MD5");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证签名
     * @param params    参数
     * @param sign      签名
     * @return  校验通过
     */
    public static boolean verify(Map<String, String> params, String sign, CreditPayToken creditPayToken) throws UnsupportedEncodingException {
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);


        String expect = Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString(creditPayToken.getKey(), Charsets.UTF_8)
                .hash()
                .toString();

        final boolean isSignMatch = Objects.equal(expect, sign);
        if(!isSignMatch){
            log.error("creditPay sign mismatch, expected ({}), actual({}), toVerify is:{}", expect, sign, toVerify);
        } else {
            log.info("creditPay sign matched, expected ({}), actual({}), toVerify is:{}", expect, sign, toVerify);
        }
        return isSignMatch;
    }


}
