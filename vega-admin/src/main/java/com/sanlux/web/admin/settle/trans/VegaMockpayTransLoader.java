/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle.trans;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.dto.paging.PayChannelDetailCriteria;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.service.PayChannelDetailReadService;
import io.terminus.parana.web.admin.jobs.settle.trans.ParanaMockpayTransLoader;
import io.terminus.pay.constants.Channels;
import io.terminus.pay.constants.Tokens;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.mockpay.MockPayToken;
import io.terminus.pay.mockpay.trans.MockpayTrans;
import io.terminus.pay.model.PayTransCriteria;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author : panxin
 */
//@Component
//@Primary
@Slf4j
@Deprecated
public class VegaMockpayTransLoader extends ParanaMockpayTransLoader {

    @RpcConsumer
    private PayChannelDetailReadService payChannelDetailReadService;

    @Override
    public List<MockpayTrans> loadTrans(PayTransCriteria criteria, MockPayToken token) throws PayException {
        log.info("[mockpay] pay trans loader, PayTransCriteria = {}, CreditPayToken = {}");

        List<MockpayTrans> transList = Lists.newArrayList();

        List<PayChannelDetail> channelDetailList = findPayChannelDetailsByChannel(Channels.MOCKPAY);
        List<PayChannelDetail> appChannelDetailList = findPayChannelDetailsByChannel(Channels.MOCKPAY_APP);
        List<PayChannelDetail> wapChannelDetailList = findPayChannelDetailsByChannel(Channels.MOCKPAY_WAP);
        channelDetailList.addAll(appChannelDetailList);
        channelDetailList.addAll(wapChannelDetailList);

        for (PayChannelDetail detail : channelDetailList) {
            MockpayTrans trans = new MockpayTrans();
            trans.setId(detail.getId());
            trans.setFee(detail.getTradeFee());
            trans.setAccount(Tokens.DEFAULT_ACCOUNT);
            if (Objects.equal(detail.getTradeType(), TradeType.Pay.value())) {
                trans.setTradeNo(detail.getTradeNo());
            }else {
                trans.setRefundNo(detail.getTradeNo());
            }
            // trans.setChannel(detail.getChannel());
            transList.add(trans);
        }
        return transList;
    }

    private List<PayChannelDetail> findPayChannelDetailsByChannel(String channel) {
        Integer pageNo = 1;
        Integer pageSize = 50;
        // 对账前一天的订单
        Date startAt = DateTime.now().withTimeAtStartOfDay().toDate();
        Date endAt = DateTime.now().toDate();

        PayChannelDetailCriteria criteria = new PayChannelDetailCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setTradeFinishedAtStart(startAt);
        criteria.setTradeFinishedAtEnd(endAt);
        criteria.setChannel(channel);
        criteria.setCheckStatus(CheckStatus.WAIT_CHECK.value());

        log.info("mockpay criteria : {}", criteria);

        List<PayChannelDetail> details = Lists.newArrayList();
        while (true) {
            Response<Paging<PayChannelDetail>> resp = payChannelDetailReadService.pagingPayChannelDetails(criteria);
            if(!resp.isSuccess()){
                log.error("find mockpay unchecked PayChannelDetail fail, cause={}", resp.getError());
                return Collections.emptyList();
            }
            List<PayChannelDetail> detailList = resp.getResult().getData();
            if (detailList.isEmpty()) {
                break;
            }
            pageNo++;
            criteria.setPageNo(pageNo);
            details.addAll(detailList);
        }

        log.info("mockpay channel detail : {}", details);

        return details;
    }

}
