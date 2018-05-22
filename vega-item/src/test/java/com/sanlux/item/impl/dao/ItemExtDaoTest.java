package com.sanlux.item.impl.dao;

import com.google.common.collect.Lists;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.BaseDaoTest;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.model.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by cuiwentao
 * on 16/10/21
 */
public class ItemExtDaoTest extends BaseDaoTest {

    @Autowired
    private ItemExtDao itemExtDao;

    @Autowired
    private ItemDao itemDao;


    private Item item;


    @Before
    public void init() {
        item = make();

        itemDao.create(item);
        assertNotNull(item.getId());
    }

    @Test
    public void testUpdateImageByCategoryId() {
        itemExtDao.updateImageByCategoryIdAndShopId(item.getCategoryId(), item.getShopId(), "test");

        Item test = itemDao.findById(item.getId());

        Assert.assertEquals(test.getMainImage(), "test");
    }

    @Test
    public void testFindItemIdsByCategoryIdAndShopId() {
        List<Integer> statuses = Lists.newArrayList(DefaultItemStatus.ITEM_ONSHELF,
                DefaultItemStatus.ITEM_FREEZE,
                DefaultItemStatus.ITEM_WAIT_AUDIT,
                DefaultItemStatus.ITEM_REFUSE);
        List<Long> test = itemExtDao.findItemIdsByCategoryIdAndShopId(item.getCategoryId(), item.getShopId(), statuses);

        Assert.assertTrue(!test.isEmpty());
    }

    @Test
    public void testBatchUpdateItemInfoMd5() {

        Item demo = make();
        demo.setItemInfoMd5("md5");
        itemDao.create(demo);

        List<Item> items = Lists.newArrayList();
        item.setItemInfoMd5("md5");
        items.add(item);
        items.add(demo);
        itemExtDao.batchUpdateItemInfoMd5(items);

        Item test = itemDao.findById(demo.getId());
        Assert.assertEquals(test.getItemInfoMd5(), "md5");
    }

    @Test
    public void testBatchUpdateStockByShopIdAndId () {
        Item demo = make();
        demo.setStockQuantity(100);
        itemDao.create(demo);

        List<Item> items = Lists.newArrayList();
        item.setStockQuantity(99);
        items.add(item);
        items.add(demo);

        itemExtDao.batchUpdateStockByShopIdAndId(items);

        Item test = itemDao.findById(demo.getId());
        Assert.assertTrue(100 == test.getStockQuantity());
    }

    @Test
    public void findItemsByCategoryIds () {
        List<Long> categoryIds = Lists.newArrayList();
        categoryIds.add(28L);
        List<Item> items = itemExtDao.findItemsByCategoryIds(categoryIds);
        Assert.assertTrue(!items.isEmpty());
    }

    private Item make() {

        Item item = new Item();

        item.setCategoryId(28L);
        item.setShopId(100L);
        item.setItemCode("code");
        item.setStatus(1);
        return item;
    }

}
