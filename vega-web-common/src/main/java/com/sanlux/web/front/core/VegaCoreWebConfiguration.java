/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core;

import com.sanlux.web.front.core.events.trade.listener.config.ParanaTradeListenerConfig;
import com.sanlux.web.front.core.settlement.service.VegaSettleCreateServiceImpl;
import com.sanlux.web.front.core.settlement.service.VegaSettleUpdateServiceImpl;
import com.sanlux.web.front.core.trade.VegaRefundParamsMaker;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.web.core.settle.SettleCreateService;
import io.terminus.parana.web.core.settle.SettleUpdateService;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-02-09
 */
@Configuration
@ComponentScan
@Import(ParanaTradeListenerConfig.class)
public class VegaCoreWebConfiguration extends WebMvcConfigurerAdapter {


    @Bean
    public JedisTemplate pampasJedisTemplate(Pool<Jedis> pool) {
        return new JedisTemplate(pool);
    }

    @Bean
    public SettleCreateService settleCreateService() {
        return new VegaSettleCreateServiceImpl();
    }

    @Bean
    public SettleUpdateService settleUpdateService() {
        return new VegaSettleUpdateServiceImpl();
    }

    @Bean
    public VegaRefundParamsMaker refundParamsMaker(){
        return new VegaRefundParamsMaker();
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("vega_messages", "messages", "parana_store_messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(true);
        messageSource.setCacheSeconds(-1);
        messageSource.setAlwaysUseMessageFormat(false);
        return messageSource;
    }

}
