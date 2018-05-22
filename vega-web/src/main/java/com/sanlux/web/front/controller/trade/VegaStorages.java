/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.trade;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.StorageConstants;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.trade.service.VegaOrderWriteService;
import com.sanlux.web.front.core.store.VegaStorageLeaveSyncWriter;
import com.sanlux.web.front.core.trade.VegaOrderReadLogic;
import com.sanlux.web.front.core.trade.VegaOrderWriteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.store.enums.EntryGodownStatus;
import io.terminus.parana.store.enums.EntryGodownType;
import io.terminus.parana.store.enums.LeaveGodownStatus;
import io.terminus.parana.store.enums.LeaveGodownType;
import io.terminus.parana.store.enums.SonEntryGodownStatus;
import io.terminus.parana.store.model.EntryGodown;
import io.terminus.parana.store.model.LeaveGodown;
import io.terminus.parana.store.model.SonEntryGodown;
import io.terminus.parana.store.model.SonLeaveGodown;
import io.terminus.parana.store.service.EntryGodownWriteService;
import io.terminus.parana.store.service.LeaveGodownWriteService;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import io.terminus.parana.web.core.component.order.ReturnLogic;
import io.terminus.parana.web.core.order.RefundReadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Sanlux 仓储, 入库, 出库
 *
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/storage")
public class VegaStorages {

    @RpcConsumer
    private LeaveGodownWriteService leaveGodownWriteService;
    @RpcConsumer
    private EntryGodownWriteService entryGodownWriteService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private OrderReadService orderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private VegaOrderWriteLogic vegaOrderWriteLogic;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private VegaOrderReadLogic orderReadLogic;
    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private ItemReadService itemReadService;
    @Autowired
    private  ReturnLogic returnLogic;
    @Autowired
    private FlowPicker flowPicker;
    @Autowired
    private  CommonRefundLogic commonRefundLogic;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private VegaStorageLeaveSyncWriter vegaStorageLeaveSyncWriter;


    /**
     * 创建出库单
     * @param orderId 订单ID
     * @param orderType 订单类型
     * @param leaveGodownType 出库单类型
     * @return 出库单ID
     */
    @RequestMapping(value = "/leave-godown", method = RequestMethod.POST)
    public Long createLeaveStorage(@RequestParam("orderId") Long orderId,
                                   @RequestParam(value = "orderType", required = false, defaultValue = "1") Integer orderType,
                                   @RequestParam(value = "leaveGodownType") Integer leaveGodownType) {
        // 订单信息
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderDetail orderDetail = findOrderDetailByOrderId(orderId);
        ShopOrder shopOrder = orderDetail.getShopOrder();
        List<SkuOrder> skuOrders = orderDetail.getSkuOrders();

        LeaveGodownType godownType = LeaveGodownType.from(leaveGodownType);
        List<SonLeaveGodown> sonEntryGodowns = generateSonLeaveGodowns(skuOrders, godownType);
        LeaveGodown leaveGodown = createLeaveGodown(sonEntryGodowns, godownType);

        // 订单添加出库单状态和出库单信息
        Map<String, String> extra = MoreObjects.firstNonNull(shopOrder.getExtra(), Maps.newHashMap());
        extra.put(StorageConstants.ORDER_STORAGE_LEAVE_STATUS, String.valueOf(LeaveGodownStatus.WAIT.getValue()));
        extra.put(StorageConstants.ORDER_STORAGE_LEAVE_ID, String.valueOf(leaveGodown.getId()));

        updateOrderExtra(orderId, orderLevel, extra);

        // 更新订单状态
        vegaOrderWriteLogic.updateOrder(shopOrder,
                OrderLevel.fromInt(orderType), VegaOrderEvent.FIRST_DEALER_OUT);

        return leaveGodown.getId();
    }

