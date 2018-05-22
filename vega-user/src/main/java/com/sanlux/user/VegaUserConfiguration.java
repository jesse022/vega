package com.sanlux.user;

import io.terminus.parana.UserAutoConfig;
import io.terminus.parana.article.impl.ArticleAutoConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Effet
 */
@Configuration
@ComponentScan
@Import({UserAutoConfig.class, ArticleAutoConfig.class})
public class VegaUserConfiguration {
}
