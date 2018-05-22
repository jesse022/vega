package com.sanlux.trade.impl.manager;

import com.sanlux.trade.impl.dao.OrderDispatchRelationDao;
import com.sanlux.trade.model.OrderDispatchRelation;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/23/16
 * Time: 5:28 PM
 */
@Slf4j
@Component
public class DispatchOrderManager {

    @Autowired
    private OrderDispatchRelationDao orderDispatchRelationDao;
    @Autowired
    private ShopOrderDao shopOrderDao;
    @Autowired
    private SkuOrderDao skuOrderDao;



    @Transactional
    public void createDispatchRelation(OrderDispatchRelation relation,ShopOrder update,Integer currentStatus){

        //创建关联关系
        orderDispatchRelationDao.create(relation);
        //更新订单中的shopId
        shopOrderDao.updateShopInfoById(update.getId(), update.getShopId(), update.getShopName());
        //更新订单状态
        shopOrderDao.updateStatus(update.getId(),update.getStatus());
        //更新子订单中的shopId
        skuOrderDao.updateShopInfoIdByOrderId(update.getId(),update.getShopId(),update.getShopName());
        //更新子订单中的状态
        skuOrderDao.updateStatusByOrderId(update.getId(),currentStatus,update.getStatus());


    }
}
