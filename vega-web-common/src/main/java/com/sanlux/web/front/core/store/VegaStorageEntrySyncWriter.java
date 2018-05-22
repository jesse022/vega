/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.store;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.StorageConstants;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.events.trade.VegaEntryGodownFinishedEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.store.enums.EntryGodownStatus;
import io.terminus.parana.store.model.EntryGodown;
import io.terminus.parana.store.service.EntryGodownReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 入库单同步状态
 *
 * @author : panxin
 */
@Slf4j
@Component
public class VegaStorageEntrySyncWriter {

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private EntryGodownReadService entryGodownReadService;
    @Autowired
    private EventBus eventBus;

    public Response<Boolean> syncFirstDealerOrder(VegaOrderStatus orderStatus) {
        synAndUpdateOrderExtra(orderStatus);
        return Response.ok(Boolean.TRUE);
    }

    public Response<Boolean> syncBuyerOrder(VegaOrderStatus orderStatus) {
        synAndUpdateOrderExtra(orderStatus);
        return Response.ok(Boolean.TRUE);
    }

    public Response<Boolean> syncDealerOrder(VegaOrderStatus orderStatus) {
        synAndUpdateOrderExtra(orderStatus);
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 更新订单状态
     * @param orderStatus 订单
     */
    private void synAndUpdateOrderExtra(VegaOrderStatus orderStatus) {
        Integer pageNo = 1;
        Integer pageSize = 100;

        // 已入库的订单IDs
        List<ShopOrder> alreadyEntryOrders = checkAlreadyEntryOrders(pageNo, pageSize, orderStatus);

        if (!alreadyEntryOrders.isEmpty()) {
            // 更新已入库的入库单ID对应的订单状态
            alreadyEntryOrders.stream().forEach(shopOrder -> {
                Map<String, String> extra = MoreObjects.firstNonNull(shopOrder.getExtra(), Maps.newHashMap());
                extra.put(StorageConstants.ORDER_STORAGE_ENTRY_STATUS, String.valueOf(EntryGodownStatus.FINISH.getValue()));
                shopOrder.setExtra(extra);
            });
        }
        eventBus.post(new VegaEntryGodownFinishedEvent(alreadyEntryOrders));
    }

    private List<ShopOrder> checkAlreadyEntryOrders(Integer pageNo, Integer pageSize, VegaOrderStatus orderStatus) {
        Map<String, Object> criteria = Maps.newHashMap();
        List<Integer> status = Lists.newArrayList();
        status.add(orderStatus.getValue());
        criteria.put("status", status);

        List<ShopOrder> alreadyEntryOrders = Lists.newArrayList();

        while (true) {
            // 待入库订单
            Response<Paging<ShopOrder>> resp = vegaOrderReadService.pagingShopOrder(pageNo, pageSize, criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging order that waiting for storage out. criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<ShopOrder> paging = resp.getResult();
            List<ShopOrder> orders = paging.getData();
            Long total = paging.getTotal();

            // 待入库的订单对应的入库单IDs
            Map<Long, ShopOrder> waitEntryOrderStorageEntries = Maps.newHashMap();

            // 查找每个待入库订单的入库单ID
            orders.stream().forEach(shopOrder -> {
                if (shopOrder.getExtra() == null || shopOrder.getExtra().isEmpty()) {
                    log.debug("shopOrder = {}, extra is null or empty.", shopOrder);
                }else {
                    String id = shopOrder.getExtra().get(StorageConstants.ORDER_STORAGE_ENTRY_ID);

                    if (Strings.isNullOrEmpty(id)) {
                        log.debug("shopOrder.extra.entryGodownId is null, shopOrder = {}", shopOrder.toString());
                    }else {
                        log.info("orderId = {}, entryGodownId = {}", shopOrder.getId(), id);

                        Long storeId = Long.valueOf(id);
                        waitEntryOrderStorageEntries.put(storeId, shopOrder);
                    }
                }
            });

            // 待入库的入库单IDs
            List<Long> waitOutStoreIds = findWaitStoreIds(waitEntryOrderStorageEntries);
            if (waitOutStoreIds.isEmpty()) {
                log.info("find wait out storeIds is empty");
                if (pageNo * pageSize > total) {
                    log.debug("complete read orders that wait storage out, orderCount = {}", total);
                    break;
                }
                pageNo++;
                continue;
            }

            // 已入库的入库单IDs
            List<Long> alreadyEntryStoreIds = checkAlreadyEntryStores(waitOutStoreIds);

            // 已入库入库单对应的订单IDs
            alreadyEntryStoreIds.stream().forEach(storeId -> {
                alreadyEntryOrders.add(waitEntryOrderStorageEntries.get(storeId));
            });

            if (pageNo * pageSize > total) {
                log.debug("complete read orders that wait storage out, orderCount = {}", total);
                break;
            }
            pageNo++;
        }
        return alreadyEntryOrders;
    }

    /**
     * 获取所有待入库的订单对应的入库单IDs
     * @param waitEntryOrderStorageEntries 待入库的入库单IDs对应的订单IDs
     * @return 待入库的入库单IDs
     */
    private List<Long> findWaitStoreIds(Map<Long, ShopOrder> waitEntryOrderStorageEntries) {
        List<Long> waitOutStoreIds = Lists.newArrayList();
        waitEntryOrderStorageEntries.entrySet().forEach(entry -> {
            waitOutStoreIds.add(entry.getKey());
        });
        return waitOutStoreIds;
    }

    /**
     * 根据待入库的入库单IDs查询已入库的入库单IDs
     * @param waitEntryStoreIds 待入库的入库单IDs
     * @return 已入库的入库单IDs
     */
    private List<Long> checkAlreadyEntryStores(List<Long> waitEntryStoreIds) {
        List<Long> alreadyEntryStoreIds = Lists.newArrayList();
        Response<List<EntryGodown>> resp = entryGodownReadService.findByIdsAndStatus(waitEntryStoreIds, EntryGodownStatus.FINISH.getValue());
        if (!resp.isSuccess()) {
            log.error("failed to find EntryGodown by Ids = [{}], status = {}, cause : {}",
                    waitEntryStoreIds, EntryGodownStatus.FINISH.getValue(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        // 所有已入库的出库单IDs
        List<EntryGodown> finished = resp.getResult();
        alreadyEntryStoreIds.addAll(finished.stream().map(EntryGodown::getId).collect(Collectors.toList()));

        return alreadyEntryStoreIds;
    }

}
