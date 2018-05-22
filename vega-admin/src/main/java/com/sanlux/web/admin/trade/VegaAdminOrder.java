package com.sanlux.web.admin.trade;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaNoteType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.dto.OrderDispatchCriteria;
import com.sanlux.trade.dto.OrderDispatchDetail;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.dto.YouyuncaiOrderCriteria;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.enums.VegaPayType;
import com.sanlux.trade.model.OrderDispatchRelation;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.*;
import com.sanlux.user.service.YouyuncaiUserReadService;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.settlement.event.VegaOrderAcceptEvent;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import com.sanlux.web.front.core.trade.VegaOrderReadLogic;
import com.sanlux.web.front.core.trade.VegaOrderWriteLogic;
import com.sanlux.web.front.core.trade.service.VegaOrderReaderComponent;
import com.sanlux.web.front.core.youyuncai.order.constants.YouyuncaiConstants;
import com.sanlux.web.front.core.youyuncai.request.VegaYouyuncaiComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.session.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by liangfujie on 16/8/23
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/vega/order")
public class VegaAdminOrder {

    @Autowired
    private VegaOrderReadLogic orderReadLogic;
    @Autowired
    private FlowPicker flowPicker;
    @RpcConsumer
    private OrderDispatchRelationReadService orderDispatchRelationReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private OrderDispatchRelationWriteService orderDispatchRelationWriteService;
    @Autowired
    private VegaOrderWriteLogic vegaOrderWriteLogic;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private OrderReadService orderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ObjectMapper objectMapper;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    @Autowired
    private VegaOrderComponent vegaOrderComponent;

    @RpcConsumer
    private PaymentReadService paymentReadService;
    @Autowired
    private VegaOrderReaderComponent vegaOrderReaderComponent;
    @Autowired
    private VegaYouyuncaiComponent vegaYouyuncaiComponent;

    @RpcConsumer
    private YouyuncaiOrderReadService youyuncaiOrderReadService;

    @RpcConsumer
    private YouyuncaiUserReadService youyuncaiUserReadService;


    @RequestMapping(value = "/paging-dispatch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderDispatchDetail> pagingDispatch(OrderDispatchCriteria orderDispatchCriteria) {
        if (orderDispatchCriteria.getStartAt() != null) {
            orderDispatchCriteria.setStartAt(handleDate(orderDispatchCriteria.getStartAt()));
        }
        if (orderDispatchCriteria.getEndAt() != null) {
            orderDispatchCriteria.setEndAt((new DateTime(handleDate(orderDispatchCriteria.getEndAt()))).plusDays(1).toDate());

        }
        Long userId = UserUtil.getUserId();
        Response<Shop> response = shopReadService.findByUserId(userId);
        if (!response.isSuccess()) {
            log.error("shop find by userId fail ,userId {}, cause {}", userId, response.getError());
            return Paging.empty();
        } else {
            Long shopId = response.getResult().getId();
            orderDispatchCriteria.setReceiveShopId(shopId);
            //判断筛选条件是否有派单商家的名称,如果有处理塞入ID

            if (!StringUtil.isEmpty(orderDispatchCriteria.getDispatchShopName())) {
                Response<Shop> resp = shopReadService.findByName(orderDispatchCriteria.getDispatchShopName());
                if (!resp.isSuccess()) {
                    log.error("dispatch shop find by name fail ,shopName {}, cause {}", orderDispatchCriteria.getDispatchShopName(), resp.getError());
                    return Paging.empty();
                }
                Shop dispatchShop = resp.getResult();
                orderDispatchCriteria.setDispatchShopId(dispatchShop.getId());

            }
            Response<Paging<OrderDispatchRelation>> resp = orderDispatchRelationReadService.paging(orderDispatchCriteria);
            if (!resp.isSuccess()) {
                log.error("OrderDispatchRelation paging fail ,shopId {}, cause {}", shopId, resp.getError());
                throw new JsonResponseException("order.dispatch.relation.paging.fail");
            } else {
                Paging<OrderDispatchRelation> paging = resp.getResult();
                List<OrderDispatchRelation> lists = paging.getData();
                List<OrderDispatchDetail> list = Lists.newArrayList();
                for (OrderDispatchRelation orderDispatchRelation : lists) {
                    OrderDispatchDetail orderDispatchDetail = new OrderDispatchDetail();
                    orderDispatchDetail.setCreatedAt(orderDispatchRelation.getCreatedAt());//塞入派单时间
                    Map<String, String> orderCriteria = Maps.newHashMap();
                    orderCriteria.put("id", String.valueOf(orderDispatchRelation.getOrderId()));
                    Response<Paging<OrderGroup>> res = orderReadLogic.pagingOrder(orderCriteria);
                    if (!res.isSuccess()) {
                        log.error("OrderGroup paging fail ,id{}, cause {}", orderDispatchRelation.getOrderId(), res.getError());
                        throw new JsonResponseException("order.paging.fail");

                    }
                    Paging<OrderGroup> orderGroupPaging = res.getResult();
                    List<OrderGroup> orderGroupList = orderGroupPaging.getData();
                    if (!orderGroupList.isEmpty()) {
                        orderDispatchDetail.setOrderGroup(orderGroupList.get(0));

                    }
                    list.add(orderDispatchDetail);

                }
                return new Paging<OrderDispatchDetail>(paging.getTotal(), list);
            }
        }


    }


