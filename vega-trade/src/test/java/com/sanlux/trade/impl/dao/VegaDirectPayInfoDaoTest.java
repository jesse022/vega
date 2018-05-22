package com.sanlux.trade.impl.dao;

import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.model.VegaDirectPayInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by liangfujie on 16/10/28
 */
public class VegaDirectPayInfoDaoTest extends BaseDaoTest {

    @Autowired
    private VegaDirectPayInfoDao vegaDirectPayInfoDao;

    private VegaDirectPayInfo vegaDirectPayInfo;


    @Before
    public  void  init(){
        vegaDirectPayInfo =new VegaDirectPayInfo();
        vegaDirectPayInfo.setStatus(1);
        vegaDirectPayInfo.setBusinessId("201611031413150000000000001143");
        vegaDirectPayInfo.setOrderId(12345l);
       vegaDirectPayInfoDao.create(vegaDirectPayInfo);
        assertNotNull(vegaDirectPayInfo.getId());

        vegaDirectPayInfo =new VegaDirectPayInfo();
        vegaDirectPayInfo.setStatus(-1);
        vegaDirectPayInfo.setBusinessId("201611031413150000000000201143");
        vegaDirectPayInfo.setOrderId(12345l);
        vegaDirectPayInfoDao.create(vegaDirectPayInfo);
        assertNotNull(vegaDirectPayInfo.getId());
    }

    @Test
    public void findOrderId(){
        VegaDirectPayInfo VegaDirectPayInfo1 = vegaDirectPayInfoDao.findByOrderId(12345l);
        System.out.println(VegaDirectPayInfo1.getStatus());

    }

    @Test
    public void findById() {
        VegaDirectPayInfo VegaDirectPayInfo1 = vegaDirectPayInfoDao.findById(vegaDirectPayInfo.getId());

        assertNotNull(VegaDirectPayInfo1);
    }


    @Test
    public void fun(){
        vegaDirectPayInfoDao.updateStatusByBusinessId("201611031413150000000000001143",2);
    }

    @Test
    public void findByBusinessId(){

        VegaDirectPayInfo vegaDirectPayInfo2 = vegaDirectPayInfoDao.findByBusinessId("201611031413150000000000001143");
        assertNotNull(vegaDirectPayInfo2);
    }

}
