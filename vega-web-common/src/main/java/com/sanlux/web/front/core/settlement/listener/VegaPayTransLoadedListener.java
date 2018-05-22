package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.trade.settle.model.AllinpayTrans;
import com.sanlux.trade.settle.service.AllinpayTransWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.settle.model.AlipayTrans;
import io.terminus.parana.settle.model.KjtpayTrans;
import io.terminus.parana.settle.model.UnionpayTrans;
import io.terminus.parana.settle.model.WechatpayTrans;
import io.terminus.parana.settle.service.PayTransWriteService;
import io.terminus.pay.event.PayTransLoadedEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 *  处理通联支付
 * DATE: 16/9/26 下午11:15 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class VegaPayTransLoadedListener {

    @Autowired
    protected EventBus eventBus;

    @RpcConsumer
    private PayTransWriteService payTransWriteService;
    @RpcConsumer
    private AllinpayTransWriteService allinpayTransWriteService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPayTransLoaded(PayTransLoadedEvent event){
        try {
            log.info("handle PayTransLoadedEvent, channel={}", event.getChannel());

            if (event.getChannel().contains("alipay")) {
                List<AlipayTrans> transList = JSON_MAPPER.fromJson(event.getPayTransList(),
                        JSON_MAPPER.createCollectionType(List.class, AlipayTrans.class));
                //List<AlipayTrans> transList = JsonMapper.nonEmptyMapper().fromJson(event.getPayTransList(),
                //        JsonMapper.nonEmptyMapper().createCollectionType(List.class, AlipayTrans.class));
                for (AlipayTrans alipayTrans : transList) {
                    val rCreate = payTransWriteService.createAlipayTrans(alipayTrans);
                    if (!rCreate.isSuccess()) {
                        log.error("createAlipayTrans fail, trans={}, cause={}", alipayTrans, rCreate.getError());
                    }
                }
            } else if (event.getChannel().contains("wechatpay")) {
                List<WechatpayTrans> transList = JSON_MAPPER.fromJson(event.getPayTransList(),
                        JSON_MAPPER.createCollectionType(List.class, WechatpayTrans.class));
                //List<WechatpayTrans> transList = JsonMapper.nonEmptyMapper().fromJson(event.getPayTransList(),
                //        JsonMapper.nonEmptyMapper().createCollectionType(List.class, WechatpayTrans.class));
                for (WechatpayTrans trans : transList) {
                    val rCreate = payTransWriteService.createWechatPayTrans(trans);
                    if (!rCreate.isSuccess()) {
                        log.error("createWechatPayTrans fail, trans={}, cause={}", trans, rCreate.getError());
                    }
                }
            } else if (event.getChannel().contains("unionpay")) {
                List<UnionpayTrans> transList = JsonMapper.nonEmptyMapper().fromJson(event.getPayTransList(),
                        JsonMapper.nonEmptyMapper().createCollectionType(List.class, UnionpayTrans.class));
                for (UnionpayTrans trans : transList) {
                    val rCreate = payTransWriteService.createUnionpayTrans(trans);
                    if (!rCreate.isSuccess()) {
                        log.error("createUnionpayTrans fail, trans={}, cause={}", trans, rCreate.getError());
                    }
                }
            } else if (event.getChannel().contains("kjtpay")) {
                List<KjtpayTrans> transList = JsonMapper.nonEmptyMapper().fromJson(event.getPayTransList(),
                        JsonMapper.nonEmptyMapper().createCollectionType(List.class, KjtpayTrans.class));
                for (KjtpayTrans trans : transList) {
                    val rCreate = payTransWriteService.createKjtpayTrans(trans);
                    if (!rCreate.isSuccess()) {
                        log.error("createKjtpayTrans fail, trans={}, cause={}", trans, rCreate.getError());
                    }
                }
            }else if (event.getChannel().contains("allinpay")) {
                List<AllinpayTrans> transList = JSON_MAPPER.fromJson(event.getPayTransList(), JSON_MAPPER.createCollectionType(List.class, AllinpayTrans.class));
                for (AllinpayTrans alipayTrans : transList) {
                    val rCreate = allinpayTransWriteService.create(alipayTrans);
                    if (!rCreate.isSuccess()) {
                        log.error("create allinpay trans fail, trans={}, cause={}", alipayTrans, rCreate.getError());
                    }
                }
            }
        }catch (Exception e){
            log.error("handle PayTransLoadedEvent fail, event={}, cause={}", event, Throwables.getStackTraceAsString(e));
        }
    }
}
