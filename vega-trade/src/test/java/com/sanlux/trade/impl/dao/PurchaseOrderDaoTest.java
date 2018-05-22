package com.sanlux.trade.impl.dao;

import com.sanlux.trade.model.PurchaseOrder;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/9/16
 * Time: 1:44 PM
 */
public class PurchaseOrderDaoTest extends BaseDaoTest{

    @Autowired
    private PurchaseOrderDao purchaseOrderDao;

    private PurchaseOrder purchaseOrder;

    @Before
    public void init() {
        purchaseOrder = make();

        purchaseOrderDao.create(purchaseOrder);
        assertNotNull(purchaseOrder.getId());
    }

    @Test
    public void findById() {
        PurchaseOrder purchaseOrderExist = purchaseOrderDao.findById(purchaseOrder.getId());

        assertNotNull(purchaseOrderExist);
    }

    @Test
    public void update() {
        //
        purchaseOrder.setSkuQuantity(23);
        purchaseOrderDao.update(purchaseOrder);

        PurchaseOrder  updated = purchaseOrderDao.findById(purchaseOrder.getId());
        assertEquals(updated.getSkuQuantity(), Integer.valueOf(23));
    }

    @Test
    public void delete() {
        purchaseOrderDao.delete(purchaseOrder.getId());

        PurchaseOrder deleted = purchaseOrderDao.findById(purchaseOrder.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("buyerId", purchaseOrder.getBuyerId());
        Paging<PurchaseOrder > purchaseOrderPaging = purchaseOrderDao.paging(0, 20, params);

        assertThat(purchaseOrderPaging.getTotal(), is(1L));
        assertEquals(purchaseOrderPaging.getData().get(0).getId(), purchaseOrder.getId());
    }

    @Test
    public void testFindByBuyerId(){
        assertNotNull(purchaseOrderDao.findByBuyerId(purchaseOrder.getBuyerId()));

    }

    private PurchaseOrder make() {
        PurchaseOrder purchaseOrder = new PurchaseOrder();


        purchaseOrder.setName("采购清单1");

        purchaseOrder.setBuyerId(2l);

        purchaseOrder.setBuyerName("name");

        purchaseOrder.setSkuQuantity(23);

        //purchaseOrder.setExtraJson("2222");

        purchaseOrder.setCreatedAt(new Date());

        purchaseOrder.setUpdatedAt(new Date());


        return purchaseOrder;
    }


}