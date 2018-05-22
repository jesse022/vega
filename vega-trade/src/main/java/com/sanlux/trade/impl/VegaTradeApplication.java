package com.sanlux.trade.impl;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

/**
 * Desc:
 * Mail: F@terminus.io
 * Data: 16/3/7
 * Author: yangzefeng
 */
@SpringBootApplication
public class VegaTradeApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VegaTradeApplication.class);
        YamlPropertiesFactoryBean yml = new YamlPropertiesFactoryBean();
        yml.setResources(new ClassPathResource("env/default.yml"));
        application.setDefaultProperties(yml.getObject());
        application.setBanner(new ParanaBanner());
        application.run(args);
    }
}
