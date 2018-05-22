package com.sanlux.web.front;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Effet
 */
@SpringBootApplication
public class VegaWebApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VegaWebApplication.class);
        YamlPropertiesFactoryBean yml = new YamlPropertiesFactoryBean();
        yml.setResources(new ClassPathResource("env/default.yml"));
        application.setDefaultProperties(yml.getObject());
        application.setBanner(new ParanaBanner());
        application.run(args);

    }
}
