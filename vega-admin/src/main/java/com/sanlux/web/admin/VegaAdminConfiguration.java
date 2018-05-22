/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin;

import com.sanlux.pay.AppAllinpayAutoConfig;
import com.sanlux.pay.PcAllinpayAutoConfig;
import com.sanlux.pay.VegaPayConfiguration;
import com.sanlux.pay.WapAllinpayAutoConfig;
import com.sanlux.trade.VegaTradeApiConfig;
import com.sanlux.trade.settle.component.VegaSummaryRule;
import com.sanlux.web.admin.settle.config.VegaSettleJobConfig;
import com.sanlux.web.admin.store.config.VegaStorageConfig;
import com.sanlux.web.admin.trade.job.VegaOrderNotPaidExpireJob;
import com.sanlux.web.front.core.VegaCoreWebConfiguration;
import com.sanlux.web.front.core.trade.VegaRefundParamsMaker;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.TradeApiConfig;
import io.terminus.parana.UserApiConfig;
import io.terminus.parana.web.admin.AdminConfiguration;
import io.terminus.parana.web.admin.interceptors.ParanaLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Author  : panxin
 * Date    : 6:17 PM 2/29/16
 * Mail    : panxin@terminus.io
 */
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({"com.sanlux.web.admin","io.terminus.parana.store.job.report"})
@Import({VegaTradeApiConfig.class,
        ItemApiConfiguration.class,
        TradeApiConfig.class,
        UserApiConfig.class,
        AdminConfiguration.class,
        VegaCoreWebConfiguration.class,
        VegaPayConfiguration.class,
        VegaSettleJobConfig.class,
        PcAllinpayAutoConfig.class,
        WapAllinpayAutoConfig.class,
        AppAllinpayAutoConfig.class,
        VegaStorageConfig.class
})
public class VegaAdminConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private ParanaLoginInterceptor paranaLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(paranaLoginInterceptor);
    }




    @Bean
    public VegaOrderNotPaidExpireJob orderNotPaidExpireJob(){
        return new VegaOrderNotPaidExpireJob();
    }

    @Bean
    public VegaSummaryRule vegaSummaryRule() {
        return new VegaSummaryRule();
    }

    @Configuration
    @ConditionalOnProperty(name = "pay.cron.trans.allinpay.pc.enable", havingValue = "true")
    @ComponentScan({
            "com.sanlux.pay.allinpay.job", "com.sanlux.pay.allinpay.trans.component"
    })
    public static class TransConfig{

    }
}
