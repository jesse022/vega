package com.sanlux.pay;

import com.sanlux.pay.allinpay.component.AppAllinpayTokenProvider;
import com.sanlux.pay.allinpay.component.WapAllinpayTokenProvider;
import com.sanlux.pay.allinpay.constants.AllinpayChannels;
import com.sanlux.pay.allinpay.paychannel.AppAllinpayChannel;
import com.sanlux.pay.allinpay.paychannel.WapAllinpayChannel;
import com.sanlux.pay.allinpay.token.AppAllinpayToken;
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
public class AppAllinpayAutoConfig {

    @Configuration
    @ConditionalOnProperty(name = "pay.token.provider.version", havingValue = "yaml", matchIfMissing = true)
    @EnableConfigurationProperties({AppAllinpayToken.class})
    public static class TokenProviderConfig{

        @Bean
        public AppAllinpayTokenProvider appAllinpayTokenProvider(AppAllinpayToken token){
            return new AppAllinpayTokenProvider(token);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "pay.channel.allinpay.app.enable", havingValue = "true", matchIfMissing = true)
    public static class PayConfig {
        @Bean
        @ConditionalOnMissingBean(ChannelRegistry.class)
        public ChannelRegistry channelRegistry() {
            return new ChannelRegistry();
        }

        @Bean
        public PayChannel appAllinpayChannel(ChannelRegistry channelRegistry,
                                          TokenProvider<AppAllinpayToken> tokenProvider) {
            AppAllinpayChannel alipayChannel = new AppAllinpayChannel(tokenProvider);
            channelRegistry.register(AllinpayChannels.APP, alipayChannel);
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
