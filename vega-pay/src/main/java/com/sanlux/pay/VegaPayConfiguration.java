package com.sanlux.pay;

import com.sanlux.pay.credit.impl.CreditPayTokenProvider;
import com.sanlux.pay.credit.paychannel.CreditPaymentChannel;
import com.sanlux.pay.credit.request.CreditPayToken;
import io.terminus.pay.api.ChannelRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class VegaPayConfiguration {


    @Bean
    public ChannelRegistry channelRegistry() {
        return new ChannelRegistry();
    }

    @Configuration
    public static class ProductPayConfig{

        @ConditionalOnProperty(name = "pay.channel.credit-pay")
        @EnableConfigurationProperties(CreditPayToken.class)
        public static class CreditPayConfig{

            @Bean
            @ConditionalOnMissingBean(name = "creditPayTokenProvider")
            public CreditPayTokenProvider creditPayTokenProvider(ChannelRegistry channelRegistry, CreditPayToken creditPayToken){

                CreditPayTokenProvider creditPayTokenProvider= new CreditPayTokenProvider(creditPayToken);
                channelRegistry.register("credit-pay", new CreditPaymentChannel(creditPayTokenProvider));
                channelRegistry.register("credit-pay-wap", new CreditPaymentChannel(creditPayTokenProvider));
                channelRegistry.register("credit-pay-member", new CreditPaymentChannel(creditPayTokenProvider));
                channelRegistry.register("credit-pay-member-wap", new CreditPaymentChannel(creditPayTokenProvider));
                return creditPayTokenProvider;
            }
        }
    }

}
