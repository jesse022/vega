package com.sanlux.web.front;

import com.sanlux.web.front.open.OPSecurityManager;
import io.terminus.pampas.openplatform.core.SecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author:cp
 * Created on 9/7/16.
 */
@Configuration
public class VegaOpenApiConfiguration {

    @Bean
    public SecurityManager OPSecurityManager(){
        return new OPSecurityManager();
    }

}
