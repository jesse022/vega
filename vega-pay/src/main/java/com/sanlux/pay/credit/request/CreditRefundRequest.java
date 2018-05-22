/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.sanlux.pay.credit.request;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sanlux.pay.credit.dto.CreditPayRefundData;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.net.URLEncoder;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * 退款请求
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-25 11:47 AM  <br>
 * Author: xiao
 */
@Slf4j
public class CreditRefundRequest extends Request {

    private static final Joiner DETAIL_JOINER = Joiner.on("^").skipNulls();
    private static final Joiner REFUND_JOINER = Joiner.on("#").skipNulls();
    private static final DateTimeFormatter DFT_BATCH = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DFT_REFUND = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private CreditRefundRequest(CreditPayToken creditPayToken, String service) {
        super(creditPayToken);
        params.put("service", service);
        params.put("refund_date", DFT_REFUND.print(DateTime.now()));
    }

    //无密退款
    public static CreditRefundRequest buildWithNoPwd(CreditPayToken creditPayToken) {
        return new CreditRefundRequest(creditPayToken, "refund_credit_by_platform_nopwd");
    }

    //退款明细字符串用#拼接
    private String getRefundDetail(List<CreditPayRefundData> refunds) {
        List<String> refundDetails = Lists.newArrayListWithCapacity(refunds.size());
        for (CreditPayRefundData refund : refunds) {
            String tradeNo = refund.getTradeNo();
            Integer refundAmount = refund.getRefundAmount();
            String reason = refund.getReason();
            String detail = DETAIL_JOINER.join(tradeNo, DECIMAL_FORMAT.format(refundAmount / 100.0), reason);
            refundDetails.add(detail);
        }
        return REFUND_JOINER.join(refundDetails);
    }


    /**
     * 后台通知
     *
     * @param notify 通知
     */
    public CreditRefundRequest notify(String notify) {
        if (notNull(notify)) {
            params.put("notify_url", notify);
        }
        return this;
    }


    /**
     * 退款批次号
     *
     * @param batchNo 批次号
     */
    public CreditRefundRequest batch(String batchNo) {
        checkArgument(!Strings.isNullOrEmpty(batchNo), "credit.refund.batch.no.empty");
        params.put("batch_no", batchNo);
        return this;
    }

    /**
     * sellerNo
     *
     * @return this
     */
    public CreditRefundRequest sellerNo(String sellerNo) {
        if (notNull(sellerNo)) {
            params.put("seller_no", sellerNo);
        }
        return this;
    }

    /**
     * 退货详情
     *
     * @param refunds 退货列表
     */
    public CreditRefundRequest detail(List<CreditPayRefundData> refunds) {
        String detail = getRefundDetail(refunds);
        params.put("detail_data", detail);
        params.put("batch_num", refunds.size() + "");
        return this;
    }

    /**
     * 向支付宝网关发送退货请求
     *
     * @return 退货请求结果
     */
    public Boolean refund() {
        String url = super.refundUrl();
        log.debug("refund url: {}", url);
        String body = HttpRequest.get(url).connectTimeout(10000).readTimeout(10000).body();
        log.debug("refund result: {}", body);
        return convertToResponse(body);
    }

    @Override
    public void sign() {
        try {
            super.sign();
            String refundDate = (String) params.get("refund_date");
            if (!Strings.isNullOrEmpty(refundDate)) {
                params.put("refund_date", URLEncoder.encode(refundDate, "utf-8"));
            }

            String detailData = (String) params.get("detail_data");
            if (!Strings.isNullOrEmpty(detailData)) {
                params.put("detail_data", URLEncoder.encode(detailData, "utf-8"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
