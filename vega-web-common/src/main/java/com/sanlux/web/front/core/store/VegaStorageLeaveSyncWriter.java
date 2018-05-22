/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.store;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.StorageConstants;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.events.trade.VegaLeaveGodownFinishedEvent;
import com.sanlux.web.front.core.trade.VegaOrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.store.enums.LeaveGodownStatus;
import io.terminus.parana.store.model.LeaveGodown;
import io.terminus.parana.store.service.LeaveGodownReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 出库单同步状态
 *
 * @author : panxin
 */
@Slf4j
@Component
public class VegaStorageLeaveSyncWriter {


    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private VegaOrderReadLogic orderReadLogic;
    @RpcConsumer
    private LeaveGodownReadService leaveGodownReadService;
    @Autowired
    private EventBus eventBus;
    
    public Response<Boolean> syncBuyerOrder(VegaOrderStatus orderStatus) {
        updateOrderExtra(orderStatus);
        return Response.ok(Boolean.TRUE);
    }

    public Response<Boolean> syncDealerOrder(VegaOrderStatus orderStatus) {
        updateOrderExtra(orderStatus);
        return Response.ok(Boolean.TRUE);
    }

    public Response<Boolean> manualSync(Long orderId) {
        // 订单信息
        ShopOrder orderBase = (ShopOrder) orderReadLogic.findOrder(orderId, OrderLevel.SHOP);
        Map<Long, ShopOrder> waitOutOrderStorageEntries = Maps.newHashMap();

        if (!ImmutableList.of(
                VegaOrderStatus.FIRST_DEALER_OUT_WAITE_OVER.getValue(),
                VegaOrderStatus.FIRST_DEALER_OUT_WAIT_OVER_PLATFORM.getValue()).contains(orderBase.getStatus())) {
            log.warn("failed to sync order storage status, cause this order's status can not sync");
            return Response.fail("order.status.can.not.sync");
        }

        // 订单对应的出库单信息
        Long storeId = Long.valueOf(orderBase.getExtra().get(StorageConstants.ORDER_STORAGE_LEAVE_ID));
        waitOutOrderStorageEntries.put(storeId, orderBase);

        // 出库单ID
        List<Long> alreadyLeaveOrders = checkAlreadyLeaveStores(Collections.singletonList(storeId));
        if (alreadyLeaveOrders.isEmpty()) {
            log.warn("failed to sync order storage status, cause this order's storage is not out yet.");
            return Response.fail("order.storage.not.out.yet");
        }
        doUpdate(Collections.singletonList(orderBase));

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 更新订单
     * @param orderStatus 订单状态
     */
    private void updateOrderExtra(VegaOrderStatus orderStatus) {
        Integer pageNo = 1;
        Integer pageSize = 100;

        // 已出库的订单IDs
        List<ShopOrder> alreadyLeaveOrders = checkAlreadyLeaveOrders(pageNo, pageSize, orderStatus);
        doUpdate(alreadyLeaveOrders);
    }

    private void doUpdate(List<ShopOrder> alreadyLeaveOrders) {
        if (!alreadyLeaveOrders.isEmpty()) {
            alreadyLeaveOrders.stream().forEach(shopOrder -> {
                Map<String, String> extra = MoreObjects.firstNonNull(shopOrder.getExtra(), Maps.newHashMap());
                extra.put(StorageConstants.ORDER_STORAGE_ENTRY_STATUS, String.valueOf(LeaveGodownStatus.FINISH.getValue()));
                shopOrder.setExtra(extra);
            });
        }
        eventBus.post(new VegaLeaveGodownFinishedEvent(alreadyLeaveOrders));
    }

    private List<ShopOrder> checkAlreadyLeaveOrders(Integer pageNo, Integer pageSize, VegaOrderStatus orderStatus) {
        Map<String, Object> criteria = Maps.newHashMap();
        List<Integer> status = Lists.newArrayList();
        status.add(orderStatus.getValue());
        criteria.put("status", status);

        List<ShopOrder> alreadyLeaveOrders = Lists.newArrayList();

        while (true) {
            // 待出库订单
            Response<Paging<ShopOrder>> resp = vegaOrderReadService.pagingShopOrder(pageNo, pageSize, criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging order that waiting for storage out. criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<ShopOrder> paging = resp.getResult();
            List<ShopOrder> orders = paging.getData();
            Long total = paging.getTotal();

            // 待出库的订单对应的出库单IDs
            Map<Long, ShopOrder> waitOutOrderStorageEntries = Maps.newHashMap();

            // 查找每个待出库订单的出库单ID
            orders.stream().forEach(shopOrder -> {
                // Long orderId = shopOrder.getId();
                Long storeId = Long.valueOf(shopOrder.getExtra().get(StorageConstants.ORDER_STORAGE_LEAVE_ID));
                waitOutOrderStorageEntries.put(storeId, shopOrder);
            });

            // 待出库的出库单IDs
            List<Long> waitOutStoreIds = findWaitStoreIds(waitOutOrderStorageEntries);
            if (waitOutStoreIds.isEmpty()) {
                log.info("find storeIds is empty, no wait out store.");
                if (pageNo * pageSize > total) {
                    log.debug("complete read orders that wait storage out, orderCount = {}", total);
                    break;
                }
                pageNo++;
                continue;
            }

            // 已出库的出库单IDs
            List<Long> alreadyLeaveStoreIds = checkAlreadyLeaveStores(waitOutStoreIds);

            // 已出库出库单对应的订单IDs
            alreadyLeaveStoreIds.stream().forEach(storeId -> {
                alreadyLeaveOrders.add(waitOutOrderStorageEntries.get(storeId));
            });

            if (pageNo * pageSize > total) {
                log.debug("complete read orders that wait storage out, orderCount = {}", total);
                break;
            }
            pageNo++;
        }
        return alreadyLeaveOrders;
    }

    /**
     * 获取所有待出库的订单对应的出库单IDs
     * @param waitOutOrderStorageEntries 待出库的出库单IDs对应的订单IDs
     * @return 待出库的出库单IDs
     */
    private List<Long> findWaitStoreIds(Map<Long, ShopOrder> waitOutOrderStorageEntries) {
        List<Long> waitOutStoreIds = Lists.newArrayList();
        waitOutOrderStorageEntries.entrySet().forEach(entry -> {
            waitOutStoreIds.add(entry.getKey());
        });
        return waitOutStoreIds;
    }

    /**
     * 根据待出库的出库单IDs查询已出库的出库单IDs
     * @param waitOutStoreIds 待出库的出库单IDs
     * @return 已出库的出库单IDs
     */
    private List<Long> checkAlreadyLeaveStores(List<Long> waitOutStoreIds) {
        List<Long> alreadyLeaveStoreIds = Lists.newArrayList();
        Response<List<LeaveGodown>> resp = leaveGodownReadService.findByIdsAndStatus(waitOutStoreIds, LeaveGodownStatus.FINISH.getValue());
        if (!resp.isSuccess()) {
            log.error("failed to find leaveGoDown by IDs = [{}], cause: {}", waitOutStoreIds, resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        // 所有已出库的出库单IDs
        List<LeaveGodown> lefts = resp.getResult();
        alreadyLeaveStoreIds.addAll(lefts.stream().map(LeaveGodown::getId).collect(Collectors.toList()));

        return alreadyLeaveStoreIds;
    }

}
