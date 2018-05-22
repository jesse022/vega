/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.config;

import com.sanlux.trade.settle.component.VegaSummaryRule;
import org.springframework.context.annotation.Bean;

/**
 * @author : panxin
 */
//@Configuration
public class VegaSummaryRuleConfig {

    @Bean
    public VegaSummaryRule summaryRule(){
        return new VegaSummaryRule();
    }

}
