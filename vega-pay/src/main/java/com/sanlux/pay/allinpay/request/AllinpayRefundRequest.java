package com.sanlux.pay.allinpay.request;

import com.allinpay.ets.client.PaymentResult;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import io.terminus.common.model.Response;
import io.terminus.pay.exception.PayException;
import lombok.extern.slf4j.Slf4j;

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
public class AllinpayRefundRequest extends AllinpayRequest {

    private AllinpayRefundRequest(AllinpayToken allinpayToken) {
        super(allinpayToken);
        params.put("version", "v2.3");
        params.put("signType", "0");

    }

    public static AllinpayRefundRequest build(AllinpayToken allinpayToken) {
        return new AllinpayRefundRequest(allinpayToken);
    }


    public AllinpayRefundRequest pc(){
        //params.put("service", "create_direct_pay_by_user");

        return this;
    }

    public AllinpayRefundRequest wap(){
        //params.put("service", "alipay.wap.create.direct.pay.by.user");
        return this;
    }


    /**
     * 商户订单号
     * @param orderNo 商户订单号
     * @return this
     */
    public AllinpayRefundRequest orderNo(String orderNo) {
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
    public AllinpayRefundRequest refundNo(String refundNo) {

        params.put("mchtRefundOrderNo", refundNo);
        return this;
    }


    /**
     * 退款金额
     * @param refundAmount 退款金额
     * @return this
     */
    public AllinpayRefundRequest refundAmount(Long refundAmount) {
        if(refundAmount==null){
            throw new PayException("allinpay.pay.refund.amount.empty");
        }
        params.put("refundAmount", refundAmount);
        return this;
    }



    /**
     * 订单提交时间
     * @param orderDatetime 订单提交时间
     * @return this
     */
    public AllinpayRefundRequest orderDatetime(String orderDatetime) {
            params.put("orderDatetime", orderDatetime);
        return this;
    }


    /**
     * 向通联网关发送退货请求
     *
     * @return 退货请求结果
     */
    public Response<Boolean> refund() {
        String url = super.url();
        log.info("allinpay refund url: {}", url);
        String body = post(super.urlRefund(), super.paramRefund());
        log.info("allinpay refund result: {}", body);
        return convertToResponse(body);
    }


    protected Response<Boolean> convertToResponse(String body) {
        Response<Boolean> result = new Response<Boolean>();
        if(Strings.isNullOrEmpty(body)){
            return Response.fail("退款请求成功但返回结果为空");
        }
        Splitter.MapSplitter splitter = Splitter.on("&").withKeyValueSeparator("=");
        Map<String, String> map = splitter.split(body);

        //验签是商户为了验证接收到的报文数据确实是支付网关发送的。
        //构造订单结果对象，验证签名。
        if("".equals(map.get("ERRORCODE"))||null==map.get("ERRORCODE")) {
            //如果errorCode为空，说明返回正确退款报文信息，接下来对报文进行解析验签
            PaymentResult paymentResult = JSON_MAPPER.fromJson(JSON_MAPPER.toJson(map), PaymentResult.class);
            paymentResult.setKey(allinpayToken.getKey());
            //验证签名：返回true代表验签成功；否则验签失败。
            boolean verifyResult = paymentResult.verify();
            if (verifyResult) {
                result.setResult(Boolean.TRUE);
            }else{
                result.setError("验签失败");
            }
        }else {
            result.setError(map.get("ERRORMSG"));
        }
        return result;
    }

    private String post(String url,Map<String, Object> params){
        return HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params).body();
    }

}
