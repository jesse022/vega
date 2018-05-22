package com.sanlux.pay;

import com.sanlux.pay.allinpay.component.PcAllinpayTokenProvider;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.paychannel.PcAllinpayChannel;
import com.sanlux.pay.allinpay.token.PcAllinpayToken;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.service.PayChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * DATE: 16/8/22 下午2:22 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Configuration
public class PcAllinpayAutoConfig {

    @Configuration
    @ConditionalOnProperty(name = "pay.token.provider.version", havingValue = "yaml", matchIfMissing = true)
    @EnableConfigurationProperties({PcAllinpayToken.class})
    public static class TokenProviderConfig{

        @Bean
        public PcAllinpayTokenProvider pcAllinpayTokenProvider(PcAllinpayToken token){
            return new PcAllinpayTokenProvider(token);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "pay.channel.allinpay.pc.enable", havingValue = "true", matchIfMissing = true)
    public static class PayConfig {
        @Bean
        @ConditionalOnMissingBean(ChannelRegistry.class)
        public ChannelRegistry channelRegistry() {
            return new ChannelRegistry();
        }

        @Bean
       // @ConditionalOnMissingBean(name = "pcAllinpayChannel")
        public PayChannel pcAllinpayChannel(ChannelRegistry channelRegistry,
                                          TokenProvider<PcAllinpayToken> tokenProvider) {
            PcAllinpayChannel alipayChannel = new PcAllinpayChannel(tokenProvider);
            channelRegistry.register(AllinpayChannels.PC, alipayChannel);
            return alipayChannel;
        }
    }




}