    //供运营查看自己待审核派送的订单
    @RequestMapping(value = "/paging-wait-dispatch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderGroup> pagingWaitDispatch(@RequestParam Map<String, String> orderCriteria) {

        Long userId = UserUtil.getUserId();
        Response<Shop> response = shopReadService.findByUserId(userId);
        if (!response.isSuccess()) {
            log.error("shop find by userId fail ,userId {}, cause {}", userId, response.getError());
            throw new JsonResponseException("shop.find.fail");
        } else {
            Long shopId = response.getResult().getId();
            orderCriteria.put("shopId", String.valueOf(shopId));
            orderCriteria.put("statusStr",String.valueOf(VegaOrderStatus.PAID_WAIT_CHECK.getValue()));
            OrderCriteria criteria = objectMapper.convertValue(orderCriteria, OrderCriteria.class);
            if (criteria != null && !Strings.isNullOrEmpty(criteria.getMobile())) {
                Response<User> userR = userReadService.findBy(criteria.getMobile(), LoginType.MOBILE);
                if (!userR.isSuccess()) {
                    log.error("fail to find user by mobile {}, error code:{}",
                            criteria.getMobile(), userR.getError());
                    return Paging.empty();
                } else {
                    User user = userR.getResult();
                    criteria.setBuyerId(user.getId());
                }
            }

            if (criteria != null && !Strings.isNullOrEmpty(criteria.getShopName())) {
                Response<Shop> shopR = shopReadService.findByName(criteria.getShopName());
                if (!shopR.isSuccess()) {
                    log.error("fail to find shop by name {}, error code:{}",
                            criteria.getShopName(), shopR.getError());
                    return Paging.empty();
                } else {
                    Shop shop = shopR.getResult();
                    criteria.setShopId(shop.getId());
                }
            }

            Response<Paging<OrderGroup>> ordersR = vegaOrderReadService.findBy(criteria);
            if (!ordersR.isSuccess()) {
                //直接返回交给herd处理
                throw new JsonResponseException("order.find.fail");

            }
            log.info("paging order result is :{}",ordersR.getResult().getData());
            Paging<OrderGroup> orderGroupPaging = ordersR.getResult();

            Multimap<Long, SkuOrder> byShopOrderId = ArrayListMultimap.create();
            vegaOrderReaderComponent.groupSkuOrderByShopOrderId(byShopOrderId, orderGroupPaging.getData());

            for (OrderGroup orderGroup : orderGroupPaging.getData()) {
                try {
                    //// TODO: 16/6/14 暂时只有在线支付一个流程
                    Flow flow = flowPicker.pick(orderGroup.getShopOrder(), OrderLevel.SHOP);
                    List<Long> skuIds = Lists.transform(orderGroup.getSkuOrderAndOperations(), new Function<OrderGroup.SkuOrderAndOperation, Long>() {
                        @Nullable
                        @Override
                        public Long apply(OrderGroup.SkuOrderAndOperation input) {
                            return input.getSkuOrder().getSkuId();
                        }
                    });

                    Response<List<Sku>> skuRes = skuReadService.findSkusByIds(skuIds);
                    if(!skuRes.isSuccess()){
                        log.error("fail to find sku  by ids {} for order paging error:{}",
                                skuIds, skuRes.getError());
                        continue;
                    }

                    ImmutableMap<Long,Sku> skuIdAndSkuMap = Maps.uniqueIndex(skuRes.getResult(), new Function<Sku, Long>() {
                        @Nullable
                        @Override
                        public Long apply(@Nullable Sku input) {
                            if(Arguments.isNull(input)){
                                return 0L;
                            }
                            return input.getId();
                        }
                    });

                    for (OrderGroup.SkuOrderAndOperation skuOrderAndOperation : orderGroup.getSkuOrderAndOperations()) {
                        SkuOrder skuOrder = skuOrderAndOperation.getSkuOrder();
                        skuOrderAndOperation.setSku(skuIdAndSkuMap.get(skuOrder.getSkuId()));
                        skuOrderAndOperation.setSkuOrderOperations(flow.availableOperations(skuOrder.getStatus()));
                    }
                    //确定店铺订单可以执行的操作
                    //如果是根据状态筛选,那归组出来的子订单可能不能构成一个总单,这个时候就要以数据库真实数据为准
                    //如果不根据状态筛选, 由于订单列表查询的时候只会返回有限数量的子订单,所以也要重新找一把
                    orderGroup.setShopOrderOperations(
                            pickCommonSkuOperation(byShopOrderId.get(orderGroup.getShopOrder().getId()), flow));

                } catch (Exception e) {
                    log.error("fail to find order operations by orderGroup {}, cause:{}, skip it",
                            orderGroup, Throwables.getStackTraceAsString(e));
                }
            }
            return orderGroupPaging;

        }

    }


    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderGroup> paging(@RequestParam Map<String, String> orderCriteria) {

        Long userId = UserUtil.getUserId();
        Response<Shop> response = shopReadService.findByUserId(userId);
        if (!response.isSuccess()) {
            log.error("shop find by userId fail ,userId {}, cause {}", userId, response.getError());
            throw new JsonResponseException("shop.find.fail");
        } else {
            Long shopId = response.getResult().getId();
            orderCriteria.put("shopId", String.valueOf(shopId));
            Response<Paging<OrderGroup>> resp = orderReadLogic.pagingOrder(orderCriteria);
            if (!resp.isSuccess()) {
                log.error("order not dispatch paging fail ,shopId {}, cause {}", shopId, resp.getError());
                throw new JsonResponseException("order.not.dispatch.relation.paging.fail");
            } else {
                return resp.getResult();
            }

        }

    }

