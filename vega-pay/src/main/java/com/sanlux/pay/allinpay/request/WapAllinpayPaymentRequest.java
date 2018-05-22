package com.sanlux.pay.allinpay.request;

import com.google.common.base.Strings;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import io.terminus.pay.exception.PayException;

/**
 * 封装支付请求参数
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 5:29 PM
 */
public class WapAllinpayPaymentRequest extends AllinpayRequest {


    private WapAllinpayPaymentRequest(AllinpayToken allinpayToken) {
        super(allinpayToken);
        params.put("payType", "10");
        params.put("version", "v1.0");
        params.put("signType", "1");



    }

    public static WapAllinpayPaymentRequest build(AllinpayToken allinpayToken) {
        return new WapAllinpayPaymentRequest(allinpayToken);
    }


    /**
     * 付款人姓名
     * @param payerName 付款人姓名
     * @return this
     */
    public WapAllinpayPaymentRequest payerName(String payerName){
        params.put("payerName", payerName);
        return this;
    }

    /**
     * 付款人邮件联 系方式
     * @param payerEmail 付款人邮件联 系方式
     * @return this
     */
    public WapAllinpayPaymentRequest payerEmail(String payerEmail){
        params.put("payerEmail", payerEmail);
        return this;
    }


    /**
     * 付款人电话联 系方式
     * @param payerTelephone 付款人电话联 系方式
     * @return this
     */
    public WapAllinpayPaymentRequest payerTelephone(String payerTelephone){
        params.put("payerTelephone", payerTelephone);
        return this;
    }

    /**
     * 商品价格
     * @param productPrice 商品价格
     * @return this
     */
    public WapAllinpayPaymentRequest productPrice(String productPrice){
        params.put("productPrice", productPrice);
        return this;
    }

    /**
     * 商品数量
     * @param productNum 商品数量
     * @return this
     */
    public WapAllinpayPaymentRequest productNum(String productNum){
        params.put("productNum", productNum);
        return this;
    }

    /**
     * 商品代码
     * @param productId 商品代码
     * @return this
     */
    public WapAllinpayPaymentRequest productId(String productId){
        params.put("productId", productId);
        return this;
    }


    /**
     * 前台返回URL
     *
     * @param forward 支付成功后的转发路径
     * @return this
     */
    public WapAllinpayPaymentRequest forward(String forward,String systemNo) {
        if (!Strings.isNullOrEmpty(forward)) {
            params.put("pickupUrl", forward);
        }
        return this;
    }

    /**
     * 后台通知URL
     *
     * @param notify 步后台通知
     * @return this
     */
    public WapAllinpayPaymentRequest notify(String notify) {
        if (!Strings.isNullOrEmpty(notify)) {
            params.put("receiveUrl", notify);
        }
        return this;
    }

    /**
     * 商户订单号
     * @param orderNo 商户订单号
     * @return this
     */
    public WapAllinpayPaymentRequest orderNo(String orderNo) {
        if(Strings.isNullOrEmpty(orderNo)){
            throw new PayException("allinpay.pay.order.no.empty");
        }
        params.put("orderNo", orderNo);
        return this;
    }

    /**
     * 订单金额
     * @param total 订单金额
     * @return this
     */
    public WapAllinpayPaymentRequest total(Integer total) {
        if(total==null){
            throw new PayException("allinpay.pay.total.empty");
        }
       // String fee = DECIMAL_FORMAT.format(total / 100.0);
        params.put("orderAmount", total);
        return this;
    }



    /**
     * 订单提交时间
     * @param orderDatetime 订单提交时间
     * @return this
     */
    public WapAllinpayPaymentRequest orderDatetime(String orderDatetime) {
            params.put("orderDatetime", orderDatetime);
        return this;
    }



    /**
     * 支付超时分钟数 最大值为 9999 分钟
     *
     * @param minutes 分钟
     * @return this
     */
    public WapAllinpayPaymentRequest orderExpireDatetime(int minutes) {
        if (minutes > 0 && minutes <= 9999) {
            params.put("orderExpireDatetime", minutes);
        }
        return this;
    }


    /**
     * 收银台上显示的商品标题
     *
     * @param title 商品标题
     */
    public WapAllinpayPaymentRequest title(String title) {
        if (title != null) {
            params.put("productName", title);
        }
        return this;
    }


    /**
     * 商品内容
     *
     * @param content 商品内容
     */
    public WapAllinpayPaymentRequest content(String content) {
        if (!Strings.isNullOrEmpty(content)) {
            params.put("productDesc", content);
        }

        return this;
    }



    public String pay() {
        return super.url();
    }


    /**
     * 扩展字段
     * @param ext1 扩展字段
     */
    public WapAllinpayPaymentRequest ext1(String ext1) {
        if (ext1 != null) {
            params.put("ext1", ext1);
        }
        return this;
    }

    /**
     * 扩展字段
     * @param ext2 扩展字段
     */
    public WapAllinpayPaymentRequest ext2(String ext2) {
        if (ext2 != null) {
            params.put("ext1", ext2);
        }
        return this;
    }

    /**
     * 扩展字段
     * @param extTL 扩展字段
     */
    public WapAllinpayPaymentRequest extTL(String extTL) {
        if (extTL != null) {
            params.put("extTL", extTL);
        }
        return this;
    }

    /**
     * 贸易类型
     * @param tradeNature 贸易类型
     */
    public WapAllinpayPaymentRequest tradeNature(String tradeNature) {
        if (tradeNature != null) {
            params.put("tradeNature", tradeNature);
        }
        return this;
    }



}
