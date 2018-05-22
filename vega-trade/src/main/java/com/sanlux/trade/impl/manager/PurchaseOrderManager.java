package com.sanlux.trade.impl.manager;

import com.sanlux.trade.enums.PurchaseSkuOrderStatus;
import com.sanlux.trade.impl.dao.PurchaseOrderDao;
import com.sanlux.trade.impl.dao.PurchaseSkuOrderDao;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/9/16
 * Time: 6:14 PM
 */
@Slf4j
@Component
public class PurchaseOrderManager {

    @Autowired
    private PurchaseOrderDao purchaseOrderDao;

    @Autowired
    private PurchaseSkuOrderDao purchaseSkuOrderDao;



    @Transactional
    public void deletePurchaseOrder(Long purchaseOrderId){
        purchaseOrderDao.delete(purchaseOrderId);
        purchaseSkuOrderDao.deleteByPurchaseOrderId(purchaseOrderId);
    }



    @Transactional
    public void deletePurchaseSkuOrder(PurchaseOrder purchaseOrder,PurchaseSkuOrder purchaseSkuOrder){
        purchaseSkuOrderDao.delete(purchaseSkuOrder.getId());
        PurchaseOrder purchaseOrderUpdate = new PurchaseOrder();
        //更新采购单商品数
        purchaseOrder.setSkuQuantity(purchaseOrder.getSkuQuantity()-1);
        purchaseOrderDao.update(purchaseOrderUpdate);
    }

    @Transactional
    public void createPurchaseSkuOrer(PurchaseSkuOrder purchaseSkuOrder,PurchaseOrder exist){
        purchaseSkuOrderDao.create(purchaseSkuOrder);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        //更新采购单商品数
        purchaseOrder.setSkuQuantity(exist.getSkuQuantity()+1);
        purchaseOrderDao.update(purchaseOrder);
    }


    @Transactional
    public Long changeTempPurchaseSkuOrder(Sku sku, Integer quantity, PurchaseOrder purchaseOrder, Long userId,
                                           String userName){

        if(!Arguments.isNull(purchaseOrder)){
            //删除之前的临时商品
            purchaseSkuOrderDao.deleteByPurchaseOrderId(purchaseOrder.getId());
        }else{
            purchaseOrder = new PurchaseOrder();
            purchaseOrder.setIsTemp(Boolean.TRUE);
            purchaseOrder.setBuyerId(userId);
            purchaseOrder.setBuyerName(userName);
            purchaseOrder.setSkuQuantity(1);
            purchaseOrder.setName("临时采购单-立即购买");
            purchaseOrder.setCreatedAt(new Date());
            purchaseOrder.setUpdatedAt(new Date());
            purchaseOrderDao.create(purchaseOrder);
        }

        PurchaseSkuOrder purchaseSkuOrder = new PurchaseSkuOrder();
        purchaseSkuOrder.setPurchaseId(purchaseOrder.getId());
        purchaseSkuOrder.setBuyerId(userId);
        purchaseSkuOrder.setBuyerName(userName);
        purchaseSkuOrder.setCreatedAt(new Date());
        purchaseSkuOrder.setStatus(PurchaseSkuOrderStatus.CHOOSED.value());
        purchaseSkuOrder.setQuantity(quantity);
        purchaseSkuOrder.setShopId(sku.getShopId());
        purchaseSkuOrder.setShopName("");
        purchaseSkuOrder.setSkuId(sku.getId());
        purchaseSkuOrder.setUpdatedAt(new Date());
        purchaseSkuOrder.setCreatedAt(new Date());

        purchaseSkuOrderDao.create(purchaseSkuOrder);

        return purchaseOrder.getId();

    }


    @Transactional
    public void batchchangePurchaseSkuOrer(List<PurchaseSkuOrder> purchaseSkuOrderUpdates,List<PurchaseSkuOrder> purchaseSkuOrderCreates){

        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrderCreates){

            purchaseSkuOrderDao.create(purchaseSkuOrder);
        }

        for (PurchaseSkuOrder  purchaseOrderSkuUpdate : purchaseSkuOrderUpdates){
            purchaseSkuOrderDao.update(purchaseOrderSkuUpdate);

        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        //更新采购单商品数
        purchaseOrder.setSkuQuantity(purchaseSkuOrderCreates.size()+1);
        purchaseOrderDao.update(purchaseOrder);
    }

    @Transactional
    public void batchaDeletePurchaseSkuOrder(List<Long> skuIds,PurchaseOrder exist){

        purchaseSkuOrderDao.deleteByPurchaseOrderIdAndSkuIds(exist.getId(),skuIds);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        //更新采购单商品数
        purchaseOrder.setSkuQuantity(exist.getSkuQuantity()-skuIds.size());
        purchaseOrderDao.update(purchaseOrder);

    }
}
