package com.sanlux.item.impl.dao;

import com.google.common.collect.Lists;
import com.sanlux.item.BaseDaoTest;
import io.terminus.common.model.Paging;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Sku;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Before;

import java.util.List;
import java.util.Objects;


/**
 * Created by cuiwentao
 * on 16/10/28
 */
public class SkuExtDaoTest extends BaseDaoTest {

    private Sku sku;

    @Autowired
    private SkuExtDao skuExtDao;

    @Autowired
    private SkuDao skuDao;

    @Before
    public void setUp() throws Exception {
        sku = new Sku();
        sku.setShopId(1L);
        sku.setOuterSkuId("outerSkuId");
        sku.setStockQuantity(1);
        skuDao.create(sku);
    }

    @Test
    public void testBatchUpdateStockByShopIdAndOuterSkuId () {
        List<Sku> skus = Lists.newArrayList();
        sku.setStockQuantity(100);
        skus.add(sku);

        skuExtDao.batchUpdateStockByShopIdAndId(skus);

        Sku test = skuDao.findById(sku.getId());
        Assert.assertTrue(100 == test.getStockQuantity());
    }

    @Test
    public void testFindByOuterSkuIds () {
        List<String> outerSkuIds = Lists.newArrayList();
        outerSkuIds.add(sku.getOuterSkuId());
        List<Sku> test = skuExtDao.findByOuterSkuIds(outerSkuIds);

        Assert.assertTrue(test.size() != 0);
    }

    @Test
    public void testPaging () {
        Paging<Sku> skuPaging = skuExtDao.paging(0,5);
        Assert.assertTrue(skuPaging.getTotal() != 0);
        Assert.assertTrue(!skuPaging.getData().isEmpty());
    }

    @Test
    public void testBatchUpdateOuterSkuId () {
        List<Sku> skus = Lists.newArrayList();
        sku.setOuterSkuId("test");
        skus.add(sku);
        skuExtDao.batchUpdateOuterSkuId(skus);
        Sku test = skuDao.findById(sku.getId());
        Assert.assertTrue(Objects.equals("test", test.getOuterSkuId()));
    }
}
