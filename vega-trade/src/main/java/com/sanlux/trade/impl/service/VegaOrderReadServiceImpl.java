package com.sanlux.trade.impl.service;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.dto.YouyuncaiOrderAdressDto;
import com.sanlux.trade.dto.YouyuncaiOrderConsigneeDto;
import com.sanlux.trade.dto.YouyuncaiOrderInvoiceInfoDto;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.VegaOrderChannelEnum;
import com.sanlux.trade.enums.VegaOrderPaymentStatus;
import com.sanlux.trade.impl.dao.ShopOrderExtDao;
import com.sanlux.trade.impl.dao.YouyuncaiOrderDao;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.VegaOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.DateUtil;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.*;
import io.terminus.parana.order.model.*;
import io.terminus.parana.promotion.impl.dao.PromotionDao;
import io.terminus.parana.promotion.model.Promotion;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;


/**
 * Created by liangfujie on 16/8/26
 */
@Service
@RpcProvider
@Slf4j
public class VegaOrderReadServiceImpl implements VegaOrderReadService {

    private final SkuOrderDao skuOrderDao;
    private final ShopOrderDao shopOrderDao;
    private final OrderPaymentDao orderPaymentDao;
    private final PaymentDao paymentDao;
    private final OrderShipmentDao orderShipmentDao;
    private final OrderReceiverInfoDao orderReceiverInfoDao;
    private final OrderInvoiceDao orderInvoiceDao;
    private final InvoiceDao invoiceDao;
    private final ShipmentDao shipmentDao;
    private final OrderCommentDao orderCommentDao;
    private final PromotionDao promotionDao;
    @Value("${order.auto.cancel.in.minutes: 30}")
    private Integer autoCancelInMinutes;
    @Value("${order.auto.confirm.in.minutes: 60}")
    private Integer autoConfirmInMinutes;
    private ShopOrderExtDao shopOrderExtDao;
    private YouyuncaiOrderDao youyuncaiOrderDao;



    @Autowired
    public VegaOrderReadServiceImpl(SkuOrderDao skuOrderDao, ShopOrderDao shopOrderDao, OrderPaymentDao orderPaymentDao, PaymentDao paymentDao, OrderShipmentDao orderShipmentDao, OrderReceiverInfoDao orderReceiverInfoDao, OrderInvoiceDao orderInvoiceDao, InvoiceDao invoiceDao, ShipmentDao shipmentDao, OrderCommentDao orderCommentDao, PromotionDao promotionDao, ShopOrderExtDao shopOrderExtDao,YouyuncaiOrderDao youyuncaiOrderDao) {
        this.skuOrderDao = skuOrderDao;
        this.shopOrderDao = shopOrderDao;
        this.orderPaymentDao = orderPaymentDao;
        this.paymentDao = paymentDao;
        this.orderShipmentDao = orderShipmentDao;
        this.orderReceiverInfoDao = orderReceiverInfoDao;
        this.orderInvoiceDao = orderInvoiceDao;
        this.invoiceDao = invoiceDao;
        this.shipmentDao = shipmentDao;
        this.orderCommentDao = orderCommentDao;
        this.promotionDao = promotionDao;
        this.shopOrderExtDao = shopOrderExtDao;
        this.youyuncaiOrderDao = youyuncaiOrderDao;
    }

