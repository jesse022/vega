package com.sanlux.trade.impl.settle.dao;

import com.sanlux.trade.impl.dao.BaseDaoTest;
import com.sanlux.trade.settle.model.AllinpayTrans;
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
 * Date: 10/20/16
 * Time: 5:04 PM
 */
public class AllinpayTransDaoTest extends BaseDaoTest {



    @Autowired
    private AllinpayTransDao allinpayTransDao;

    private AllinpayTrans allinpayTrans;

    @Before
    public void init() {
        allinpayTrans = make();

        allinpayTransDao.create(allinpayTrans);
        assertNotNull(allinpayTrans.getId());
    }

    @Test
    public void findById() {
        AllinpayTrans allinpayTransExist = allinpayTransDao.findById(allinpayTrans.getId());

        assertNotNull(allinpayTransExist);
    }

    @Test
    public void update() {
        allinpayTrans.setServiceFee("555");
        allinpayTrans.setUpdatedAt(new Date());
        allinpayTransDao.update(allinpayTrans);

        AllinpayTrans  updated = allinpayTransDao.findById(allinpayTrans.getId());
        assertEquals(updated.getServiceFee(), "555");
    }

    @Test
    public void delete() {
        allinpayTransDao.delete(allinpayTrans.getId());

        AllinpayTrans deleted = allinpayTransDao.findById(allinpayTrans.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        //todo
        //params.put("userId", allinpayTrans.getUserId());
        Paging<AllinpayTrans > allinpayTransPaging = allinpayTransDao.paging(0, 20, params);

        assertThat(allinpayTransPaging.getTotal(), is(1L));
        assertEquals(allinpayTransPaging.getData().get(0).getId(), allinpayTrans.getId());
    }

    private AllinpayTrans make() {
        AllinpayTrans allinpayTrans = new AllinpayTrans();


        allinpayTrans.setTransCodeMsg("23");

        allinpayTrans.setTransDate("23");

        allinpayTrans.setSellerAccount("23");

        allinpayTrans.setTradeAt(new Date());

        allinpayTrans.setTransOutOrderNo("23");

        allinpayTrans.setTradeNo("23");

        allinpayTrans.setTotalFee("23");

        allinpayTrans.setServiceFee("23");

        allinpayTrans.setServiceFeeRatio("23");

        allinpayTrans.setSettlementFee("23");

        allinpayTrans.setCurrency("23");

        allinpayTrans.setOrderOriginFee("23");

        allinpayTrans.setMemo("23");

        allinpayTrans.setCreatedAt(new Date());

        allinpayTrans.setUpdatedAt(new Date());


        return allinpayTrans;
    }

}