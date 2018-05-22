/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.sanlux.pay.credit.request;

import com.google.common.base.Strings;

import java.net.URLEncoder;

import static com.google.common.base.Preconditions.checkArgument;
import static io.terminus.common.utils.Arguments.notEmpty;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * 支付请求
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-25 11:40 AM  <br>
 * Author: xiao
 */
public class CreditPayRequest extends Request {

    private CreditPayRequest(CreditPayToken creditPayToken) {
        super(creditPayToken);
        params.put("service", "create_credit_pay_by_user");
        params.put("payment_type", "1");

    }

    public static CreditPayRequest build(CreditPayToken alipayToken) {
        return new CreditPayRequest(alipayToken);
    }

    /**
     * 信用额度前台返回URL
     *
     * @param forward 支付成功后的转发路径
     * @param systemNo 支付单ID
     * @return this
     */
    public CreditPayRequest forward(String forward, String systemNo) {
        if (notNull(forward)) {
            params.put("return_url", forward + "?data=" + systemNo);
        }
        return this;
    }


    /**
     * 支付超时分钟数
     *
     * @param minutes 分钟
     * @return this
     */
    public CreditPayRequest timeoutM(int minutes) {
        if (minutes > 0 && minutes <= 15 * 24 * 60) {
            params.put("it_b_pay", minutes + "m");
        }
        return this;
    }


    /**
     * 信用额度后台通知URL
     *
     * @param notify 信用额度异步后台通知
     * @return this
     */
    public CreditPayRequest notify(String notify) {
        if (notNull(notify)) {
            params.put("notify_url", notify);
        }
        return this;
    }



    /**
     * sellerNo
     *
     * @return this
     */
    public CreditPayRequest sellerNo(String sellerNo) {
        if (notNull(sellerNo)) {
            params.put("seller_no", sellerNo);
        }
        return this;
    }


    /**
     * 收银台上显示的商品标题
     *
     * @param title 商品标题
     */
    public CreditPayRequest title(String title) {
        if (title != null) {
            params.put("subject", title);
        }
        return this;
    }

    /**
     * 商品内容
     *
     * @param content 商品内容
     */
    public CreditPayRequest content(String content) {
        if (notEmpty(content)) {
            params.put("body", content);
        }

        return this;
    }

    public CreditPayRequest outerTradeNo(String outerTradeNo) {
        checkArgument(notEmpty(outerTradeNo), "credit.pay.outer.trade.no.empty");
        params.put("out_trade_no", outerTradeNo);
        return this;
    }

    public CreditPayRequest total(Integer total) {
        checkArgument(notNull(total), "credit.pay.total.empty");
        String fee = DECIMAL_FORMAT.format(total / 100.0);
        params.put("total_fee", fee);
        return this;
    }


    public String pay() {
        return super.url();
    }




    @Override
    public void sign() {
        try {
            super.sign();
            String subject = (String) params.get("subject");
            if (!Strings.isNullOrEmpty(subject)) {
                params.put("subject", URLEncoder.encode(subject, "utf-8"));
            }

            String body = (String) params.get("body");
            if (!Strings.isNullOrEmpty(body)) {
                params.put("body", URLEncoder.encode(body, "utf-8"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
