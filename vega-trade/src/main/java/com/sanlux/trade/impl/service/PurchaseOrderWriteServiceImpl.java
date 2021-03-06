package com.sanlux.trade.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.trade.impl.dao.PurchaseOrderDao;
import com.sanlux.trade.impl.manager.PurchaseOrderManager;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.service.PurchaseOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 写服务实现类
 * Date: 2016-08-09
 */
@Slf4j
@Service
@RpcProvider
public class PurchaseOrderWriteServiceImpl implements PurchaseOrderWriteService {

    @Autowired
    private PurchaseOrderDao purchaseOrderDao;

    @Autowired
    private PurchaseOrderManager purchaseOrderManager;



    @Override
    public Response<Long> createPurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            PurchaseOrder exist = purchaseOrderDao.findByBuyerIdAndName(purchaseOrder.getBuyerId(),purchaseOrder.getName());
            if(Arguments.notNull(exist)){
                log.error("find create purchase order: {},name is exist",purchaseOrder);
                return Response.fail("purchase.name.exist");
            }

            List<PurchaseOrder> purchaseOrders = purchaseOrderDao.findByBuyerIdNotTemp(purchaseOrder.getBuyerId(), 0);
            if(!CollectionUtils.isEmpty(purchaseOrders)&&purchaseOrders.size()>2){
                log.error("find create purchase order: {}, size gt 3",purchaseOrder);
                return Response.fail("purchase.size.gt.third");
            }

            purchaseOrderDao.create(purchaseOrder);
            return Response.ok(purchaseOrder.getId());
        } catch (Exception e) {
            log.error("create purchaseOrder failed, purchaseOrder:{}, cause:{}", purchaseOrder,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("purchase.order.create.fail");
        }
    }

    @Override
    public Response<Boolean> updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            return Response.ok(purchaseOrderDao.update(purchaseOrder));
        } catch (Exception e) {
            log.error("update purchaseOrder failed, purchaseOrder:{}, cause:{}", purchaseOrder,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("purchase.order.update.fail");
        }
    }

    @Override
    public Response<Boolean> deletePurchaseOrderById(Long purchaseOrderId) {
        try {
            purchaseOrderManager.deletePurchaseOrder(purchaseOrderId);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete purchaseOrder failed, purchaseOrderId:{}, cause:{}", purchaseOrderId,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("purchase.order.delete.fail");
        }
    }
}
