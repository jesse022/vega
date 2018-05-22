/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.user.impl.dao;

import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 模块测试
 * <p>
 * Author  : panxin
 * Date    : 3:34 PM 3/3/16
 * Mail    : panxin@terminus.io
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan({"com.sanlux.user.impl.dao"})
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class DaoConfiguration {
}
