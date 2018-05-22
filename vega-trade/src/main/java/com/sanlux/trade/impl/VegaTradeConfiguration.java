package com.sanlux.trade.impl;

import com.sanlux.trade.component.VegaPersistedOrderMaker;
import io.terminus.parana.TradeAutoConfig;
import io.terminus.parana.common.config.ImageConfig;
import io.terminus.parana.order.api.AbstractPersistedOrderMaker;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.strategy.DefaultShopOrderStatusStrategy;
import io.terminus.parana.order.strategy.ShopOrderStatusStrategy;
import io.terminus.parana.settle.api.SummaryRule;
import io.terminus.parana.settle.component.DefaultSummaryRule;
import io.terminus.parana.store.ParanaRepoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Mail: F@terminus.io
 * Data: 16/3/7
 * Author: yangzefeng
 */
@Configuration
@ComponentScan(basePackages = {
        "io.terminus.parana.settle.impl",
        "com.sanlux.trade.impl",
        "com.sanlux.store.impl"// vega
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = io.terminus.parana.settle.impl.service.SettleRichOrderReadServiceImpl.class)
})
@Import({ImageConfig.class, ParanaRepoConfiguration.class, TradeAutoConfig.class})
public class VegaTradeConfiguration {

    @ConditionalOnMissingBean({ShopOrderStatusStrategy.class})
    @Bean
    public ShopOrderStatusStrategy shopOrderStatusStrategy() {
        return new DefaultShopOrderStatusStrategy();
    }

    @Bean
    public AbstractPersistedOrderMaker orderMaker() {
        return new VegaPersistedOrderMaker(OrderLevel.SHOP, OrderLevel.SHOP);
    }

    @Bean
    @ConditionalOnMissingBean(
            name = {"summaryRule"}
    )
    public SummaryRule summaryRule() {
        return new DefaultSummaryRule();
    }

}
