package com.sanlux.item.impl.dao;

import com.sanlux.item.BaseDaoTest;
import com.sanlux.item.model.ShopItem;
import io.terminus.common.model.Paging;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * Author:cp
 * Created on 8/9/16
 */
public class ShopItemDaoTest extends BaseDaoTest {
    private ShopItem shopItem;

    @Autowired
    private ShopItemDao shopItemDao;

    @Before
    public void setUp() {
        shopItem = new ShopItem();
        shopItem.setId(1L);
        shopItem.setShopId(1L);
        shopItem.setItemId(1L);
        shopItem.setItemName("test");
        shopItem.setStatus(1);
        shopItemDao.create(shopItem);
    }

    @Test
    public void testCreate() {
        ShopItem actual = shopItemDao.findById(shopItem.getId());
        Assert.assertNotNull(actual.getId());
    }

    @Test
    public void testDelete() {
        shopItemDao.delete(shopItem.getId());
        ShopItem actual = shopItemDao.findById(shopItem.getId());
        Assert.assertNull(actual);
    }

    @Test
    public void testUpdate() {
        ShopItem toUpdated = new ShopItem();
        toUpdated.setId(shopItem.getId());
        toUpdated.setStatus(-1);
        shopItemDao.update(toUpdated);

        ShopItem actual = shopItemDao.findById(shopItem.getId());
        Assert.assertThat(actual.getStatus(), is(toUpdated.getStatus()));
    }

    @Test
    public void testFindById() {
        ShopItem actual = shopItemDao.findById(shopItem.getId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testFindByIds() {
        List<ShopItem> list = shopItemDao.findByIds(Arrays.asList(shopItem.getId()));
        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void testPaging() {
        Paging<ShopItem> paging = shopItemDao.paging(0, 20, shopItem);
        Assert.assertTrue(!paging.getData().isEmpty());
    }
}
