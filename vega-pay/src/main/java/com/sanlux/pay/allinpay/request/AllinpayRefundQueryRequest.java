package com.sanlux.pay.allinpay.request;

import com.allinpay.ets.client.SecurityUtil;
import com.allinpay.ets.client.StringUtil;
import com.allinpay.ets.client.util.Base64;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.sanlux.pay.allinpay.enums.AlinnpayRefundHandleStatus;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import io.terminus.common.model.Response;
import io.terminus.pay.exception.PayException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * 封装支付请求参数
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 5:29 PM
 */
@Slf4j
public class AllinpayRefundQueryRequest extends AllinpayRequest {

    private AllinpayRefundQueryRequest(AllinpayToken allinpayToken) {
        super(allinpayToken);
        params.put("version", "v2.4");
        params.put("signType", "1");

    }

    public static AllinpayRefundQueryRequest build(AllinpayToken allinpayToken) {
        return new AllinpayRefundQueryRequest(allinpayToken);
    }


    public AllinpayRefundQueryRequest pc(){
        //params.put("service", "create_direct_pay_by_user");

        return this;
    }

    public AllinpayRefundQueryRequest wap(){
        //params.put("service", "alipay.wap.create.direct.pay.by.user");
        return this;
    }


    /**
     * 商户订单号
     * @param orderNo 商户订单号
     * @return this
     */
    public AllinpayRefundQueryRequest orderNo(String orderNo) {
        if(Strings.isNullOrEmpty(orderNo)){
            throw new PayException("allinpay.pay.order.no.empty");
        }
        params.put("orderNo", orderNo);
        return this;
    }

    /**
     * 退款单号
     * @param refundNo 商户退款单号
     * @return this
     */
    public AllinpayRefundQueryRequest refundNo(String refundNo) {

        params.put("mchtRefundOrderNo", refundNo);
        return this;
    }


    /**
     * 退款金额
     * @param refundAmount 退款金额
     * @return this
     */
    public AllinpayRefundQueryRequest refundAmount(Long refundAmount) {
        if(refundAmount==null){
            throw new PayException("allinpay.pay.refund.amount.empty");
        }
        params.put("refundAmount", refundAmount);
        return this;
    }



    /**
     * 退款受理时间
     * @param refundDatetime 退款受理时间
     * @return this
     */
    public AllinpayRefundQueryRequest refundDatetime(String refundDatetime) {
            params.put("refundDatetime", refundDatetime);
        return this;
    }

    @Override
    public String url() {
        sign();
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return allinpayToken.getQueryRefundGateway() + "?" + suffix;
    }

    /**
     * 向通联网关发送退货查询请求
     *
     * @return 退货查询请求结果
     */
    public Response<AlinnpayRefundHandleStatus> refundQuery() {
        String url = url();
        log.info("refund query url: {}", url);
        sign();
        String body = post(allinpayToken.getQueryRefundGateway(), params);
        log.info("refund query result: {}", body);
        return convertToResponse(body);
    }


    protected Response<AlinnpayRefundHandleStatus> convertToResponse(String body) {
        Response<AlinnpayRefundHandleStatus> result = new Response<AlinnpayRefundHandleStatus>();
        checkState(!Strings.isNullOrEmpty(body), "allinpay.refund.query.fail");



        Splitter.MapSplitter splitterMap = Splitter.on("&").withKeyValueSeparator("=");
        Map<String, String> map = splitterMap.split(body);

        //验签是商户为了验证接收到的报文数据确实是支付网关发送的。
        //构造订单结果对象，验证签名。
        if("".equals(map.get("ERRORCODE"))||null==map.get("ERRORCODE")) {
            //如果errorCode为空，说明返回正确退款报文信息，接下来对报文进行解析验签
            /**
             * v2.4|1|100020091218001|NO20161018194132|10|20161018093117|0007|TKSUCC0005
             * |20161019200500 dB2k9dvG35Zhb3kGF9fEFCPGRseKxgd6xVSW6+O0qIshlTJPEBxZ9sw+1b4MLdyiSoA3i2mHVyJydQ4+YrfvaxvGVkjtZd5Rn+T5PvPGxEthrTXtVAgzvK1FIiu7NoSUfIbW7g2mVlqhRNypE9DTyZpQUduyXGJqgFqLpwIx2I4=
             */
            Splitter splitter = Splitter.on("|");
            List<String> values = splitter.splitToList(body);
            log.info(values.toString());
            String checkResult = values.get(7);
            //这里暂不取退款完成时间,以当前job时间为退款完成时间

            Splitter splitter2 = Splitter.on("\r\n");
            List<String> values2 = splitter2.splitToList(body);

            String signString = values2.get(0);
            String signMsg = values2.get(1);

            if (verify(signString,signMsg)) {
                result.setResult(AlinnpayRefundHandleStatus.from(checkResult));
            }else{
                result.setError("验签失败");
            }
        }else {
            result.setError(map.get("ERRORMSG"));
        }
        return result;
    }

    @Override
    public void sign(){
        StringBuffer buffer = new StringBuffer();
        StringUtil.appendSignPara(buffer,"version", String.valueOf(params.get("version")));
        StringUtil.appendSignPara(buffer,"signType",String.valueOf(params.get("signType")));
        StringUtil.appendSignPara(buffer,"merchantId",String.valueOf(params.get("merchantId")));
        StringUtil.appendSignPara(buffer,"orderNo",String.valueOf(params.get("orderNo")));
        StringUtil.appendSignPara(buffer,"refundAmount",String.valueOf(params.get("refundAmount")));
        StringUtil.appendSignPara(buffer,"refundDatetime","");
        StringUtil.appendSignPara(buffer,"mchtRefundOrderNo",String.valueOf(params.get("mchtRefundOrderNo")));
        StringUtil.appendLastSignPara(buffer,"key",String.valueOf(params.get("key")));
        String signMsg= SecurityUtil.MD5Encode(buffer.toString());
        params.put("signMsg", signMsg);
    }

    private Boolean verify(String signString,String singMsg){
        String fileMd5String = SecurityUtil.MD5Encode(signString);
        return SecurityUtil.verifyByRSA(allinpayToken.getCertPath(),fileMd5String.getBytes(), Base64.decode(singMsg));
    }

    private String post(String url,Map<String, Object> params){
        return HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params).body();
    }

}
