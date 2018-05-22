package com.sanlux.web.admin.trade.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.enums.AlinnpayRefundHandleStatus;
import com.sanlux.pay.allinpay.paychannel.PcAllinpayChannel;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.web.core.events.settle.RefundSettleEvent;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.service.PayChannel;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/19/16
 * Time: 11:17 AM
 */
@Slf4j
@Component
public class AllinpayRefundQueryJob {

    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private  ChannelRegistry channelRegistry;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private EventBus eventBus;


    static final Integer BATCH_SIZE = 100;     // 批处理数量
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");



    @Scheduled(cron = "0 0/5 * * * ?")
    public void handRefundQuery() {

        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("[CRON-JOB] [HANDLE-REFUßND-QUERY] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<Refund> refunds = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE, refunds);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE, refunds);
        }

        //查询是否退款成功,如果退款成功则更新退款单状态
        for (Refund refund : refunds){
            Response<AlinnpayRefundHandleStatus> response = refundQuery(refund);
            if(!response.isSuccess()){
                log.error("query refund result id:{} fail,error:{}",refund.getId(),response.getError());
            }

            checkStatus(response.getResult(),refund);
        }

        stopwatch.stop();
        log.info("[CRON-JOB] [HANDLE-REFUND-QUERY] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));


    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,List<Refund> refunds) {

        RefundCriteria criteria = new RefundCriteria();
        List<String> channels = Lists.newArrayList();
        channels.add(AllinpayChannels.PC);
        channels.add(AllinpayChannels.APP);
        channels.add(AllinpayChannels.WAP);
        criteria.setSellerNote(SystemConstant.ALLINPAY_REFUND_APPLY);
        criteria.setChannels(channels);
        Response<Paging<Refund>> pagingRes = refundReadService.findRefundBy(pageNo, size, criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging shop order fail,criteria:{},error:{}",criteria,pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<Refund> paging = pagingRes.getResult();
        List<Refund> refunds1 = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(refunds1)) {
            return Boolean.FALSE;
        }
        refunds.addAll(refunds1);

        int current = refunds1.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }

    public Response<AlinnpayRefundHandleStatus> refundQuery(Refund refund){

        PayChannel channel=channelRegistry.findChannel(AllinpayChannels.PC);
        PcAllinpayChannel paymentChannel = (PcAllinpayChannel)channel;
        return paymentChannel.refundQuery(refund.getTradeNo(),refund.getFee(),refund.getOutId());

    }

    public void checkStatus(AlinnpayRefundHandleStatus handleStatus,Refund refund){
        if(Arguments.isNull(handleStatus)){
            log.error("query refund (id:{}) refund status result is null",refund.getId());
            return;
        }
        switch (handleStatus){
            case TKSUCC0001:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,handleStatus.toString(),refund);
                break;
            case TKSUCC0002:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,handleStatus.toString(),refund);
                break;
            case TKSUCC0003:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,handleStatus.toString(),refund);
                break;
            case TKSUCC0004:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,handleStatus.toString(),refund);
                break;
            case TKSUCC0005:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,handleStatus.toString(),refund);
                break;
            case TKSUCC0006:
                //退款成功
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY_AGREE,handleStatus.toString(),refund);

                //生成结算事件
                eventBus.post(new RefundSettleEvent(refund.getChannel(), refund.getOutId(), refund.getId(), new Date()));
                break;
            case TKSUCC0007:
                //退款失败
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY_REJECT,handleStatus.toString(),refund);
                break;
            case TKSUCC0008:
                //通联审核不通过
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY_REJECT,handleStatus.toString(),refund);
                break;
            default:
                updateRefundInfo(SystemConstant.ALLINPAY_REFUND_APPLY,"通联返回的处理状态不匹配",refund);
        }
    }

    public void updateRefundInfo(String refundApplyStatus ,String message,Refund refund){
        Refund refundUpdate = new Refund();
        refundUpdate.setId(refund.getId());
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }
        extraMap.put(SystemConstant.ALLINPAY_REFUND_APPLY_MESSAGE,message);
        refundUpdate.setExtra(extraMap);
        refundUpdate.setSellerNote(refundApplyStatus);
        //退款成功设置退款成功时间
        if(AlinnpayRefundHandleStatus.TKSUCC0006.toString().equals(message)){
            refundUpdate.setRefundAt(new Date());

            Map<String, String> tages = refund.getTags();
            String isShiped = tages.get(SystemConstant.IS_SHIPPED);
            Integer orderStatus = VegaOrderStatus.REFUND.getValue();
            if(isShiped.equals(SystemConstant.SHIPPED)){
                orderStatus = VegaOrderStatus.RETURN_REFUND.getValue();
            }
            refundUpdate.setStatus(orderStatus);

        }

        Response<Boolean> updateRes = refundWriteService.update(refundUpdate);
        if(!updateRes.isSuccess()){
            log.error("update refund:{} fail,error:{}", refundUpdate, updateRes.getError());
        }

    }



}
