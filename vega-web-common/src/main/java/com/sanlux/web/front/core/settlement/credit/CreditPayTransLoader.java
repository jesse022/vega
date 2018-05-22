package com.sanlux.web.front.core.settlement.credit;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.pay.credit.request.CreditPayToken;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.dto.paging.PayChannelDetailCriteria;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.service.PayChannelDetailReadService;
import io.terminus.pay.api.TransLoader;
import io.terminus.pay.constants.Tokens;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.PayTransCriteria;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * DATE: 16/9/12 上午10:40 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Component
@Primary
@Slf4j
public class CreditPayTransLoader implements TransLoader<CreditPayTrans, CreditPayToken> {

    @RpcConsumer
    private PayChannelDetailReadService payChannelDetailReadService;

    @Override
    public List<CreditPayTrans> loadTrans(PayTransCriteria criteria,
                                          CreditPayToken token) throws PayException {
        log.info("[credit-pay] pay trans loader, PayTransCriteria = {}, CreditPayToken = {}");

        List<CreditPayTrans> transList = Lists.newArrayList();

        List<PayChannelDetail> channelDetailList = findPayChannelDetailsByChannel(criteria, CreditPayConstants.PAY_CHANNEL);
        List<PayChannelDetail> wapChannelDetailList = findPayChannelDetailsByChannel(criteria, CreditPayConstants.WAP_PAY_CHANNEL);
        List<PayChannelDetail> memberChannelDetailList = findPayChannelDetailsByChannel(criteria, CreditPayConstants.MEMBER_PAY_CHANNEL);
        List<PayChannelDetail> wapMemberChannelDetailList = findPayChannelDetailsByChannel(criteria, CreditPayConstants.MEMBER_WAP_PAY_CHANNEL);
        channelDetailList.addAll(wapChannelDetailList);
        channelDetailList.addAll(memberChannelDetailList);
        channelDetailList.addAll(wapMemberChannelDetailList);

        for (PayChannelDetail detail : channelDetailList) {
            CreditPayTrans trans = new CreditPayTrans();
            trans.setId(detail.getId());
            trans.setFee(detail.getTradeFee());
            trans.setAccount(Tokens.DEFAULT_ACCOUNT);
            if (Objects.equal(detail.getTradeType(), TradeType.Pay.value())) {
                trans.setTradeNo(detail.getTradeNo());
            }else {
                trans.setRefundNo(detail.getTradeNo());
            }
            trans.setChannel(detail.getChannel());
            transList.add(trans);
        }

        return transList;
    }

    private List<PayChannelDetail> findPayChannelDetailsByChannel(PayTransCriteria transCriteria, String channel) {
        Integer pageNo = 1;
        Integer pageSize = 50;
        // 对账前一天的订单
        Date startAt = null;
        Date endAt = null;

        if (transCriteria.getStart() != null) {
            startAt = transCriteria.getStart();
        }else {
            startAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        }
        if (transCriteria.getEnd() != null) {
            endAt = DateTime.now().withTimeAtStartOfDay().toDate();
        }

        PayChannelDetailCriteria criteria = new PayChannelDetailCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setTradeFinishedAtStart(startAt);
        criteria.setTradeFinishedAtEnd(endAt);
        criteria.setChannel(channel);
        criteria.setCheckStatus(CheckStatus.WAIT_CHECK.value());

        log.info("credit-pay criteria : {}", criteria);

        List<PayChannelDetail> details = Lists.newArrayList();
        while (true) {
            Response<Paging<PayChannelDetail>> resp=payChannelDetailReadService.pagingPayChannelDetails(criteria);
            if(!resp.isSuccess()){
                log.error("find credit-pay unchecked PayChannelDetail fail, cause={}", resp.getError());
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

        log.info("credit-pay channel detail : {}", details);

        return details;
    }
}
