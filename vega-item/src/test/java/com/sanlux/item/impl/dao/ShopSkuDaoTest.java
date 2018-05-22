package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sanlux.item.BaseDaoTest;
import com.sanlux.item.dto.ShopSkuCriteria;
import com.sanlux.item.model.ShopSku;
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
 * Created on 8/2/16
 */
public class ShopSkuDaoTest extends BaseDaoTest {

    private ShopSku shopSku;

    @Autowired
    private ShopSkuDao shopSkuDao;

    @Before
    public void setUp() throws Exception {
        shopSku = new ShopSku();
        shopSku.setId(1L);
        shopSku.setShopId(1L);
        shopSku.setCategoryId(1L);
        shopSku.setItemId(1L);
        shopSku.setSkuId(1L);
        shopSku.setStatus(1);
        shopSku.setPrice(1);
        shopSku.setExtraPrice(ImmutableMap.of("level1", 123));
        shopSku.setStockType(1);
        shopSku.setStockQuantity(1);
        shopSkuDao.create(shopSku);
    }

    @Test
    public void testCreate() {
        ShopSku actual = shopSkuDao.findById(shopSku.getId());
        Assert.assertNotNull(actual.getId());
    }

    @Test
    public void testDelete() {
        shopSkuDao.delete(shopSku.getId());
        ShopSku actual = shopSkuDao.findById(shopSku.getId());
        Assert.assertNull(actual);
    }

    @Test
    public void testUpdate() {
        ShopSku toUpdated = new ShopSku();
        toUpdated.setId(shopSku.getId());
        toUpdated.setStockQuantity(20);
        shopSkuDao.update(toUpdated);

        ShopSku actual = shopSkuDao.findById(shopSku.getId());
        Assert.assertThat(actual.getStockQuantity(), is(toUpdated.getStockQuantity()));
    }

    @Test
    public void testFindById() {
        ShopSku actual = shopSkuDao.findById(shopSku.getId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testFindByIds() {
        List<ShopSku> list = shopSkuDao.findByIds(Arrays.asList(shopSku.getId()));
        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void testFindByShopIdAndItemIds() {
        List<ShopSku> list = shopSkuDao.findByShopIdAndItemIds(shopSku.getShopId(),Arrays.asList(shopSku.getItemId()));
        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void testFindByShopIdAndItemId() {
        List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopSku.getShopId(),shopSku.getItemId());
        Assert.assertTrue(!shopSkus.isEmpty());
    }

    @Test
    public void testFindByShopIdAndSkuId() {
        ShopSku actual = shopSkuDao.findByShopIdAndSkuId(shopSku.getShopId(), shopSku.getSkuId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testFindByShopIdAndSkuIds() {
        List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndSkuIds(shopSku.getShopId(), Arrays.asList(shopSku.getSkuId()));
        Assert.assertTrue(!shopSkus.isEmpty());
    }

    @Test
    public void testPaging () {
        ShopSkuCriteria criteria = new ShopSkuCriteria();
        criteria.setCategoryId(1L);
        criteria.setShopId(1L);
        criteria.setPageNo(1);
        criteria.setPageSize(20);
        Paging<ShopSku> paging = shopSkuDao.paging(criteria.toMap());
        Assert.assertTrue(paging.getTotal() != null);
    }

    @Test
    public void testBatchUpdateStockByShopIdAndSkuId () {
        ShopSku test = new ShopSku();
        test.setItemId(1L);
        test.setShopId(2L);
        test.setSkuId(100L);
        test.setStockQuantity(99);
        shopSkuDao.create(test);
        List<ShopSku> shopSkus = Lists.newArrayList();
        shopSku.setStockQuantity(88);
        shopSku.setPrice(1000);
        shopSkus.add(shopSku);
        shopSkus.add(test);
        shopSkuDao.batchUpdateByShopIdAndSkuId(shopSkus);

        ShopSku demo = shopSkuDao.findByShopIdAndSkuId(shopSku.getShopId(), shopSku.getSkuId());
        Assert.assertTrue(88 == demo.getStockQuantity());
        Assert.assertTrue(1000 == demo.getPrice());

    }

}