    /**
     * 根据订单ID和分页条件筛选订单的分页信息
     *
     * @param shopOrderId 订单ID
     * @param pageNo      当前页数
     * @param pageSize    页面大小
     * @return 分页细节信息
     */
    @Override
    public Response<VegaOrderDetail> pagingVegaOrderDetailByShopId(Long shopOrderId,FlowPicker flowPicker, Integer pageNo, Integer pageSize) {

        try {
            VegaOrderDetail vegaOrderDetail = new VegaOrderDetail();
            ShopOrder shopOrder = shopOrderDao.findById(shopOrderId);
            if (Arguments.isNull(shopOrder)) {
                log.error("find shop order by id:{} fail because shop order not exist", shopOrderId);
                return Response.fail("shop.order.not.exist");
            }
            vegaOrderDetail.setShopOrder(shopOrder);
            PageInfo e = PageInfo.of(pageNo, pageSize);
            HashMap<String, Object> params = Maps.newHashMap();
            params.put("orderId", shopOrderId);
            params.put("limit", e.getLimit());
            params.put("offset", e.getOffset());
            Paging<SkuOrder> skuOrderPaging = skuOrderDao.paging(params);
            List<SkuOrder> skuOrders = skuOrderPaging.getData();
            ////返回订单信息
            setOrderInfo(vegaOrderDetail, shopOrder, skuOrders);
            //计算总优惠
            //setDiscount(vegaOrderDetail);
            //查询支付单信息
            setPaymentInfo(vegaOrderDetail, shopOrderId);
            //平台营销信息
            //setGlobalPromotion(vegaOrderDetail);
            //查询配送单信息
            setShipmentInfo(vegaOrderDetail, shopOrderId);

            if (Objects.equal(shopOrder.getChannel(), VegaOrderChannelEnum.YOU_YUN_CAI.value())) {
                // 友云采订单
                YouyuncaiOrder youyuncaiOrder = youyuncaiOrderDao.findByOrderId(shopOrderId);

                //查询发票信息
                setYouyuncaiOrderInvoiceInfo(vegaOrderDetail, youyuncaiOrder);
                //查询用户收货地址
                setYouyuncaiOrderReceiverInfo(vegaOrderDetail, youyuncaiOrder);
            } else {
                //查询发票信息
                setInvoiceInfo(vegaOrderDetail, shopOrderId);
                //查询用户收货地址
                setReceiverInfo(vegaOrderDetail, shopOrderId);
            }
            //查询评价信息
            setCommentInfo(vegaOrderDetail, shopOrderId, skuOrders);
            vegaOrderDetail.setSkuOrders(skuOrders);
            //封装订单可操作信息
            transToSkuOrderAndOperationPaging(vegaOrderDetail, flowPicker, skuOrderPaging);
            return Response.ok(vegaOrderDetail);

        } catch (Exception var5) {
            log.error("fail to find order detail info by shop order id {}, cause:{}", shopOrderId, Throwables.getStackTraceAsString(var5));
            return Response.fail("order.detail.find.fail");
        }


    }


    /**
     * 根据订单ID获取订单详情信息
     * Created by lujm on 2017/02/13
     * @param shopOrderId 订单ID
     * @return 订单详情
     */
    @Override
    public Response<VegaOrderDetail> findVegaOrderDetailByShopOrderId(Long shopOrderId) {
        try {
            VegaOrderDetail vegaOrderDetail = new VegaOrderDetail();
            ShopOrder shopOrder = shopOrderDao.findById(shopOrderId);
            if (Arguments.isNull(shopOrder)) {
                log.error("find shop order by id:{} fail because shop order not exist", shopOrderId);
                return Response.fail("shop.order.not.exist");
            }
            vegaOrderDetail.setShopOrder(shopOrder);
            List<SkuOrder> skuOrders = skuOrderDao.findByOrderId(shopOrderId);
            //订单详情
            setOrderInfo(vegaOrderDetail, shopOrder, skuOrders);
            if (Objects.equal(shopOrder.getChannel(), VegaOrderChannelEnum.YOU_YUN_CAI.value())) {
                // 友云采订单
                YouyuncaiOrder youyuncaiOrder = youyuncaiOrderDao.findByOrderId(shopOrderId);

                //查询发票信息
                setYouyuncaiOrderInvoiceInfo(vegaOrderDetail, youyuncaiOrder);
                //查询用户收货地址
                setYouyuncaiOrderReceiverInfo(vegaOrderDetail, youyuncaiOrder);
            } else {
                //查询用户收货地址
                setReceiverInfo(vegaOrderDetail, shopOrderId);
                //发票信息
                setInvoiceInfo(vegaOrderDetail, shopOrderId);
            }
            return Response.ok(vegaOrderDetail);
        } catch (Exception var5) {
            log.error("fail to find order detail info by shop order id {}, cause:{}", shopOrderId, Throwables.getStackTraceAsString(var5));
            return Response.fail("order.detail.find.fail");
        }
    }

