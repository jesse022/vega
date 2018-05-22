package com.sanlux.pay.allinpay.job;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.token.PcAllinpayToken;
import com.sanlux.pay.allinpay.trans.AllinpayTrans;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.api.TransCollector;
import io.terminus.pay.api.TransLoader;
import io.terminus.pay.component.TransJobTemplate;
import io.terminus.pay.model.PayTransCriteria;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * DATE: 16/9/4 上午10:18 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@RestController
@Slf4j
public class PcAllinpayTransJob extends TransJobTemplate<PcAllinpayToken,AllinpayTrans> {


    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    @Autowired
    public PcAllinpayTransJob(TokenProvider<PcAllinpayToken> tokenProvider,
                              EventBus eventBus,
                              TransLoader<AllinpayTrans, PcAllinpayToken> transLoader,
                              TransCollector<AllinpayTrans> transCollector) {
        super(tokenProvider, eventBus, transLoader, transCollector, AllinpayChannels.PC);
    }

    @Scheduled(cron="${pay.cron.trans.allinpay.pc: 0 0 2 * * ?}")
    public void syncAlipayTrans(){
        log.info("[CRON-JOB] [HANDLE-LOAD-ALLINPAY-TRANS] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        PayTransCriteria criteria = new PayTransCriteria().startFromYesterday().endWithStartOfToday();
        super.syncTrans(criteria);

        stopwatch.stop();
        log.info("[CRON-JOB] [HANDLE-LOAD-ALLINPAY-TRANS] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @RequestMapping("/api/pay/trans/sync/allinpay")
    public void syncAlipayTrans(@RequestParam String settleDate){
        PayTransCriteria criteria = new PayTransCriteria();
        Date start = DATE_TIME_FORMAT.parseDateTime(settleDate).withTimeAtStartOfDay().toDate();
        criteria.setStart(start);
        super.syncTrans(criteria);
    }

}
