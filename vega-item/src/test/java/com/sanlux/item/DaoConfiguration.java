/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.item;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Author:cp
 * Created on 8/2/16
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan({"com.sanlux.item.impl.dao",
        "com.sanlux.category.impl.dao",
        "com.sanlux.shop.impl.dao",
        "io.terminus.parana.shop.impl.dao",
        "io.terminus.parana.item.impl.dao"})
public class DaoConfiguration {
}