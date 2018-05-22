package com.sanlux;

import com.sanlux.item.component.VegaDeliveryFeeTemplateChecker;
import com.sanlux.item.component.VegaInitialItemInfoFiller;
import io.terminus.parana.delivery.component.DeliveryFeeTemplateChecker;
import io.terminus.parana.item.api.InitialItemInfoFiller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author:cp
 * Created on 8/8/16.
 */
@Configuration
public class VegaItemApiConfiguration {

    @Bean
    public InitialItemInfoFiller initialItemInfoFiller() {
        return new VegaInitialItemInfoFiller();
    }

    @Bean
    public DeliveryFeeTemplateChecker deliveryFeeTemplateChecker() {
        return new VegaDeliveryFeeTemplateChecker();
    }

}
