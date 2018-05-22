package com.sanlux.trade;

import com.sanlux.trade.component.VegaFlowPicker;
import com.sanlux.trade.component.VegaPersistedOrderMaker;
import com.sanlux.trade.component.VegaRefundCharger;
import com.sanlux.trade.component.VegaRichOrderMaker;
import io.terminus.parana.order.api.AbstractPersistedOrderMaker;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.api.RefundCharger;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.order.model.OrderLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Desc:
 * Mail: F@terminus.io
 * Data: 16/3/8
 * Author: songrenfei
 */
@Configuration
public class VegaTradeApiConfig {

    @Bean
    public AbstractPersistedOrderMaker orderMaker() {
        return new VegaPersistedOrderMaker(OrderLevel.SHOP, OrderLevel.SHOP);
    }

    @Bean
    public RefundCharger refundCharge() {
        return new VegaRefundCharger();
    }

    @Bean
    public RichOrderMaker richOrderMaker() {
        return new VegaRichOrderMaker();
    }

    @Bean
    public FlowPicker flowPicker() {
        return new VegaFlowPicker();
    }


}