    /**
     * 友云采订单查询
     * @param orderCriteria 订单查询条件
     * orderId       集乘网订单Id
     * userId        买家用户Id
     * orderCode     友云采订单Id
     * invoiceState  开票状态    0为不开票，1为随货开票，2为集中开票
     * hasInvoiced   是否已开票  0:未开 1：部分已开 2：全部已开
     * startAt       开始时间
     * endAt         截止时间
     * youyuncaiNameType  按名称查询类型 1:企业 2:机构
     * companyName   企业或机构名称
     * pageNo        分页页数
     * pageSize      分页每页显示条数
     * @return 分页结果
     */
    @RequestMapping(value = "/paging-yc", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderGroup> pagingByYouyuncaiOrder(@RequestParam Map<String, Object> orderCriteria) {

        YouyuncaiOrderCriteria youyuncaiOrderCriteria = objectMapper.convertValue(orderCriteria, YouyuncaiOrderCriteria.class);

        youyuncaiOrderCriteria.setPageNo(Arguments.isNull(orderCriteria.get("pageNo")) || Arguments.isEmpty(orderCriteria.get("pageNo").toString())
                ? 1 : Integer.parseInt(orderCriteria.get("pageNo").toString()));
        youyuncaiOrderCriteria.setPageSize(Arguments.isNull(orderCriteria.get("pageSize")) || Arguments.isEmpty(orderCriteria.get("pageSize").toString())
                ? 20 : Integer.parseInt(orderCriteria.get("pageSize").toString()));

        if (!Strings.isNullOrEmpty(youyuncaiOrderCriteria.getCompanyName())) {
            // 公司名称不为空
            String name = youyuncaiOrderCriteria.getCompanyName();
            if (Objects.equal(youyuncaiOrderCriteria.getYouyuncaiNameType(), 1)) {
                // 企业名称
                name = YouyuncaiConstants.GROUP_NAME + "%" + name +"%" + YouyuncaiConstants.ORG_NAME;
            } else {
                // 机构名称
                name = YouyuncaiConstants.ORG_NAME + "%" + name;
            }

            Response<List<UserProfile>> userListRsp = youyuncaiUserReadService.findYouyuncaiUserByName(name);
            if (!userListRsp.isSuccess()) {
                log.error("find youyuncai user failed by user profile extraJson = {}, cause:{}", name, userListRsp.getError());
                return new Paging<>(0L, Collections.<OrderGroup>emptyList());
            }

            List<Long> userIds = Lists.transform(userListRsp.getResult(), UserProfile::getUserId);

            if (!Arguments.isNullOrEmpty(userIds)) {
                youyuncaiOrderCriteria.setUserIds(userIds);
            } else {
                return new Paging<>(0L, Collections.<OrderGroup>emptyList());
            }
        }


        Response<Paging<YouyuncaiOrder>> pagingRes = youyuncaiOrderReadService.paging(youyuncaiOrderCriteria);
        if (!pagingRes.isSuccess()) {
            log.error("you yun cai order paging fail ,orderCriteria = {}, cause {}", orderCriteria, pagingRes.getError());
            throw new JsonResponseException("paging.youyuncai.order.failed");
        }

        List<Long> shopOrderIds = Lists.transform(pagingRes.getResult().getData(), YouyuncaiOrder::getOrderId);

        if (Arguments.isNullOrEmpty(shopOrderIds)) {
            return new Paging<>(0L, Collections.<OrderGroup>emptyList());
        }

        Response<List<OrderGroup>> orderList =vegaOrderReadService.findByShopOrderIds(shopOrderIds, youyuncaiOrderCriteria.getSkuOrderLimit());
        if (!orderList.isSuccess()) {
            log.error("order list fail ,orderCriteria = {}, cause {}", orderCriteria, pagingRes.getError());
            throw new JsonResponseException("paging.order.failed");
        }

        List<OrderGroup> orderGroups = orderList.getResult();

        //按照订单ID从小到大排序
        Collections.sort(orderGroups, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                OrderGroup orderGroup0=(OrderGroup)o1;
                OrderGroup orderGroup2=(OrderGroup)o2;
                return orderGroup0.getShopOrder().getId().compareTo(orderGroup2.getShopOrder().getId());
            }
        });

