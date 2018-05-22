package com.sanlux.web.front.core.events.trade.listener.config;

import io.terminus.parana.web.core.events.trade.listener.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Effet
 */
@Configuration
public class ParanaTradeListenerConfig {


/*

    @Bean
    public OrderCommentMarkListener orderCommentMarkListener() {
        return new OrderCommentMarkListener();
    }
*/


    @Bean
    public OrderCreationListener orderCreationListener() {
        return new OrderCreationListener();
    }

    @Bean
    public OrderStatusUpdater orderStatusUpdater() {
        return new OrderStatusUpdater();
    }

    @Bean
    public OrderCommentListener orderCommentListener(){
        return new OrderCommentListener();
    }

}
