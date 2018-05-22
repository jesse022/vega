package com.sanlux.item.impl.dao;

import com.google.common.collect.Lists;
import com.sanlux.item.BaseDaoTest;
import io.terminus.parana.item.impl.dao.ItemDetailDao;
import io.terminus.parana.item.model.ItemDetail;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/10/24
 */
public class ItemDetailExtDaoTest extends BaseDaoTest {

    @Autowired
    private ItemDetailExtDao itemDetailExtDao;

    @Autowired
    private ItemDetailDao itemDetailDao;

    private ItemDetail itemDetail;

    @Before
    public void init() {
        itemDetail = make();

        itemDetailDao.create(itemDetail);
    }

    @Test
    public void testBatchUpdateItemDetail() {

        ItemDetail demo = make();
        demo.setItemId(101L);
        itemDetailDao.create(demo);
        List<ItemDetail> itemDetails = Lists.newArrayList();
        itemDetail.setDetail("detail");
        itemDetails.add(itemDetail);
        itemDetails.add(demo);
        itemDetailExtDao.batchUpdateItemDetail(itemDetails);

        ItemDetail test = itemDetailDao.findByItemId(100L);
        Assert.assertEquals(test.getDetail(), "detail");
    }

    @Test
    public void testFindByItemIds() {
        ItemDetail demo = make();
        demo.setItemId(101L);
        itemDetailDao.create(demo);
        List<Long> itemIds = Lists.newArrayList();
        itemIds.add(100L);
        itemIds.add(101L);
        List<ItemDetail> itemDetails = itemDetailExtDao.findItemDetailsByItemIds(itemIds);
        Assert.assertTrue(itemDetails.size() != 0);
    }


    private ItemDetail make() {

        ItemDetail itemDetail = new ItemDetail();

        itemDetail.setItemId(100L);

        return itemDetail;
    }
}
