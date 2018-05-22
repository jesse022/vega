/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.config;

import com.sanlux.web.front.core.settlement.listener.VegaCommissionRateCreateListener;
import com.sanlux.web.front.core.settlement.listener.VegaPayTransCollectedListener;
import com.sanlux.web.front.core.settlement.listener.VegaPayTransLoadedListener;
import com.sanlux.web.front.core.settlement.listener.VegaPaymentPayChannelDetailListener;
import com.sanlux.web.front.core.settlement.listener.VegaPaymentSettlementListener;
import com.sanlux.web.front.core.settlement.listener.VegaRefundPayChannelDetailListener;
import io.terminus.parana.settle.api.SummaryRule;
import io.terminus.parana.settle.component.DefaultSummaryRule;
import io.terminus.parana.web.core.events.settle.listener.PayTransLoadedListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author : panxin
 */
@Configuration
@ConditionalOnProperty(name = "vega.settle.listener.enable", havingValue = "true", matchIfMissing = true)
@Import(VegaSettleTradeConfig.class)
public class VegaSettlementConfig {

    @Bean
    public VegaPaymentSettlementListener vegaOrderAcceptListener() {
         // 支付成功, Sanlux 实际是在确认接单之后才生成结算单
        return new VegaPaymentSettlementListener();
    }

    @Bean
    public SummaryRule summaryRule() {
        // 用默认就可以
        return new DefaultSummaryRule();
    }

    @Bean
    public VegaCommissionRateCreateListener vegaCommissionRateCreateListener() {
        return new VegaCommissionRateCreateListener();
    }

    @Bean
    public VegaPayTransCollectedListener vegaPayTransCollectedListener() {
        // 添加了信用额度支付对账
        return new VegaPayTransCollectedListener();
    }

//    @Bean
    public PayTransLoadedListener payTransLoadedListener() {
        return new PayTransLoadedListener();
    }

    @Bean
    public VegaPayTransLoadedListener vegaPayTransLoadedListener(){
        return new VegaPayTransLoadedListener();
    }

    @Bean
    public VegaPaymentPayChannelDetailListener vegaPaymentPayChannelDetailListener() {
        return new VegaPaymentPayChannelDetailListener();
    }

    @Bean
    public VegaRefundPayChannelDetailListener vegaRefundPayChannelDetailListener() {
        return new VegaRefundPayChannelDetailListener();
    }

}
