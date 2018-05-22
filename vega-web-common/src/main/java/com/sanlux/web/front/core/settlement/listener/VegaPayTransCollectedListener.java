package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.web.core.settle.SettleUpdateService;
import io.terminus.pay.event.PayTransCollectedEvent;
import io.terminus.pay.model.PayTrans;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * DATE: 16/7/29 下午5:30 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class VegaPayTransCollectedListener {

    @Autowired
    protected SettleUpdateService settleUpdateService;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPayTransCollected(PayTransCollectedEvent event){
        log.info("dispatch PayTransCollectedEvent={}", event);
        try {
            for (PayTrans payTrans : event.getPayTransList()) {
                val trans = BeanMapper.map(payTrans, io.terminus.parana.settle.model.PayTrans.class);
                if(payTrans.getRefundNo()!=null){
                    trans.setTradeType(TradeType.Refund.value());
                }else{
                    trans.setTradeType(TradeType.Pay.value());
                }
                if(payTrans.getChannel().contains("mockpay")) {
                    trans.setLoadAt(DateTime.now().plusDays(1).toDate());
                }else{
                    trans.setLoadAt(event.getLoadedAt());
                }
                settleUpdateService.checkSettleDetails(trans);
            }
        }catch (Exception e){
            log.error("dispatchTransCollectedEvent fail, event={}, cause={}", event, Throwables.getStackTraceAsString(e));
        }
    }

}
