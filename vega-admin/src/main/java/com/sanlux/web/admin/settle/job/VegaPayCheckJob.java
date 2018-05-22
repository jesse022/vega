package com.sanlux.web.admin.settle.job;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.pay.allinpay.job.PcAllinpayTransJob;
import com.sanlux.trade.settle.criteria.VegaSettleOrderDetailCriteria;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.pay.alipay.pc.job.ProdPcAlipayTransJob;
import io.terminus.pay.model.PayTransCriteria;
import io.terminus.pay.wechatpay.app.job.ProdAppWechatpayTransJob;
import io.terminus.pay.wechatpay.jsapi.job.ProdJsapiWechatpayTransJob;
import io.terminus.pay.wechatpay.qr.job.ProdQrWechatpayTransJob;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 结算支付对账自动补对job
 * Created by lujm on 2017/4/21.
 */
@Slf4j
@Component
public class VegaPayCheckJob {
    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;

    @Autowired
    private CreditPayTransJob creditPayTransJob;

    @Autowired
    private PcAllinpayTransJob pcAllinpayTransJob;

    @Autowired
    private ProdPcAlipayTransJob prodPcAlipayTransJob;

    @Autowired
    private ProdQrWechatpayTransJob prodQrWechatpayTransJob;

    @Autowired
    private ProdAppWechatpayTransJob prodAppWechatpayTransJob;

    @Autowired
    private ProdJsapiWechatpayTransJob prodJsapiWechatpayTransJob;

    @Autowired
    private HostLeader hostLeader;


    static final Integer BATCH_SIZE = 100;     // 每批处理数量
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DFT2 = DateTimeFormat.forPattern("yyyy-MM-dd");


    @Scheduled(cron = "${pay.auto.check.job.cron: 0 0 3 * * ?}")//每天凌晨3点执行
    public void handPaycheck() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }

            log.info("[CRON-JOB] [HANDLE-PAY-AUTO-CHECK-JOB] begin {}", DFT.print(DateTime.now()));
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<SettleOrderDetail> canAutoCheckPayments = Lists.newArrayList();

            int pageNo = 1;
            boolean next = batchHandle(pageNo, BATCH_SIZE, canAutoCheckPayments);
            while (next) {
                pageNo++;
                next = batchHandle(pageNo, BATCH_SIZE, canAutoCheckPayments);
            }
            for (SettleOrderDetail settleOrderDetail : canAutoCheckPayments) {
                Date startDate = DFT2.parseDateTime(DateToString(settleOrderDetail.getPaidAt())).toDate();
                Date endDate = DFT2.parseDateTime(DateToString(settleOrderDetail.getPaidAt())).plusDays(1).toDate();
                PayTransCriteria criteria = new PayTransCriteria();
                if (Arguments.isNull(settleOrderDetail.getChannel())){
                    continue;
                }
                if(settleOrderDetail.getChannel().startsWith("allinpay")){
                    criteria.setStart(endDate);
                }else {
                    criteria.setStart(startDate);
                    criteria.setEnd(endDate);
                }
                switch (settleOrderDetail.getChannel()) {
                    //1.信用支付
                    case "credit-pay":
                    case "credit-pay-wap":
                    case "credit-pay-member":
                    case "credit-pay-member-wap":
                        creditPayTransJob.syncCreditPayTrans(criteria);
                        break;
                    //2.通联支付
                    case "allinpay-pc":
                    case "allinpay-wap":
                    case "allinpay-app":
                    case "allinpay":
                        pcAllinpayTransJob.syncTrans(criteria);
                        break;
                    //3.支付宝
                    case "alipay-pc":
                    case "alipay-app":
                    case "alipay-wap":
                        prodPcAlipayTransJob.syncTrans(criteria);
                        break;
                    //4.微信
                    case "wechatpay-qr":
                        prodQrWechatpayTransJob.syncTrans(criteria);
                        break;
                    case "wechatpay-app":
                        prodAppWechatpayTransJob.syncTrans(criteria);
                        break;
                    case "wechatpay-jsapi":
                        prodJsapiWechatpayTransJob.syncTrans(criteria);
                        break;
                }
            }
            stopwatch.stop();
            log.info("[CRON-JOB] [HANDLE-PAY-AUTO-CHECK-JOB] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }catch (Exception e){
            log.error("[CRON-JOB] [HANDLE-PAY-AUTO-CHECK-JOB] ERROR at {},cause : {}", DFT.print(DateTime.now()),Throwables.getStackTraceAsString(e));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size, List<SettleOrderDetail> canAutoCheckPayments) {
        VegaSettleOrderDetailCriteria criteria = new VegaSettleOrderDetailCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(size);
        criteria.setCheckStatus(CheckStatus.WAIT_CHECK.value());//待对账状态
        criteria.setPaidAtEnd(DateTime.now().plusDays(-3).toDate());//支付时间为3天之前
        Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging SettleOrderDetail by criteria = {}, cause : {}",
                    criteria, resp.getError());
            return Boolean.FALSE;
        }
        List<SettleOrderDetail> listDetail = resp.getResult().getData();

        if (resp.getResult().getTotal().equals(0L) || CollectionUtils.isEmpty(listDetail)) {
            return Boolean.FALSE;
        }
        canAutoCheckPayments.addAll(listDetail);

        int current = listDetail.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }

    /**
     * Date转String
     *
     * @param date Date
     * @return String
     */
    private String DateToString(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return Objects.isNull(date) ? format.format(new Date()) : format.format(date);
    }
}
