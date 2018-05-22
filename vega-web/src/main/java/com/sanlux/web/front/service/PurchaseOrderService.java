package com.sanlux.web.front.service;

import com.google.common.base.Optional;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.service.PurchaseOrderReadService;
import com.sanlux.trade.service.PurchaseSkuOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/22/16
 * Time: 11:21 AM
 */
@Slf4j
@Service
public class PurchaseOrderService {

    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;




    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId){

        //检测当前采购单是否存在
        Response<Optional<PurchaseOrder>> existRes = purchaseOrderReadService.findPurchaseOrderById(purchaseOrderId);
        if(!existRes.isSuccess()){
            log.error("find purchase order by id:{} fail,error:{}",purchaseOrderId,existRes.getError());
            throw new JsonResponseException(existRes.getError());
        }

        if(!existRes.getResult().isPresent()){
            log.error("not find purchase order by id:{}",purchaseOrderId);
            throw new JsonResponseException("purchase.order.not.exist");
        }

        return existRes.getResult().get();

    }


    public List<PurchaseSkuOrder> getPurchaseSkuOrdersByPurchaseOrderId(Long purchaseOrderId){

        //检测当前采购单是否存在
        Response<List<PurchaseSkuOrder>> skuRes = purchaseSkuOrderReadService.finByPurchaseOrderIdAndStatus(purchaseOrderId, 1);
        if(!skuRes.isSuccess()){
            log.error("find purchase sku order by purchase order id:{} status:{} fail,error:{}",purchaseOrderId,1,skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        return skuRes.getResult();

    }



    public List<PurchaseSkuOrder> getPurchaseSkuOrdersByPurchaseOrderAndShopId(Long purchaseOrderId,Long shopId){

        //检测当前采购单是否存在
        Response<List<PurchaseSkuOrder>> skuRes = purchaseSkuOrderReadService.finByPurchaseOrderIdAndStatusAndShopId(purchaseOrderId, 1, shopId);
        if(!skuRes.isSuccess()){
            log.error("find purchase sku order by purchase order id:{} status:{} shop id:{} fail,error:{}",purchaseOrderId,1,shopId,skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        return skuRes.getResult();

    }

}