        // 倒序
        Collections.reverse(orderGroups);

        return new Paging<>(pagingRes.getResult().getTotal(), orderGroups);
    }

    /**
     * 根据集乘网订单ID获取友云采订单详情
     * @param shopOrderId  集乘网订单Id
     * @return 查询结果
     */
    @RequestMapping(value = "/detail-yc", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public YouyuncaiOrder detailByYouyuncaiOrder(@RequestParam("id") Long shopOrderId) {
        Response<YouyuncaiOrder> response = this.youyuncaiOrderReadService.findByOrderId(shopOrderId);
        if (!response.isSuccess()) {
            log.error("find you yun cai order by orderId = {} failed, cause = {}", shopOrderId, response.getError());
            throw new JsonResponseException("find.youyuncai.order.fail");
        }
        return response.getResult();
    }

    /**
     * 运营中心分页查看今日付款功能接口
     * @param startAt 支付开始时间
     * @param endAt   支付结束时间
     * @return 订单分页信息
     */
    @RequestMapping(value = "/paging-today-payment-order", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OrderGroup> pagingTodayPaymentOrder(@RequestParam(value = "pageNo", required = false) String pageNo,
                                                      @RequestParam(value = "size", required = false) String size,
                                                      @RequestParam(value = "startAt", required = false) String startAt,
                                                      @RequestParam(value = "endAt", required = false) String endAt) {
        Map<String, String> orderCriteria = Maps.newHashMap();
        DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd");
        // 默认查询当天付款订单
        if (Strings.isNullOrEmpty(startAt)) {
            orderCriteria.put("startAt",DFT.print(DateTime.now().withTimeAtStartOfDay()));
        } else {
            orderCriteria.put("startAt",startAt);
        }
        if (Strings.isNullOrEmpty(endAt)) {
            orderCriteria.put("endAt",DFT.print(DateTime.now().plusDays(1).withTimeAtStartOfDay()));
        } else {
            orderCriteria.put("endAt",endAt);
        }
        orderCriteria.put("pageNo", pageNo);
        orderCriteria.put("size", size);
        orderCriteria.put("pagingTodayPaymentOrder","true");// 加入查询今日付款订单标记
        Response<Paging<OrderGroup>> resp = orderReadLogic.pagingOrder(orderCriteria);
        if (!resp.isSuccess()) {
            log.error("paging today payment order fail ,startAt:{}, endAt:{}, cause:{}", startAt, endAt, resp.getError());
            throw new JsonResponseException("order.today.payment.paging.fail");
        }
        return resp.getResult();
    }


    /**
     * 运营取消订单
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @param sellerNote 卖家备注
     * @return 是否操作成功
     */
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean adminCancelOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                    @RequestParam(required = false) String sellerNote) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        //运营店铺
        Shop shop = getShopByUserId(paranaUser.getId());

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(shop.getId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to seller(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.seller");
        }

        Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.PLATFORM_CANCEL);
        if (isSuccess) {
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.SELLER_CANCEL));
        }

        vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注

        return isSuccess;
    }

    /**
     * 运营跳转线下支付订单(跳过在线支付环节到下一默认环节)
     * 支持所有订单跳转
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @return 是否操作成功
     */
    @RequestMapping(value = "/skip", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean adminSkipForOfflinePaymentOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.PLATFORM_CHECK_FOR_OFFLINE_PAYMENT);
        if (isSuccess) {
            Response<Boolean> booleanResponse = vegaOrderWriteService.updateOrderPayType(orderId, VegaPayType.OFFLINE_PAYMENT.value());
            if (!booleanResponse.isSuccess()) {
                log.error("update shop and sku order pay type (id={}) payType: {} fail,cause: {}", orderId, VegaPayType.OFFLINE_PAYMENT.value(),
                        booleanResponse.getError());
                throw new JsonResponseException("update.shop.order.fail");
            }
        }
        return isSuccess;
    }

    /**
     * 运营审核友云采订单
     * @param orderId 订单Id
     * @param orderType 订单类型
     * @param type 操作类型 1:通过 0:拒绝
     * @param sellerNote 备注
     * @return 是否成功
     */
    @RequestMapping(value = "/check-yc", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean adminCheckYcOrder(@RequestParam("orderId") Long orderId,
                                     @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                     @RequestParam(value = "type") Integer type,
                                     @RequestParam(required = false) String sellerNote) {
        VegaOrderEvent vegaOrderEvent;
        switch (type) {
            case 1:
                vegaOrderEvent = VegaOrderEvent.PLATFORM_CHECK_FOR_YC;
                break;
            case 0:
                vegaOrderEvent = VegaOrderEvent.PLATFORM_REJECT_FOR_YC;
                break;
            default:
                return Boolean.FALSE;
        }

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, vegaOrderEvent);
        if (isSuccess && Arguments.notNull(sellerNote)) {
            vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注
        }

        if (isSuccess) {
            // 执行友云采交期确认接口
            return vegaYouyuncaiComponent.deliveryOrder(orderId, type);
        }

        return isSuccess;
    }


    /**
     * 运营拒绝订单
     *
     * @param orderId   订单id
     * @param orderType 订单类型
     * @param sellerNote 卖家备注
     * @return 是否操作成功
     */
    @RequestMapping(value = "/reject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean adminRejectOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "orderType", defaultValue = "1") Integer orderType,
                                    @RequestParam(required = false) String sellerNote) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        //运营店铺
        Shop shop = getShopByUserId(paranaUser.getId());

        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        if (!Objects.equal(shop.getId(), orderBase.getShopId())) {
            log.error("the order(id={},type={}) not belong to seller(shop id={})",
                    orderId, orderType, paranaUser.getShopId());
            throw new JsonResponseException("order.not.belong.to.seller");
        }

        Boolean isSuccess = vegaOrderWriteLogic.updateOrder(orderBase, orderLevel, VegaOrderEvent.PLATFORM_REJECT);
        if (isSuccess) {
            // 运营直接拒绝的正向订单事件。
            Response<List<Payment>> resp = paymentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.fromInt(orderType));
            List<Payment> paymentList = resp.getResult();
            Payment payment = null;
            for (Payment p : paymentList) {
                if (Objects.equal(p.getStatus(), 1)) {
                    payment = p;
                    break;
                }
            }

            String channel = payment.getChannel();
            Long paymentId = payment.getId();
            String tradeNo = payment.getOutId();
            String paymentCode = payment.getPaySerialNo();;
            Date paidAt = payment.getPaidAt();

            eventBus.post(new VegaOrderAcceptEvent(orderId, channel, paymentId, tradeNo, paymentCode, paidAt, null));
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.REJECT));
        }

        vegaOrderComponent.setOrderRejectSellerNote(orderId,sellerNote);//添加拒绝备注


        return isSuccess;
    }


    /**
     * 运营人员派单
     *
     * @param orderId 订单id
     * @param shopId  要派给某个店铺id
     * @param skuId   要派给某个商品所属店铺
     * @return 是否派送成功
     */
    @RequestMapping(value = "/dispatch/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean dispatchOrder(@PathVariable(value = "id") Long orderId,
                                 @RequestParam(value = "shopId", required = false) Long shopId,
                                 @RequestParam(value = "skuId", required = false) Long skuId,
                                 @RequestParam(value = "orderType", defaultValue = "1") Integer orderType) {

        ParanaUser paranaUser = UserUtil.getCurrentUser();
        OrderLevel orderLevel = OrderLevel.fromInt(orderType);
        OrderBase orderBase = orderReadLogic.findOrder(orderId, orderLevel);
        //判断shop是否有效
        Shop shop = null;
        if (Arguments.notNull(shopId)) {
            shop = getShopById(shopId);
        }
        if (Arguments.notNull(skuId)) {
            shop = getShopBySkuId(skuId);
        }
        if (Arguments.isNull(shop)) {
            log.error("find dispatch order by id:{}  shop id:{} sku id: {} fail", orderId, shopId, skuId);
            throw new JsonResponseException("shop.not.exist");
        }

        VegaOrderEvent orderEvent;
        if (Objects.equal(shop.getType(), VegaShopType.SUPPLIER.value())) {
            orderEvent = VegaOrderEvent.PLATFORM_CHECK;
        } else {
            orderEvent = VegaOrderEvent.PLATFORM_CHECK_FOR_DEALER;
        }

        Boolean isSuccess = vegaOrderWriteLogic.dispatchOrder(orderBase, orderLevel, orderEvent, shop, paranaUser);
        if (isSuccess) {
            //短信提醒事件
            eventBus.post(new TradeSmsEvent(orderId, TradeSmsNodeEnum.DISPATCHER));
        }
        return isSuccess;
    }

    /**
     * 运营后台添加备注(订单备注,发票备注等)
     *
     * @param orderId       订单id
     * @param operationNote 备注
     * @param type          类型
     * @return 是否操作成功
     */
    @RequestMapping(value = "/note", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean adminNoteOrder(@RequestParam("orderId") Long orderId,
                                  @RequestParam(required = false) String operationNote,
                                  @RequestParam(required = false) Integer type) {
        if (Arguments.isNull(type)) {
            type = VegaNoteType.OPERATION_ORDER_NOTE.value();
        }

        return vegaOrderComponent.setOrderNote(orderId,operationNote, type);
    }

    /**
     * 获取订单运营备注详情
     *
     * @param orderId 订单id
     * @return ShopOrder 店铺订单详情
     */
    @RequestMapping(value = "/note/{orderId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String adminNoteOrderInfo(@PathVariable(value = "orderId") Long orderId) {
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(orderId);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",orderId,shopOrderRes.getError());
            throw new JsonResponseException(shopOrderRes.getError());
        }
        Map<String, String> extraMap = shopOrderRes.getResult().getExtra();
        if(!CollectionUtils.isEmpty(extraMap)){
            return extraMap.get(SystemConstant.OPERATION_NOTE);
        }
        return null;
    }

    /**
     * 运营后台订单分页查询新接口,原接口地址:/api/order/paging
     * 塞入成本总价(供货价)
     *
     * @param orderCriteria 分页查询条件
     * @return 分页信息
     */
    @RequestMapping(value = "/pagingByAdmin", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<OrderGroup>> findBy(@RequestParam Map<String, String> orderCriteria) {
        Response<Paging<OrderGroup>> response = orderReadLogic.pagingOrder(orderCriteria);
        if (response.isSuccess()) {
            for (OrderGroup orderGroup : response.getResult().getData()) {
                setOrderSellerPrice(orderGroup);
            }
        }
        return response;
    }

    /**
     * 塞入订单成本价
     *
     * @param orderGroup orderGroup
     */
    private void setOrderSellerPrice(OrderGroup orderGroup) {
        ShopOrder shopOrder = orderGroup.getShopOrder();
        Map<String, String> shopOrderTags = shopOrder.getTags();
        Long orderSellerPrice = 0L;

        Response<VegaOrderDetail> resp = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderGroup.getShopOrder().getId());
        if (!resp.isSuccess()) {
            log.error("vega order detail fail, shopOrderId error:{}", orderGroup.getShopOrder().getId(), resp.getError());
        } else {
            VegaOrderDetail vegaOrderDetail = resp.getResult();
            for (SkuOrder skuOrder : vegaOrderDetail.getSkuOrders()) {
                Map<String, String> tags = skuOrder.getTags();
                String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
                if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                    orderSellerPrice += skuOrder.getQuantity() * Long.valueOf(orderSkuSellerPrice);
                }
            }
            shopOrderTags.put(SystemConstant.ORDER_SELLER_PRICE, orderSellerPrice.toString());
            shopOrder.setTags(shopOrderTags);
        }
    }


    private ShopOrder getShopOrderById(Long shopOrderId) {
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(shopOrderId);
        if (!shopOrderRes.isSuccess()) {
            log.error("find shop order by id:{} fail,error:{}", shopOrderId, shopOrderRes.getError());
            throw new JsonResponseException(shopOrderRes.getError());
        }
        return shopOrderRes.getResult();
    }


    private Shop getShopById(Long shopId) {
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }


    private Shop getShopBySkuId(Long skuId) {

        Response<Sku> skuRes = skuReadService.findSkuById(skuId);
        if (!skuRes.isSuccess()) {
            log.error("find sku by id:{} fail,error:{}", skuId, skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        Response<Shop> shopRes = shopReadService.findById(skuRes.getResult().getShopId());
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", skuRes.getResult().getShopId(), shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }

    private Shop getShopByUserId(Long userId) {
        Response<Shop> shopRes = shopReadService.findByUserId(userId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by user id:{} fail,error:{}", userId, shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }


    @RequestMapping(value = "/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaOrderDetail detail(@RequestParam("id") Long shopOrderId, @RequestParam(required = false) Integer pageNo
            , @RequestParam(required = false) Integer pageSize) {
        Response<VegaOrderDetail> response = this.vegaOrderReadService.pagingVegaOrderDetailByShopId(shopOrderId,flowPicker, pageNo, pageSize);
        if (!response.isSuccess()) {
            log.error("vegaOrder detail paging fail,shopOrderId{},pageNo{},pageSize{}", shopOrderId, pageNo, pageSize);
            throw new JsonResponseException("vega.order.detail.paging.fail");
        }
        return response.getResult();

    }

    /**
     * 修改运费(总订单运费 ShopOrder)
     *
     * @param orderId 订单ID
     * @param shipFee 运费
     * @return 修改结果
     */
    @RequestMapping(value = "/change/ship-fee/{orderId}", method = RequestMethod.PUT)
    public Boolean changeShipFee(@PathVariable(value = "orderId") Long orderId,
                                 @RequestParam(value = "shipFee") Integer shipFee) {
        Response<OrderDetail> orderResp = orderReadService.findOrderDetailById(orderId);
        if (!orderResp.isSuccess()) {
            log.error("failed to find order detail by orderId = ({}), cause : {}",
                    orderId, orderResp.getError());
            throw new JsonResponseException(500, orderResp.getError());
        }
        Long originalFee = orderResp.getResult().getShopOrder().getOriginFee();
        Integer diffFee = Arguments.isNull(orderResp.getResult().getShopOrder().getDiffFee()) ? 0 : orderResp.getResult().getShopOrder().getDiffFee();

        Response<Boolean> resp = vegaOrderWriteService.changeShopOrderShipFeeById(orderId, originalFee + shipFee + diffFee , shipFee);
        if (!resp.isSuccess()) {
            log.error("failed to change ship fee of shopOrder by orderId = ({}), shipFee = ({}), cause : {}",
                    orderId, shipFee, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 修改总单价格(总订单 ShopOrder)
     *
     * @param orderId 订单ID
     * @param modifyFee 修改价格(单位元,前端控制精确到小数2位)
     * @return 修改结果
     */
    @RequestMapping(value = "/change/diff-fee/{orderId}", method = RequestMethod.PUT)
    public Boolean changeDiffFee(@PathVariable(value = "orderId") Long orderId,
                                 @RequestParam(value = "diffFee") Float modifyFee) {
        Response<OrderDetail> orderResp = orderReadService.findOrderDetailById(orderId);
        if (!orderResp.isSuccess()) {
            log.error("failed to find order detail by orderId = ({}), cause : {}",
                    orderId, orderResp.getError());
            throw new JsonResponseException(500, orderResp.getError());
        }
        if(!ImmutableList.of(
                VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),
                VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue()).contains(orderResp.getResult().getShopOrder().getStatus())){
            log.error("failed to change diff fee of shopOrder by orderId = ({}), diffFee = ({})", orderId, modifyFee);
            throw new JsonResponseException("order.status.can.not.change.diff.fee");
        }
        Long originalFee = orderResp.getResult().getShopOrder().getOriginFee(); //产品原价
        Integer shipFee = orderResp.getResult().getShopOrder().getShipFee(); //运费

        Integer diffFee = (int)(modifyFee * 100); //改价金额

        Response<Boolean> resp = vegaOrderWriteService.changeShopOrderDiffFeeById(orderId, originalFee + shipFee + diffFee, diffFee);
        if (!resp.isSuccess()) {
            log.error("failed to change diff fee of shopOrder by orderId = ({}), diffFee = ({}), cause : {}",
                    orderId, modifyFee, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 运营重新打开超时关闭订单(超时关闭—>待买家付款)
     *
     * @param orderId   订单id
     * @return 是否操作成功
     */
    @RequestMapping(value = "/open/{orderId}", method = RequestMethod.PUT)
    public boolean openTimeOutOrderToWaitPay(@PathVariable("orderId") Long orderId) {
        Response<OrderDetail> orderResp = orderReadService.findOrderDetailById(orderId);
        if (!orderResp.isSuccess()) {
            log.error("failed to find order detail by orderId = ({}), cause : {}",
                    orderId, orderResp.getError());
            throw new JsonResponseException(500, orderResp.getError());
        }
        Integer currentStatus = orderResp.getResult().getShopOrder().getStatus();
        Integer newOrderStatus = VegaOrderStatus.NOT_PAID_PLATFORM.getValue();//默认退回 "未付款(下给平台)"状态
        if (!ImmutableList.of(
                VegaOrderStatus.TIMEOUT_CANCEL.getValue(),
                VegaOrderStatus.TIMEOUT_FIRST_DEALER_CANCEL.getValue(),
                VegaOrderStatus.TIMEOUT_SECOND_DEALER_CANCEL.getValue()).contains(currentStatus)) {
            log.info("failed to open order to waitPay id:{}", orderId);
            throw new JsonResponseException("order.status.can.not.open");
        }

        Response<Shop> shopRes = shopReadService.findById(orderResp.getResult().getShopOrder().getShopId());
        if (!shopRes.isSuccess()) {
            log.error("fail to find shop by shop id:{},cause:{}", orderResp.getResult().getShopOrder().getShopId(), shopRes.getError());
            throw new JsonResponseException(500, shopRes.getError());
        }


        //接单店铺为一级
        if (Objects.equal(VegaShopType.DEALER_FIRST.value(), shopRes.getResult().getType())) {
            newOrderStatus = VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue();
        }
        //接单店铺为二级
        if (Objects.equal(VegaShopType.DEALER_SECOND.value(), shopRes.getResult().getType())) {
            newOrderStatus = VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue();
        }

        Response<Boolean> handleRes = orderWriteService.shopOrderStatusChanged(orderId,
                currentStatus,
                newOrderStatus);
        if (!handleRes.isSuccess()) {
            log.info("failed to open order to waitPay id:{}", orderId, handleRes.getError());
            throw new JsonResponseException("order.open.fail");
        }
        return handleRes.getResult();
    }

    //处理日期函数,将前端传来的日期精确到时分秒
    private Date handleDate(Date date) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String dateStr = dateFormat.format(date);
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            log.error("date forMate failed ,cause{}", date);
            e.printStackTrace();
        }
        return date;
    }


    /**
     * 从sku订单总提取共有的操作作为店铺订单操作
     * @param skuOrders sku订单列表
     * @return 店铺订单操作列表
     */
    private Set<OrderOperation> pickCommonSkuOperation(Collection<SkuOrder> skuOrders, Flow flow) {
        //查询店铺操作,所有子订单共有的操作才能在订单级别操作
        ArrayListMultimap<OrderOperation, Long> groupSkuOrderIdByOperation = ArrayListMultimap.create();
        for (SkuOrder skuOrder : skuOrders) {
            Set<OrderOperation> orderOperations = flow.availableOperations(skuOrder.getStatus());
            for (OrderOperation orderOperation : orderOperations) {
                groupSkuOrderIdByOperation.put(orderOperation, skuOrder.getId());
            }
        }
        Set<OrderOperation> shopOperation = Sets.newHashSet();
        for (OrderOperation operation : groupSkuOrderIdByOperation.keySet()) {
            if (com.google.common.base.Objects.equal(groupSkuOrderIdByOperation.get(operation).size(), skuOrders.size())) {
                shopOperation.add(operation);
            }
        }
        return shopOperation;
    }


}
