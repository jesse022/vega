package com.sanlux.pay;

import com.sanlux.pay.allinpay.component.WapAllinpayTokenProvider;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.paychannel.WapAllinpayChannel;
import com.sanlux.pay.allinpay.token.WapAllinpayToken;
import io.terminus.pay.api.ChannelRegistry;
import io.terminus.pay.api.TokenProvider;
import io.terminus.pay.service.PayChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DATE: 16/8/22 下午2:22 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Configuration
public class WapAllinpayAutoConfig {

    @Configuration
    @ConditionalOnProperty(name = "pay.token.provider.version", havingValue = "yaml", matchIfMissing = true)
    @EnableConfigurationProperties({WapAllinpayToken.class})
    public static class TokenProviderConfig{

        @Bean
        public WapAllinpayTokenProvider wapAllinpayTokenProvider(WapAllinpayToken token){
            return new WapAllinpayTokenProvider(token);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "pay.channel.allinpay.wap.enable", havingValue = "true", matchIfMissing = true)
    public static class PayConfig {
        @Bean
        @ConditionalOnMissingBean(ChannelRegistry.class)
        public ChannelRegistry channelRegistry() {
            return new ChannelRegistry();
        }

        @Bean
        public PayChannel wapAllinpayChannel(ChannelRegistry channelRegistry,
                                          TokenProvider<WapAllinpayToken> tokenProvider) {
            WapAllinpayChannel alipayChannel = new WapAllinpayChannel(tokenProvider);
            channelRegistry.register(AllinpayChannels.WAP, alipayChannel);
            return alipayChannel;
        }
    }
 /*   @Configuration
    @ConditionalOnProperty(name = "pay.cron.trans.allinpay.pc.enable", havingValue = "true")
    @ComponentScan({
            "com.sanlux.pay.allinpay.job", "com.sanlux.pay.allinpay.trans.component"
    })
    public static class TransConfig{

    }
*/


}
