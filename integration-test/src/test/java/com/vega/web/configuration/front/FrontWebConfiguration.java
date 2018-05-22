package com.vega.web.configuration.front;


import com.sanlux.VegaItemConfiguration;
import com.sanlux.trade.impl.VegaTradeConfiguration;
import com.sanlux.user.VegaUserConfiguration;
import com.sanlux.web.front.VegaWebConfiguration;
import com.vega.MockLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.charset.Charset;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2016-05-27 4:50 PM  <br>
 * Author: xiao
 */

@Configuration
@Import({VegaItemConfiguration.class,
        VegaTradeConfiguration.class,
        VegaUserConfiguration.class,
        VegaWebConfiguration.class
})
public class FrontWebConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public MockLoginInterceptor mockLoginInterceptor() {
        return new MockLoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ctx.getBean(MockLoginInterceptor.class));
    }
    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(
                Charset.forName("UTF-8"));
    }

}
