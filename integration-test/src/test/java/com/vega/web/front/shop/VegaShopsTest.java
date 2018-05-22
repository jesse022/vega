/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.vega.web.front.shop;

import com.vega.web.BaseWebTest;
import com.vega.web.configuration.front.FrontWebConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author : panxin
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(FrontWebConfiguration.class)
public class VegaShopsTest extends BaseWebTest {
}
