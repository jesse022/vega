package com.vega.web.configuration.admin;


import com.sanlux.VegaItemConfiguration;
import com.sanlux.trade.impl.VegaTradeConfiguration;
import com.sanlux.user.VegaUserConfiguration;
import com.sanlux.web.admin.VegaAdminConfiguration;
import com.vega.MockLoginInterceptor;
import io.terminus.parana.web.front.open.ParanaOpenSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2016-05-27 4:50 PM  <br>
 * Author: xiao
 */


@Configuration
@Import({VegaItemConfiguration.class,
        VegaTradeConfiguration.class,
        VegaUserConfiguration.class,
        VegaAdminConfiguration.class
})
@EnableWebMvc
@EnableAutoConfiguration
public class AdminWebConfiguration extends WebMvcConfigurerAdapter {

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
    public Filter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    @Bean
    public io.terminus.pampas.openplatform.core.SecurityManager securityManager(){
        return new ParanaOpenSecurityManager();
    }

}
