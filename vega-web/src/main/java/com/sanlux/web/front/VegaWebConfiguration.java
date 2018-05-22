package com.sanlux.web.front;

import com.sanlux.VegaItemApiConfiguration;
import com.sanlux.pay.AppAllinpayAutoConfig;
import com.sanlux.pay.PcAllinpayAutoConfig;
import com.sanlux.pay.VegaPayConfiguration;
import com.sanlux.pay.WapAllinpayAutoConfig;
import com.sanlux.trade.VegaTradeApiConfig;
import com.sanlux.trade.component.VegaFlowPicker;
import com.sanlux.trade.component.VegaRichOrderMaker;
import com.sanlux.web.front.core.VegaCoreWebConfiguration;
import com.sanlux.web.front.core.trade.VegaRefundParamsMaker;
import io.terminus.boot.redis.autoconfigure.RedisAutoConfiguration;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.ip.IPLibrary;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.TradeApiConfig;
import io.terminus.parana.UserApiConfig;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.web.core.CoreWebConfiguration;
import io.terminus.parana.web.front.FrontConfiguration;
import io.terminus.parana.web.front.interceptors.ParanaLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import java.io.IOException;

/**
 * @author Effet
 */
@Configuration
@EnableAutoConfiguration
@Import({VegaTradeApiConfig.class,
        ItemApiConfiguration.class,
        VegaItemApiConfiguration.class,
        TradeApiConfig.class,
        UserApiConfig.class,
        FrontConfiguration.class,
        CoreWebConfiguration.class,
        VegaCoreWebConfiguration.class,
        VegaPayConfiguration.class,
        PcAllinpayAutoConfig.class,
        WapAllinpayAutoConfig.class,
        AppAllinpayAutoConfig.class,
        RedisAutoConfiguration.class
})
@ComponentScan(value = {"io.terminus.parana.store.web.dto",
        "io.terminus.parana.store.web.check",
        "io.terminus.parana.store.web.repo",
        "io.terminus.parana.store.web.storage",
        "io.terminus.parana.store.web.executor",
        "io.terminus.parana.store.web.report",
        "io.terminus.parana.store.web.util"
})
@EnableScheduling
@EnableWebMvc
public class VegaWebConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private ParanaLoginInterceptor paranaLoginInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(paranaLoginInterceptor);
    }
    @Bean
    public RichOrderMaker richOrderMaker() {
        return new VegaRichOrderMaker();
    }

    @Bean
    public FlowPicker flowPicker() {
        return new VegaFlowPicker();
    }

    @ConditionalOnProperty("ip.dic")
    @Bean
    public IPLibrary ipLibrary(@Value("${ip.dic}") String ipDic) throws IOException {
        return new IPLibrary(ipDic);
    }


    @Bean
    public JedisTemplate pampasJedisTemplate(Pool<Jedis> pool) {
        return new JedisTemplate(pool);
    }
}
