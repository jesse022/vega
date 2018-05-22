package com.vega.web.front.trade;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.vega.web.BaseWebTest;
import com.vega.web.configuration.front.FrontWebConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/7/16
 * Time: 1:31 PM
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(FrontWebConfiguration.class)
public class PurchaseOrdersTest extends BaseWebTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreatePurchaseOrder() throws Exception {
        Map<String, Object> form = Maps.newHashMap();
        form.put("name", "my purchase oder");
        Long purchaseId = postFormForObject("/api/purchase/order", form, Long.class);
        assertThat(purchaseId).isNotNull();
    }

    @Test
    public void testUpdatePurchaseOrder() throws Exception {

        String name ="采购单1";
        restTemplate.put("http://localhost:{port}api/purchase/order/{id}?name={name}", null,
                ImmutableMap.of("port",getPort(), "id", 1L, "name", name));
    }

    @Test
    public void testCheckPurchaseIsExist() throws Exception {
        //String name ="我的采购单";
        //Boolean isExist = restTemplate.getForObject("http://localhost:api/purchase/name?name={name}", Boolean.class, ImmutableMap.of( "name", name));
        Boolean isExist = getForObject("api/purchase/name?name=我的采购单", Boolean.class);
        assertTrue(isExist);
    }

    @Test
    public void testDeletePurchaseOrder()throws Exception{

        restTemplate.delete("http://localhost:{port}api/purchase/order/{id}",ImmutableMap.of("port",getPort(),"id", 1L));

    }



}