    /**
     * 一级经销商手工同步状态(出库单完成——>待发货)
     * @param orderId 订单ID
     * @return 结果
     */
    @RequestMapping(value = "/leave-godown/sync/{orderId}", method = RequestMethod.GET)
    public Boolean syncOrderLeaveGodown(@PathVariable Long orderId) {
        Response<Boolean> resp = vegaStorageLeaveSyncWriter.manualSync(orderId);
        if (!resp.isSuccess()) {
            log.error("failed to manual sync leaveGodown, orderId = {}, cause : {}",
                    orderId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 生成入库单
     * @param orderId 订单ID
     * @param orderType 订单类型
     * @return 入库单ID
     */
    @RequestMapping(value = "/entry-godown/{orderId}", method = RequestMethod.POST)
    public Long createEntryStorage(@PathVariable("orderId") Long orderId,
                                   @RequestParam(value = "orderType", required = false, defaultValue = "1") Integer orderType,
                                   @RequestParam(value = "entryGodownType", required = false, defaultValue = "1") Integer entryGodownType) {
        // 订单信息
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        ShopOrder shopOrder = null;
        SkuOrder skuOrderPut =null;
        List<SkuOrder> skuOrders = Lists.newArrayList();

        if (orderLevel == OrderLevel.SKU) {
            // 只会得到一个子单
            skuOrderPut = findSkuOrderById(orderId);
            if(Arguments.isNull(skuOrderPut)){
                throw new JsonResponseException("sku.order.not.exist");
            }
            skuOrders.add(skuOrderPut);

            Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(skuOrderPut.getOrderId());
            if(!shopOrderRes.isSuccess()){
                log.error("find shop order by id:{} fail,error:{}",skuOrderPut.getOrderId(),shopOrderRes.getError());
                throw new JsonResponseException(shopOrderRes.getError());
            }
            shopOrder = shopOrderRes.getResult();
        }
        if (orderLevel == OrderLevel.SHOP) {
            OrderDetail orderDetail = findOrderDetailByOrderId(orderId);
            shopOrder = orderDetail.getShopOrder();
            List<SkuOrder> allSkuOrders = orderDetail.getSkuOrders();
            //这里只取和总单状态一致的子单（存在子单退货情况）
            skuOrders = Lists.newArrayList();
            for (SkuOrder skuOrder : allSkuOrders){
                if(Objects.equal(skuOrder.getStatus(),shopOrder.getStatus())){
                    skuOrders.add(skuOrder);
                }
            }
        }

        // 创建入库单 extra存入库单ID
        EntryGodownType godownType = EntryGodownType.from(entryGodownType);
        List<SonEntryGodown> sonEntryGodowns = generateSonEntryGodowns(skuOrders, godownType);
        EntryGodown entryGodown = createEntryGodown(sonEntryGodowns, godownType);

        // 订单添加入库单状态和出库单信息
        Map<String, String> extra = MoreObjects.firstNonNull(shopOrder.getExtra(), Maps.newHashMap());
        extra.put(StorageConstants.ORDER_STORAGE_ENTRY_STATUS, String.valueOf(LeaveGodownStatus.WAIT.getValue()));
        extra.put(StorageConstants.ORDER_STORAGE_ENTRY_ID, String.valueOf(entryGodown.getId()));

        updateOrderExtra(orderId, orderLevel, extra);

        // 更新订单状态
        Flow flow = flowPicker.pick(shopOrder, OrderLevel.SHOP);
        Integer targetStatus;
        if (orderLevel == OrderLevel.SHOP) {
            targetStatus = flow.target(shopOrder.getStatus(), VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation());

            Response<Boolean> response =vegaOrderWriteService.shopOrderStatusChanged(shopOrder.getId(),shopOrder.getStatus(),targetStatus);
            if (!response.isSuccess()) {
                log.error("failed to batch update shopOrders(id={}) status to {}, error code:{}",
                        shopOrder.getId(), targetStatus, response.getError());
                throw new JsonResponseException(response.getError());
            }
        }else{
            targetStatus = flow.target(skuOrderPut.getStatus(), VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation());
            Response<Boolean> response =orderWriteService.skuOrderStatusChanged(skuOrderPut.getId(),skuOrderPut.getStatus(),targetStatus);
            if (!response.isSuccess()) {
                log.error("failed to batch update shop order(id={}) status to {}, error code:{}",
                        skuOrderPut.getId(), targetStatus, response.getError());
                throw new JsonResponseException(response.getError());
            }
        }


        return entryGodown.getId();
    }

    /**
     * 生成退货入库单
     * @param refundId 退款单ID
     * @return 入库单ID
     */
    @RequestMapping(value = "/refund/entry-godown/{refundId}", method = RequestMethod.POST)
    public Long createRefundEntryStorage(@PathVariable("refundId") Long refundId) {
        Refund refund = findRefundById(refundId);
        List<OrderRefund> orderRefundList = findOrderRefundListByRefundId(refundId);

        // 目前只会关联一订单ID (skuOrder or shopOrder)
        OrderRefund orderRefund = orderRefundList.get(0);
        Integer orderType = orderRefund.getOrderType();
        Long orderId = orderRefund.getOrderId();

        ShopOrder shopOrder = null;
        SkuOrder skuOrder = null;
        Shop shop = null;
        EntryGodown entryGodown = null;
        List<SonEntryGodown> sonEntryGodowns = null;

        if (Objects.equal(OrderLevel.SHOP.getValue(), orderType)) {
            OrderDetail orderDetail = findOrderDetailByOrderId(orderId);
            shopOrder = orderDetail.getShopOrder();
            List<SkuOrder> skuOrders = orderDetail.getSkuOrders();
            sonEntryGodowns = generateSonEntryGodowns(skuOrders, EntryGodownType.REJECT);

            // 创建入库单 extra存入库单ID
            entryGodown = createEntryGodown(sonEntryGodowns, EntryGodownType.REJECT);
        }else {
            skuOrder = findSkuOrderById(orderId);
            sonEntryGodowns = generateSonEntryGodowns(Collections.singletonList(skuOrder), EntryGodownType.REJECT);

            // 创建入库单 extra存入库单ID
            entryGodown = createEntryGodown(sonEntryGodowns, EntryGodownType.REJECT);
        }

        // 订单添加入库单状态和出库单信息
        Map<String, String> extra = MoreObjects.firstNonNull(refund.getExtra(), Maps.newHashMap());
        extra.put(StorageConstants.ORDER_STORAGE_ENTRY_STATUS, String.valueOf(LeaveGodownStatus.WAIT.getValue()));
        extra.put(StorageConstants.ORDER_STORAGE_ENTRY_ID, String.valueOf(entryGodown.getId()));

        updateRefundOrderExtra(refundId, extra);

        // 更新订单状态
        /*vegaOrderWriteLogic.updateOrder(shopOrder,
                OrderLevel.fromInt(orderType), VegaOrderEvent.FIRST_DEALER_PUT_STORAGE);*/

        ParanaUser seller = UserUtil.getCurrentUser();
        returnLogic.confirmReturn(refundId, seller, VegaOrderEvent.FIRST_DEALER_PUT_STORAGE.toOrderOperation());

        return entryGodown.getId();
    }

    /**
     * 更新退款单Extra
     * @param refundId 退款单ID
     * @param extra extra
     */
    private void updateRefundOrderExtra(Long refundId, Map<String, String> extra) {
        Refund refund = new Refund();
        refund.setId(refundId);
        refund.setExtra(extra);
        Response<Boolean> resp = refundWriteService.update(refund);
        if (!resp.isSuccess()) {
            log.error("failed to update refund extra, refund = {}, cause : {}", refund, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
    }

    /**
     * SkuOrder详情
     * @param orderId 订单ID
     * @return 信息
     */
    private SkuOrder findSkuOrderById(Long orderId) {
        Response<SkuOrder> resp = skuOrderReadService.findById(orderId);
        if (!resp.isSuccess()) {
            log.error("failed to find skuOrder by Id = {}, cause : {}", orderId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 退款单对应的订单详情
     * @param refundId 退款单ID
     * @return 详情
     */
    private List<OrderRefund> findOrderRefundListByRefundId(Long refundId) {
        Response<List<OrderRefund>> resp = refundReadService.findOrderIdsByRefundId(refundId);
        if (!resp.isSuccess()) {
            log.error("failed to find orderRefund by refundId = {}, cause : {}",
                    refundId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 退款单信息
     * @param refundId 退款单ID
     * @return 信息
     */
    private Refund findRefundById(Long refundId) {
        Response<Refund> resp = refundReadService.findById(refundId);
        if (!resp.isSuccess()) {
            log.error("failed to find refund by refundId = {}, cause : {}", refundId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 更新订单Extra
     * @param orderId 订单ID
     * @param orderLevel 订单类型
     * @param extra extra
     */
    private void updateOrderExtra(Long orderId, OrderLevel orderLevel, Map<String, String> extra) {
        Response<Boolean> resp = orderWriteService.updateOrderExtra(orderId, orderLevel, extra);
        if (!resp.isSuccess()) {
            log.error("failed to update shop order extra, orderId = {}, orderType = {}, extra = {}, cause : {}",
                    orderId, orderLevel, extra, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
    }

    /**
     * 创建入库单
     * @param sonEntryGodowns 入库单
     * @return 入库单信息
     */
    private EntryGodown createEntryGodown(List<SonEntryGodown> sonEntryGodowns, EntryGodownType type) {
        EntryGodown entryGodown = new EntryGodown();
        entryGodown.setType(type.getValue());
        entryGodown.setUserId(currentUser().getId());
        entryGodown.setStatus(EntryGodownStatus.WAIT.getValue());
        entryGodown.setCreatedAt(new Date());
        entryGodown.setUpdatedAt(new Date());

        Response<EntryGodown> resp = entryGodownWriteService.create(entryGodown, sonEntryGodowns);
        if (!resp.isSuccess()) {
            log.error("failed to create EntryGodown = {}, sonEntryGodowns = {}, cause : {}",
                    entryGodown, sonEntryGodowns, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 创建出库单
     * @param sonLeaveGodowns 出库单
     * @return 出库单
     */
    private LeaveGodown createLeaveGodown(List<SonLeaveGodown> sonLeaveGodowns, LeaveGodownType type) {
        LeaveGodown leaveGodown = new LeaveGodown();
        leaveGodown.setType(type.getValue());
        leaveGodown.setStatus(LeaveGodownStatus.WAIT.getValue());
        leaveGodown.setUserId(currentUser().getId());
        leaveGodown.setCreatedAt(new Date());
        leaveGodown.setUpdatedAt(new Date());

        Response<LeaveGodown> resp = leaveGodownWriteService.create(leaveGodown, sonLeaveGodowns);
        if (!resp.isSuccess()) {
            log.error("failed to create LeaveGodown = {}, sonLeaveGodown = {}, cause : {}",
                    leaveGodown, sonLeaveGodowns, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 生成入库子单
     * @param skuOrders 订单SKUs
     * @param entryGodownType 入库单类型
     * @return 入库子单s
     */
    private List<SonEntryGodown> generateSonEntryGodowns(List<SkuOrder> skuOrders, EntryGodownType entryGodownType) {
        List<SonEntryGodown> sonEntryGodowns = Lists.newArrayList();
        SonEntryGodown seg = null;
        for (SkuOrder order : skuOrders) {
            seg = new SonEntryGodown();
            seg.setSkuId(order.getSkuId());
            seg.setItemQuantity(order.getQuantity());
            seg.setItemName(findItemById(findSkuById(order.getSkuId()).getItemId()).getName());
            seg.setItemImage(findItemById(findSkuById(order.getSkuId()).getItemId()).getMainImage());
            seg.setType(entryGodownType.getValue());
            seg.setStatus(SonEntryGodownStatus.WAIT.getValue());
            seg.setCreatedAt(new Date());
            seg.setUpdatedAt(new Date());
            seg.setStoragedNumber(0);
            seg.setUserId(currentUser().getId());
            seg.setPrice(Integer.valueOf(order.getTags().get(SystemConstant.ORDER_SKU_PRICE)));

            sonEntryGodowns.add(seg);
        }
        return sonEntryGodowns;
    }

    /**
     * 生成出库子单
     * @param skuOrders 订单SKUs
     * @param leaveGodownType 出库单类型
     * @return 出库子单s
     */
    private List<SonLeaveGodown> generateSonLeaveGodowns(List<SkuOrder> skuOrders, LeaveGodownType leaveGodownType) {
        List<SonLeaveGodown> sonLeaveGodowns = Lists.newArrayList();
        SonLeaveGodown slg = null;
        for (SkuOrder order : skuOrders) {
            slg = new SonLeaveGodown();
            slg.setSkuId(order.getSkuId());
            slg.setItemQuantity(order.getQuantity());
            slg.setItemName(findItemById(findSkuById(order.getSkuId()).getItemId()).getName());
            slg.setItemImage(findItemById(findSkuById(order.getSkuId()).getItemId()).getMainImage());
            slg.setType(leaveGodownType.getValue());
            slg.setStatus(SonEntryGodownStatus.WAIT.getValue());
            slg.setCreatedAt(new Date());
            slg.setUpdatedAt(new Date());
            slg.setLeavedNumber(0);
            slg.setUserId(currentUser().getId());

            sonLeaveGodowns.add(slg);
        }
        return sonLeaveGodowns;
    }

    /**
     * 查找订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    private OrderDetail findOrderDetailByOrderId(Long orderId) {
        Response<OrderDetail> resp = orderReadService.findOrderDetailById(orderId);
        if (!resp.isSuccess()) {
            log.error("failed to find order detail by orderId = {}, cause : {}", orderId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }


    /**
     * 查询sku信息
     * @param skuId skuID
     * @return 信息
     */
    private Sku findSkuById(Long skuId) {
        Response<Sku> resp = skuReadService.findSkuById(skuId);
        if (!resp.isSuccess()) {
            log.error("failed to find sku by skuId = {}, cause : {}", skuId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 查询sku信息
     * @param itemId itemId
     * @return 信息
     */
    private Item findItemById(Long itemId) {
        Response<Item> resp = itemReadService.findById(itemId);
        if (!resp.isSuccess()) {
            log.error("failed to find item by itemId = {}, cause : {}", itemId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 当前登录用户
     * @return 用户
     */
    private ParanaUser currentUser() {
        return UserUtil.getCurrentUser();
    }

}
