/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.vega.web.admin.shop;

import com.sanlux.common.enums.VegaShopType;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.dto.VegaShopUserDto;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.model.VegaShopExtra;
import com.vega.web.BaseWebTest;
import com.vega.web.configuration.admin.AdminWebConfiguration;
import io.terminus.parana.shop.model.Shop;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author : panxin
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AdminWebConfiguration.class)
public class AdminVegaShopsTest extends BaseWebTest {

    private String url = "";

    @Test
    public void testFindShopById() {
        url = "/api/vega/admin/shops/53";
        VegaShop vegaShop = getForObject(url, VegaShop.class);
        assertNotNull(vegaShop.getShop());
    }

    @Test
    public void testFindByName() {
        url = "/api/vega/admin/shops/first-level-exists?shopName=dealer1-10";
        Boolean isExists = getForObject(url, Boolean.class);
        assertTrue(isExists);
    }

    @Test
    public void testCreate() {
        url = "/api/vega/admin/shops";
        VegaShopUserDto shopDto = mockShopForm(VegaShopType.SUPPLIER.value());

        Long shopId = postForObject(url, shopDto, Long.class);
        assertNotNull(shopId);
    }

    private VegaShopUserDto mockShopForm(Integer shopType) {
        VegaShopUserDto dto = new VegaShopUserDto();

        Shop shop = mockShop(shopType);
        VegaShopExtra shopExtra = mockShopExtra(shop);

        dto.setShop(shop);
        dto.setShopExtra(shopExtra);

        dto.setMobile("18673231309");
        dto.setUserName("userName");
        dto.setPassword("password");

        return dto;
    }

    private VegaShopExtra mockShopExtra(Shop shop) {
        VegaShopExtra shopExtra = new VegaShopExtra();

        shopExtra.setShopName(shop.getName());
        // // TODO: 9/27/16

        return shopExtra;
    }

    private Shop mockShop(Integer shopType) {
        Shop shop = new Shop();

        shop.setType(shopType);
        shop.setName("mockShop");
        shop.setStatus(VegaShopStatus.NORMAL.value());

        return shop;
    }

}
