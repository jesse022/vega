package com.sanlux.trade.impl.service;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import com.sanlux.trade.impl.dao.OrderDispatchRelationDao;
import com.sanlux.trade.impl.manager.VegaOrderManager;
import com.sanlux.trade.service.VegaOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.impl.manager.OrderManager;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/23/16
 * Time: 9:07 PM
 */
@Slf4j
@Service
@RpcProvider
public class VegaOrderWriteServiceImpl implements VegaOrderWriteService {

    @Autowired
    private VegaOrderManager vegaOrderManager;
    @Autowired
    private OrderDispatchRelationDao orderDispatchRelationDao;
    @Autowired
    private ShopOrderDao shopOrderDao;
    @Autowired
    private SkuOrderDao skuOrderDao;
    @Autowired
    private  OrderManager orderManager;


    @Override
    public Response<Boolean> updateShopOrderTagsJsonById(Long shopOrderId, Map<String,String> extra) {
        try {

            vegaOrderManager.updateOrderTags(shopOrderId,extra);
            return Response.ok(Boolean.TRUE);

        }catch (Exception e){
            log.error("update shop order (id={}) extra: {} fail,cause: {}",shopOrderId,extra,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.order.fail");
        }
    }

    @Override
    public Response<Boolean> changeShopOrderShipFeeById(Long orderId, Long newFee, Integer shipFee) {
        try {
            ShopOrder order = new ShopOrder();
            order.setId(orderId);
            order.setFee(newFee);
            order.setShipFee(shipFee);
            vegaOrderManager.changeShopOrderShipFee(order);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to change ship fee by orderId = ({}), cause : {}",
                    orderId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.ship.fee.failed");
        }
    }

    @Override
    public Response<Boolean> changeShopOrderDiffFeeById(Long orderId, Long newFee, Integer diffFee) {
        try {
            ShopOrder order = new ShopOrder();
            order.setId(orderId);
            order.setFee(newFee);
            order.setDiffFee(diffFee);
            vegaOrderManager.changeShopOrderShipFee(order);//共用修改运费方法
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to change diff fee by orderId = ({}), cause : {}",
                    orderId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.diff.fee.failed");
        }
    }

    @Override
    public Response<Boolean> shopOrderStatusChangedForDealerReject(Long shopOrderId,Long orderDispatchRelationId,Long receiveShopId,
                                                             String receiveShopName,Integer currentStatus,Integer newStatus) {
        try {

            vegaOrderManager.shopOrderStatusChangedForReject(shopOrderId,receiveShopId,receiveShopName, newStatus ,
                                                             currentStatus,orderDispatchRelationId);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to change shopOrder(id={})'s status from {} to {}, cause:{}", shopOrderId,
                    currentStatus, newStatus, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.status.update.fail");
        }
    }


    /**
     * 店铺订单级别的状态发生改变, 一般会引起其下子订单的状态随之发生改变,调用方负责判断状态变迁的合法性
     *
     * @param shopOrderId   店铺订单id
     * @param currentStatus 当前状态 只有店铺订单的当前状态是currentStatus才会做状态更新
     * @param newStatus     新的目标状态
     * @return 状态是否改变成功
     */
    @Override
    public Response<Boolean> shopOrderStatusChanged(Long shopOrderId, Integer currentStatus, Integer newStatus) {
        try {
            List<SkuOrder> skuOrders = skuOrderDao.findByOrderId(shopOrderId);
            if (CollectionUtils.isEmpty(skuOrders)) {
                log.error("no skuOrders found for ShopOrder(id={})", shopOrderId);
                return Response.fail("skuOrder.not.found");
            }
            orderManager.shopOrderStatusChanged(shopOrderId, currentStatus, newStatus);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to change shopOrder(id={})'s status from {} to {}, cause:{}", shopOrderId,
                    currentStatus, newStatus, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.status.update.fail");
        }
    }

    @Override
    public Response<List<Long>> batchShopOrderStatusChanged(List<Long> shopOrderIds, Integer currentStatus, Integer newStatus) {
        if (CollectionUtils.isEmpty(shopOrderIds)) {
            return Response.ok(Collections.emptyList());
        }

        List<Long> succeedIds = Lists.newArrayListWithCapacity(shopOrderIds.size());
        for (Long shopOrderId : shopOrderIds) {
            Response r = shopOrderStatusChanged(shopOrderId, currentStatus, newStatus);
            if (r.isSuccess()) {
                succeedIds.add(shopOrderId);
            } else {
                log.error("failed to update shopOrder(id={}) status to {}, error code:{}", shopOrderId, newStatus, r.getError());
            }
        }

        if (CollectionUtils.isEmpty(succeedIds)) {
            return Response.fail("shop.order.status.update.fail");
        } else if (Objects.equals(succeedIds.size(), shopOrderIds.size())) {
            return Response.ok(succeedIds);
        } else {
            log.warn("batch update shop order status partially failed");
            return Response.ok(succeedIds);
        }
    }

    @Override
    public Response<Boolean> updateOrderPayType(Long orderId, Integer payType) {
        try {
            vegaOrderManager.updateOrderPayType(orderId,payType);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("update shop and sku order pay type (id={}) payType: {} fail,cause: {}",orderId,payType,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.order.fail");
        }
    }

    @Override
    public Response<Boolean> updateSkuOrderTagsJsonById(Long skuOrderId, Map<String,String> tags) {
        try {
            return Response.ok(skuOrderDao.updateTags(skuOrderId,tags));
        }catch (Exception e){
            log.error("update sku order (id={}) tags: {} fail,cause: {}",skuOrderId, tags,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("update.sku.order.fail");
        }
    }

}