    @Override
    public Response<List<ShopOrder>> findShopOrderByBuyerIds(Date startAt, Date endAt, List<Long> buyerIds) {
        try {
            if (buyerIds == null) {
                buyerIds = Lists.newArrayList();
            }
            Map<String, Object> map = Maps.newHashMap();
            map.put("startAt", startAt);
            map.put("endAt", endAt);
            map.put("buyerId", buyerIds);
            //只导出"已发货","已确认收货","已入库"状态的订单
            map.put("status", ImmutableList.of(
                    VegaOrderStatus.PUT_STORAGE.getValue(),
                    VegaOrderStatus.SHIPPED.getValue(),
                    VegaOrderStatus.CONFIRMED.getValue()));
            List<ShopOrder> shopOrderList = shopOrderExtDao.findByBuyerIds(map);
            return Response.ok(shopOrderList);
        } catch (Exception e) {
            log.error("failed to find shop order, startAt={}, endAt={}, criteria={}, cause:{}",
                    startAt, endAt, buyerIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }
    }

    @Override
    public Response<List<VegaOrderDetail>> findShopOrderAndReceiverInfoByOrderIds(List<Long> shopIds) {
        try {
            List<VegaOrderDetail> vegaOrderDetails = Lists.newArrayList();
            List<ShopOrder> shopOrders = shopOrderDao.findByIds(shopIds);
            if (Arguments.isNull(shopOrders)) {
                log.error("find shop order by ids:{} fail because shop order not exist", shopIds);
                return Response.fail("shop.order.not.exist");
            }
            shopOrders.stream().forEach(shopOrder -> {
                VegaOrderDetail vegaOrderDetail = new VegaOrderDetail();
                vegaOrderDetail.setShopOrder(shopOrder);
                //查询用户收货地址
                setReceiverInfo(vegaOrderDetail, shopOrder.getId());
                vegaOrderDetails.add(vegaOrderDetail);
            });

            return Response.ok(vegaOrderDetails);
        } catch (Exception var5) {
            log.error("fail to find order detail info by shop order ids {}, cause:{}", shopIds, Throwables.getStackTraceAsString(var5));
            return Response.fail("order.detail.find.fail");
        }
    }

    @Override
    public Response<List<VegaOrderDetail>> findShopOrderByShopIds(Date startAt, Date endAt, List<Long> shopIds) {
        try {
            Map<String, Object> map = Maps.newHashMap();
            map.put("startAt", startAt);
            map.put("endAt", endAt);
            if (!Arguments.isNullOrEmpty(shopIds)) {
                map.put("shopId", shopIds);
            }
            //剔除"未付款"及"超时关闭","买家删除","退款"等逆向状态的订单
            List<Integer> orderStatusList = Lists.newArrayList();
            for (VegaOrderStatus orderStatus : VegaOrderStatus.values()) {
                if (orderStatus.getValue() >= 0) {
                    // 不包括全部逆向流程
                    orderStatusList.add(orderStatus.getValue());
                }
            }
            orderStatusList.removeIf(id -> ImmutableList.of(
                    VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                    VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                    VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue()
            ).contains(id));
            map.put("status",orderStatusList);

            List<VegaOrderDetail> vegaOrderDetails = Lists.newArrayList();
            List<ShopOrder> shopOrderList =  shopOrderExtDao.findByBuyerIds(map);
            for (ShopOrder shopOrder : shopOrderList) {
                VegaOrderDetail vegaOrderDetail = new VegaOrderDetail();
                List<SkuOrder> skuOrders = skuOrderDao.findByOrderId(shopOrder.getId());
                vegaOrderDetail.setShopOrder(shopOrder);
                vegaOrderDetail.setSkuOrders(skuOrders);
                vegaOrderDetails.add(vegaOrderDetail);
            }

            return Response.ok(vegaOrderDetails);
        } catch (Exception e) {
            log.error("failed to find shop order, startAt={}, endAt={}, criteria={}, cause:{}",
                    startAt, endAt, shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }
    }

    @Override
    public Response<Paging<ShopOrder>> pagingShopOrder(Integer pageNo, Integer pageSize, Map<String, Object> criteria) {
        try {
            if (criteria == null) {
                criteria = Maps.newHashMap();
            }
            PageInfo page = new PageInfo(pageNo, pageSize);
            Paging<ShopOrder> paging = shopOrderDao.paging(page.getOffset(), page.getLimit(), criteria);
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shop order, pageNo={}, size={}, criteria={}, cause:{}",
                    pageNo, pageSize, criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }
    }

    @Override
    public Response<VegaOrderDetail.SkuOrderAndOperation> findSkuOrderDetailById(Long id,FlowPicker flowPicker) {

        SkuOrder skuOrder = this.skuOrderDao.findById(id);
        if (Arguments.isNull(skuOrder)) {
            return Response.fail("sku order find fail");
        } else {
            VegaOrderDetail.SkuOrderAndOperation skuOrderAndOperation = new VegaOrderDetail.SkuOrderAndOperation();
            skuOrderAndOperation.setSkuOrder(skuOrder);
            ShopOrder shopOrder =shopOrderDao.findById(skuOrder.getOrderId());
            Flow flow = flowPicker.pick(shopOrder, OrderLevel.SHOP);
            skuOrderAndOperation.setSkuOrderOperations(flow.availableOperations(skuOrder.getStatus()));
            return Response.ok(skuOrderAndOperation);
        }


    }

    @Override
    public Response<Paging<OrderGroup>> findBy(OrderCriteria orderCriteria) {
        try {
            //orderCriteria.transformShopOrderId(); 该方法只会根据shopOrder查询
            PageInfo pageInfo = new PageInfo(orderCriteria.getPageNo(), orderCriteria.getSize());
            return shopOrderPagination(orderCriteria, pageInfo, null, 0);
        } catch (Exception e) {
            log.error("failed to find paging orders by {}, cause:{}",
                    orderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.find.fail");
        }
    }

    @Override
    public Response<List<OrderGroup>> findByShopOrderIds(List<Long> shopOrderIds, Integer skuOrderLimit) {
        try {
            return Response.ok(getShopOrderGroupList(shopOrderIds, skuOrderLimit));
        } catch (Exception e) {
            log.error("failed to find orders by orderIds = {}, cause:{}", shopOrderIds, Throwables.getStackTraceAsString(e));
            return Response.fail("order.find.fail");
        }
    }

    @Override
    public Response<Paging<OrderGroup>> findByBuyer(OrderCriteria orderCriteria) {
        try {
            //orderCriteria.transformShopOrderId(); 该方法只会根据shopOrder查询
            PageInfo pageInfo = new PageInfo(orderCriteria.getPageNo(), orderCriteria.getSize());
            return shopOrderPagination(orderCriteria, pageInfo, null, 1);
        } catch (Exception e) {
            log.error("failed to find paging orders by {}, cause:{}",
                    orderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.find.fail");
        }
    }

    @Override
    public Response<Paging<OrderGroup>> pagingSecondShopOrder(OrderCriteria orderCriteria, List<Long> ShopIds) {
        try {
            PageInfo pageInfo = new PageInfo(orderCriteria.getPageNo(), orderCriteria.getSize());
            return shopOrderPagination(orderCriteria, pageInfo, ShopIds, 2);
        } catch (Exception e) {
            log.error("failed to find paging orders by {}, cause:{}",
                    orderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.find.fail");
        }
    }

    @Override
    public Response<Paging<OrderGroup>> pagingTodayPaymentOrder(OrderCriteria orderCriteria) {
        try {
            PageInfo pageInfo = new PageInfo(orderCriteria.getPageNo(), orderCriteria.getSize());
            return shopOrderPagination(orderCriteria, pageInfo, null, 3);
        } catch (Exception e) {
            log.error("failed to find paging orders by {}, cause:{}",
                    orderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.find.fail");
        }
    }

    @Override
    public Response<Long> countTodayPaymentOrder(OrderCriteria orderCriteria) {
        try {
            return Response.ok(shopOrderExtDao.countTodayPayment(orderCriteria.toMap()));
        } catch (Exception e) {
            log.error("failed to count orders by {}, cause:{}",
                    orderCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.count.fail");
        }
    }

    /**
     *
     * @param orderCriteria 查询条件
     * @param pageInfo 分页信息
     * @param type 标志位
     *             1:代表买家订单中心,过滤买家已删除的订单
     *             2:一级经销商查询二级经销商订单
     *             3:运营中心查看今日付款订单
     * @return 分页信息
     */
    private Response<Paging<OrderGroup>> shopOrderPagination(OrderCriteria orderCriteria, PageInfo pageInfo, List<Long> ShopIds, Integer type) {
        //处理查询时间,如果结束时间和开始时间相同,自动为结束时间+1天
        handleDate(orderCriteria);
        Paging<ShopOrder> pShopOrders;
        if (Objects.equal(type, 1)) {
            pShopOrders = shopOrderExtDao.pagingByBuyer(pageInfo.getOffset(), pageInfo.getLimit(), orderCriteria.toMap());
        } else if (Objects.equal(type, 2)) {
            pShopOrders = shopOrderExtDao.pagingSecondShopOrder(pageInfo.getOffset(), pageInfo.getLimit(), orderCriteria.toMap(), ShopIds);
        } else if (Objects.equal(type, 3)) {
            pShopOrders = shopOrderExtDao.pagingTodayPaymentOrder(pageInfo.getOffset(), pageInfo.getLimit(), orderCriteria.toMap());
        } else {
            pShopOrders = shopOrderDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), orderCriteria.toMap());
        }

        if (pShopOrders.getTotal() == 0L) {
            return Response.ok(Paging.<OrderGroup>empty());
        }
        final List<ShopOrder> shopOrders = pShopOrders.getData();
        List<Long> shopOrderIds = Lists.newArrayListWithCapacity(shopOrders.size());
        for (ShopOrder shopOrder : shopOrders) {
            shopOrderIds.add(shopOrder.getId());
        }
        List<SkuOrder> skuOrders;

        if (Arguments.isNull(orderCriteria.getSkuOrderLimit())||orderCriteria.getSkuOrderLimit() < 0) {
            skuOrders = skuOrderDao.findByOrderIds(shopOrderIds);
        } else {
            skuOrders = getSkuOrderByOrderIdsForLimit(shopOrderIds, orderCriteria.getSkuOrderLimit());
        }
        ListMultimap<Long, SkuOrder> byShopOrderId = Multimaps.index(skuOrders, new Function<SkuOrder, Long>() {
            @Override
            public Long apply(SkuOrder skuOrder) {
                return skuOrder.getOrderId();
            }
        });
        List<OrderGroup> orderGroups = makeOrderGroups(byShopOrderId, shopOrders);
        Paging<OrderGroup> p = new Paging<>(pShopOrders.getTotal(), orderGroups);
        return Response.ok(p);
    }

    private List<OrderGroup> getShopOrderGroupList(List<Long> shopOrderIds, Integer skuOrderLimit) {

        List<ShopOrder> shopOrders = shopOrderDao.findByIds(shopOrderIds);

        List<SkuOrder> skuOrders = getSkuOrderByOrderIdsForLimit(shopOrderIds, skuOrderLimit);

        ListMultimap<Long, SkuOrder> byShopOrderId = Multimaps.index(skuOrders, new Function<SkuOrder, Long>() {
            @Override
            public Long apply(SkuOrder skuOrder) {
                return skuOrder.getOrderId();
            }
        });

        return makeOrderGroups(byShopOrderId, shopOrders);
    }

    private void handleDate(OrderCriteria orderCriteria) {
        if (orderCriteria.getStartAt() != null) {
            orderCriteria.setStartAt(DateUtil.withTimeAtStartOfDay(orderCriteria.getStartAt()));
        }
        if (orderCriteria.getEndAt() != null) {
            orderCriteria.setEndAt(DateUtil.withTimeAtEndOfDay(orderCriteria.getEndAt()));
        }
    }
    /**
     * 根据店铺订单和子订单, 构建对应的订单分组
     *
     * @param byShopOrderId 按照店铺订单分组的子订单
     * @param shopOrders    店铺订单
     * @return 订单分组列表
     */
    private List<OrderGroup> makeOrderGroups(ListMultimap<Long, SkuOrder> byShopOrderId, List<ShopOrder> shopOrders) {
        List<OrderGroup> orderGroups = Lists.newArrayListWithCapacity(shopOrders.size());
        for (ShopOrder shopOrder : shopOrders) {
            OrderGroup orderGroup = new OrderGroup();
            orderGroup.setShopOrder(shopOrder);
            if (byShopOrderId.containsKey(shopOrder.getId())) {
                List<SkuOrder> skuOrders = byShopOrderId.get(shopOrder.getId());
                List<OrderGroup.SkuOrderAndOperation> skuOrderAndOperations
                        = Lists.newArrayListWithCapacity(skuOrders.size());
                for (SkuOrder skuOrder : skuOrders) {
                    OrderGroup.SkuOrderAndOperation skuOrderAndOperation = new OrderGroup.SkuOrderAndOperation();
                    skuOrderAndOperation.setSkuOrder(skuOrder);
                    skuOrderAndOperations.add(skuOrderAndOperation);
                }
                orderGroup.setSkuOrderAndOperations(skuOrderAndOperations);
            }
            orderGroups.add(orderGroup);
        }
        return orderGroups;
    }


    /**
     * 根据订单id查询对应的子订单(每个订单限制了子单返回数量)
     *
     * @param shopOrderIds 店铺订单id
     * @param limit        限制数量
     * @return 子单集合
     */
    private List<SkuOrder> getSkuOrderByOrderIdsForLimit(List<Long> shopOrderIds, Integer limit) {

        List<SkuOrder> skuOrders = Lists.newArrayList();
        Set<Long> idSet = new HashSet<Long>(shopOrderIds);
        for (Long shopOrderId : idSet) {
            skuOrders.addAll(skuOrderDao.findByOrderIdWithLimit(shopOrderId, limit));
        }
        return skuOrders;
    }


    private void transToSkuOrderAndOperationPaging(VegaOrderDetail vegaOrderDetail, FlowPicker flowPicker, Paging<SkuOrder> skuOrderPaging) {

        Flow flow = flowPicker.pick(vegaOrderDetail.getShopOrder(), OrderLevel.SHOP);
        vegaOrderDetail.setShopOrderOperations(pickCommonSkuOperation(vegaOrderDetail.getSkuOrders(), flow));

        Paging<VegaOrderDetail.SkuOrderAndOperation> paging = new Paging<>();
        paging.setTotal(skuOrderPaging.getTotal());
        List<SkuOrder> skuOrders = skuOrderPaging.getData();
        List<VegaOrderDetail.SkuOrderAndOperation> skuOrderAndOperations = Lists.newArrayListWithCapacity(skuOrders.size());

        for (SkuOrder skuOrder : skuOrders) {
            VegaOrderDetail.SkuOrderAndOperation skuOrderAndOperation = new VegaOrderDetail.SkuOrderAndOperation();
            skuOrderAndOperation.setSkuOrderOperations(flow.availableOperations(skuOrder.getStatus()));
            skuOrderAndOperation.setSkuOrder(skuOrder);
            skuOrderAndOperations.add(skuOrderAndOperation);
        }
        paging.setData(skuOrderAndOperations);
        vegaOrderDetail.setSkuOrderPaging(paging);

    }


    private void setDiscount(OrderDetail orderDetail) {
        int discount = 0;
        int discount1 = discount + MoreObjects.firstNonNull(orderDetail.getShopOrder().getDiscount(), 0);
        for (SkuOrder skuOrder : orderDetail.getSkuOrders()) {
            discount1 += MoreObjects.firstNonNull(skuOrder.getDiscount(), 0).intValue();
        }
        orderDetail.setDiscount(discount1);
    }

    private void setGlobalPromotion(OrderDetail orderDetail) {
        Payment payment = orderDetail.getPayment();
        if (payment != null && payment.getPromotionId() != null) {
            Promotion promotion = promotionDao.findById(payment.getPromotionId());
            orderDetail.setPromotion(promotion);
        }

    }

    private void setOrderInfo(OrderDetail orderDetail, ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        orderDetail.setShopOrder(shopOrder);
        if (Objects.equal(shopOrder.getStatus(), VegaOrderStatus.NOT_PAID_PLATFORM.getValue()) ||
                Objects.equal(shopOrder.getStatus(), VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue()) ||
                Objects.equal(shopOrder.getStatus(), VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue())) {
            Date autoCancelTime = (new DateTime(shopOrder.getCreatedAt())).plusMinutes(autoCancelInMinutes).toDate();
            orderDetail.setAutoCancelTime(autoCancelTime);
        }

        orderDetail.setSkuOrders(skuOrders);
    }

    private void setPaymentInfo(OrderDetail orderDetail, Long shopOrderId) {
        List<OrderPayment> orderPayments = orderPaymentDao.findByOrderIdAndOrderType(shopOrderId, OrderLevel.SHOP.getValue());
        if (!CollectionUtils.isEmpty(orderPayments)) {
            OrderPayment orderPayment = orderPayments.get(0);
            for(OrderPayment payment : orderPayments){
                if(Objects.equal(VegaOrderPaymentStatus.SUCCESS_PAYMENT.value(),payment.getStatus())){
                    orderPayment = payment;
                }
            }
            Payment payment = paymentDao.findById(orderPayment.getPaymentId());
            orderDetail.setPayment(payment);
        }

    }

    private void setShipmentInfo(OrderDetail orderDetail, Long shopOrderId) {
        List<OrderShipment> orderShipments = orderShipmentDao.findByOrderIdAndOrderType(shopOrderId, OrderLevel.SHOP.getValue());
        if (CollectionUtils.isEmpty(orderShipments)) {
            List<SkuOrder> orderShipment = skuOrderDao.findByOrderId(shopOrderId);
            if (CollectionUtils.isEmpty(orderShipment)) {
                log.error("skuOrder not found by shopOrderId {}", shopOrderId);
                throw new ServiceException("sku.order.not.found");
            }

            List<Long> autoConfirmTime = Lists.transform(orderShipment, new Function<SkuOrder, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable SkuOrder o) {
                    if (Arguments.notNull(o)) {
                        return o.getId();
                    }
                    throw new ServiceException("sku.order.not.found");
                }
            });
            orderShipments = orderShipmentDao.findByOrderIdsAndOrderType(autoConfirmTime, OrderLevel.SKU.getValue());
        }

        OrderShipment orderShipment1;
        if (CollectionUtils.isEmpty(orderShipments)) {
            orderDetail.setShipType(0);
        } else {
            orderShipment1 = orderShipments.get(0);
            if (orderShipment1.getOrderLevel() == OrderLevel.SHOP) {
                orderDetail.setShipType(1);
            } else {
                orderDetail.setShipType(2);
            }
        }

        if (Objects.equal(orderDetail.getShipType(), 1)) {
            orderShipment1 = orderShipments.get(0);
            //Date autoConfirmTime1 = (new DateTime(orderShipment1.getCreatedAt())).plusMinutes(autoConfirmInMinutes).toDate();
            //orderDetail.setAutoConfirmTime(autoConfirmTime1);
            orderDetail.setShipAt(orderShipment1.getCreatedAt());
            Shipment shipment = shipmentDao.findById(orderShipment1.getShipmentId());
            orderDetail.setConfirmAt(shipment.getConfirmAt());
        }

    }

    private void setInvoiceInfo(OrderDetail orderDetail, Long shopOrderId) {
        List<OrderInvoice> orderInvoices = orderInvoiceDao.findByOrderIdAndOrderType(shopOrderId, OrderLevel.SHOP.getValue());
        List<Invoice> invoices = invoiceDao.findByIds(Lists.transform(orderInvoices, new Function<OrderInvoice, Long>() {
            public Long apply(OrderInvoice input) {
                return input.getInvoiceId();
            }
        }));
        orderDetail.setInvoices(invoices);
    }

    /**
     * 设置友云采发票信息
     * @param orderDetail 订单详情
     * @param youyuncaiOrder 友云采订单信息
     */
    private void setYouyuncaiOrderInvoiceInfo(OrderDetail orderDetail, YouyuncaiOrder youyuncaiOrder) {
        YouyuncaiOrderInvoiceInfoDto youyuncaiOrderInvoiceInfoDto =
                JsonMapper.nonDefaultMapper().fromJson(youyuncaiOrder.getInvoiceInfoJson(), YouyuncaiOrderInvoiceInfoDto.class);

        //// TODO: 2018/3/10 友云采发票信息不完整...
        List<Invoice> invoices = Lists.newArrayList();
        Invoice invoice = new Invoice();
        Map<String, String> detailMap = Maps.newHashMap();
        detailMap.put("type",youyuncaiOrderInvoiceInfoDto.getInvoiceType().toString()); // 发票类型 普通:1 增值发票:2
//        detailMap.put("titleType",); // 1:个人发票 2:公司发票
//        detailMap.put("taxIdentityNo",);//普通发票税务登记号
        detailMap.put("companyName", youyuncaiOrderInvoiceInfoDto.getCompanyName());//增值票,公司名称
//        detailMap.put("taxRegisterNo",);//增值票,税务登记号
//        detailMap.put("registerAddress",);//增值票,注册地址
//        detailMap.put("registerPhone",);//增值票,注册电话
//        detailMap.put("registerBank",);//增值票,开户银行
//        detailMap.put("bankAccount",);//增值票,银行账号

        invoice.setDetail(detailMap); //发票信息
        invoice.setTitle(youyuncaiOrderInvoiceInfoDto.getCompanyName()); //普通票公司(个人)名称

        invoices.add(invoice);
        orderDetail.setInvoices(invoices);
    }

    private void setReceiverInfo(OrderDetail orderDetail, Long shopOrderId) {
        List<OrderReceiverInfo> orderReceiverInfos = orderReceiverInfoDao.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        orderDetail.setOrderReceiverInfos(orderReceiverInfos);
    }

    /**
     * 设置友云采订单收货地址信息
     * @param orderDetail    订单详情
     * @param youyuncaiOrder 友云采订单信息
     */
    private void setYouyuncaiOrderReceiverInfo(OrderDetail orderDetail, YouyuncaiOrder youyuncaiOrder) {
        YouyuncaiOrderConsigneeDto youyuncaiOrderConsigneeDto =
                JsonMapper.nonDefaultMapper().fromJson(youyuncaiOrder.getConsigneeJson(), YouyuncaiOrderConsigneeDto.class);
        YouyuncaiOrderAdressDto youyuncaiOrderAdressDto =
                JsonMapper.nonDefaultMapper().fromJson(youyuncaiOrder.getDeliverAddressJson(), YouyuncaiOrderAdressDto.class);

        OrderReceiverInfo orderReceiverInfo = new OrderReceiverInfo();
        ReceiverInfo receiverInfo = new ReceiverInfo();

        receiverInfo.setReceiveUserName(youyuncaiOrderConsigneeDto.getName()); //收货人姓名
        receiverInfo.setMobile(youyuncaiOrderConsigneeDto.getMobile()); //收货人手机
        receiverInfo.setProvince(youyuncaiOrderAdressDto.getArea0().getName()); //一级地址
        receiverInfo.setCity(youyuncaiOrderAdressDto.getArea1().getName()); //二级地址
        receiverInfo.setRegion(youyuncaiOrderAdressDto.getArea2().getName()); //三级地址
        receiverInfo.setStreet(youyuncaiOrderAdressDto.getArea3().getName()); //四级地址
        receiverInfo.setDetail(youyuncaiOrderAdressDto.getDetailAddress()); //地址详情
        receiverInfo.setPostcode(youyuncaiOrderConsigneeDto.getZip()); //邮编
        orderReceiverInfo.setReceiverInfo(receiverInfo);

        List<OrderReceiverInfo> orderReceiverInfos = Lists.newArrayList();
        orderReceiverInfos.add(orderReceiverInfo);

        orderDetail.setOrderReceiverInfos(orderReceiverInfos);
    }

    private void setCommentInfo(OrderDetail orderDetail, Long shopOrderId, List<SkuOrder> skuOrders) {

        for (SkuOrder skuOrder : skuOrders) {
            List orderCommentList = orderCommentDao.findByItemIdAndSkuOrderId(skuOrder.getItemId(), skuOrder.getId());
            if (!CollectionUtils.isEmpty(orderCommentList)) {
                orderDetail.setCommentAt(((OrderComment) orderCommentList.get(0)).getCreatedAt());
                break;
            }
        }

    }

    private Set<OrderOperation> pickCommonSkuOperation(List<SkuOrder> skuOrders, Flow flow) {
        ArrayListMultimap<OrderOperation, Long> groupSkuOrderIdByOperation = ArrayListMultimap.create();


        for (SkuOrder skuOrder : skuOrders) {
            Set<OrderOperation> operation = flow.availableOperations(skuOrder.getStatus());
            for (OrderOperation orderOperation : operation) {
                groupSkuOrderIdByOperation.put(orderOperation, skuOrder.getId());
            }
        }

        HashSet<OrderOperation> shopOperation1 = Sets.newHashSet();

        for (Object object : groupSkuOrderIdByOperation.keySet()) {
            OrderOperation operation1 = (OrderOperation) object;
            if (groupSkuOrderIdByOperation.get(operation1).size() == skuOrders.size()) {
                shopOperation1.add(operation1);
            }

        }

        return shopOperation1;
    }


}
