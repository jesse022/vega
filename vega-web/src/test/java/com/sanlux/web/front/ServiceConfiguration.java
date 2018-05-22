package com.sanlux.web.front;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by cuiwentao
 * on 16/8/23
 */
@Configuration
@Import(DataSourceAutoConfiguration.class)
@ComponentScan
public class ServiceConfiguration {


}
