/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.settlement.config;

import com.sanlux.web.front.core.settlement.component.VegaSettleRefundJob;
import com.sanlux.web.front.core.settlement.job.VegaProdSettleTradeJob;
import com.sanlux.web.front.core.settlement.job.VegaTestSettleTradeJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author : panxin
 */
@Configuration
public class VegaSettleTradeConfig {

    @Configuration
    @ComponentScan("io.terminus.parana.web.admin.jobs.settle.trans")
    public static class TransConfig{
    }

    //@ComponentScan("io.terminus.parana.web.admin.jobs.settle.component")
    @ConditionalOnProperty(name="vega.settle.job.detail.enable", havingValue = "true", matchIfMissing = true)
    @Configuration
    @ComponentScan("com.sanlux.web.front.core.settlement.component")
    public static class SettleTradeConfig{

        @Bean
        @Profile({"test", "dev"})
        public VegaTestSettleTradeJob vegaTestSettleTradeJob(VegaSettleRefundJob vegaSettleRefundJob){
            return new VegaTestSettleTradeJob(vegaSettleRefundJob);
        }

        @Bean
        public VegaProdSettleTradeJob vegaProdSettleTradeJob(VegaSettleRefundJob vegaSettleRefundJob) {
            return new VegaProdSettleTradeJob(vegaSettleRefundJob);
        }

    }

}
