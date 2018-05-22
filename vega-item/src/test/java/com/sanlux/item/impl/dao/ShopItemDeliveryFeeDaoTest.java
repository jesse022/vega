package com.sanlux.item.impl.dao;

import com.sanlux.item.BaseDaoTest;
import com.sanlux.item.model.ShopItemDeliveryFee;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

/**
 * Author:cp
 * Created on 8/16/16.
 */
public class ShopItemDeliveryFeeDaoTest extends BaseDaoTest {

    private ShopItemDeliveryFee shopItemDeliveryFee;

    @Autowired
    private ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    @Before
    public void setUp() {
        shopItemDeliveryFee = new ShopItemDeliveryFee();
        shopItemDeliveryFee.setId(1L);
        shopItemDeliveryFee.setShopId(1L);
        shopItemDeliveryFee.setItemId(1L);
        shopItemDeliveryFee.setDeliveryFee(1);
        shopItemDeliveryFee.setDeliveryFeeTemplateId(1L);
        shopItemDeliveryFeeDao.create(shopItemDeliveryFee);
    }

    @Test
    public void testCreate() {
        ShopItemDeliveryFee actual = shopItemDeliveryFeeDao.findById(shopItemDeliveryFee.getId());
        Assert.assertNotNull(actual.getId());
    }

    @Test
    public void testUpdate() {
        ShopItemDeliveryFee toUpdated = new ShopItemDeliveryFee();
        toUpdated.setId(shopItemDeliveryFee.getId());
        toUpdated.setDeliveryFeeTemplateId(2L);
        shopItemDeliveryFeeDao.update(toUpdated);

        ShopItemDeliveryFee actual = shopItemDeliveryFeeDao.findById(shopItemDeliveryFee.getId());
        Assert.assertThat(actual.getDeliveryFeeTemplateId(), is(toUpdated.getDeliveryFeeTemplateId()));
    }

    @Test
    public void testFindById() {
        ShopItemDeliveryFee actual = shopItemDeliveryFeeDao.findById(shopItemDeliveryFee.getId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testFindByShopAndItemId() {
        ShopItemDeliveryFee actual = shopItemDeliveryFeeDao.findByShopIdAndItemId(shopItemDeliveryFee.getShopId(), shopItemDeliveryFee.getItemId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testFindByShopAndItemIds() {
        List<ShopItemDeliveryFee> actuals = shopItemDeliveryFeeDao.findByShopIdAndItemIds(shopItemDeliveryFee.getShopId(), Arrays.asList(shopItemDeliveryFee.getItemId()));
        Assert.assertTrue(!CollectionUtils.isEmpty(actuals));
    }

    @Test
    public void testHasBoundTemplate() {
        boolean hasBound = shopItemDeliveryFeeDao.hasBoundTemplate(shopItemDeliveryFee.getDeliveryFeeTemplateId());
        Assert.assertTrue(hasBound);
    }

}
