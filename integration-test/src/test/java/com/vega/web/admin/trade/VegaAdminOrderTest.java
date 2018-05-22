package com.vega.web.admin.trade;

import com.google.common.collect.Maps;
import com.vega.web.BaseWebTest;
import com.vega.web.configuration.admin.AdminWebConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/7/16
 * Time: 10:47 PM
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AdminWebConfiguration.class)
public class VegaAdminOrderTest extends BaseWebTest {


    @Test
    public void testAdminCancelOrder()throws Exception{
        Map<String, Object> form = Maps.newHashMap();
        form.put("orderId", 5L);
        form.put("orderType", 1);
        Boolean isCanceled = postFormForObject("/api/admin/vega/order/cancel", form, Boolean.class);
        assertTrue(isCanceled);
    }
}
