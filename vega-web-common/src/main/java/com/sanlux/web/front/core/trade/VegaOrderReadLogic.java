package com.sanlux.web.front.core.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.sanlux.trade.dto.fsm.VegaOrderEvent;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.trade.service.VegaOrderReaderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.order.OrderReadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by songrenfei on 16/12/16
 */
@Component
@Slf4j
public class VegaOrderReadLogic extends OrderReadLogic{

    @Autowired
    private FlowPicker flowPicker;

    @RpcConsumer
    private OrderReadService orderReadService;

    @Autowired
    private ObjectMapper objectMapper;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @Autowired
    private VegaOrderReaderComponent vegaOrderReaderComponent;


    /**
     * 分页查询订单列表
     */
    @Override
    public Response<Paging<OrderGroup>> pagingOrder(Map<String, String> orderCriteria) {
        Boolean isBuyer = Boolean.FALSE;
        Boolean isSecondOrder = Boolean.FALSE;
        Boolean isPagingTodayPaymentOrder = Boolean.FALSE;
        List<Long> ShopIds = Lists.newArrayList();
        if (orderCriteria.containsKey("supplierIsBuyer")) {
            //买家身份登录
            orderCriteria.remove("supplierIsBuyer");
            isBuyer = Boolean.TRUE;
        }
        if (orderCriteria.containsKey("pagingSecondShopOrder")) {
            //一级经销商查询二级经销商订单
            if (!Strings.isNullOrEmpty(orderCriteria.get("pagingSecondShopOrder"))) {
                ShopIds = Splitters.splitToLong(orderCriteria.get("pagingSecondShopOrder"), Splitters.COMMA);
            }
            orderCriteria.remove("pagingSecondShopOrder");
            isSecondOrder = Boolean.TRUE;
        }
        if (orderCriteria.containsKey("pagingTodayPaymentOrder")) {
            //查看今日付款订单
            orderCriteria.remove("pagingTodayPaymentOrder");
            isPagingTodayPaymentOrder = Boolean.TRUE;
        }
        OrderCriteria criteria = objectMapper.convertValue(orderCriteria, OrderCriteria.class);

        if (criteria != null && !Strings.isNullOrEmpty(criteria.getMobile())) {
            Response<User> userR = userReadService.findBy(criteria.getMobile(), LoginType.MOBILE);
            if (!userR.isSuccess()) {
                log.error("fail to find user by mobile {}, error code:{}",
                        criteria.getMobile(), userR.getError());
                return Response.ok(Paging.empty(OrderGroup.class));
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
                return Response.ok(Paging.empty(OrderGroup.class));
            } else {
                Shop shop = shopR.getResult();
                criteria.setShopId(shop.getId());
            }
        }

        Response<Paging<OrderGroup>> ordersR;
        if(isBuyer){
            ordersR = vegaOrderReadService.findByBuyer(criteria);
        } else if(isSecondOrder){
            ordersR = vegaOrderReadService.pagingSecondShopOrder(criteria, ShopIds);
        } else if(isPagingTodayPaymentOrder){
            ordersR = vegaOrderReadService.pagingTodayPaymentOrder(criteria);
        } else {
            ordersR = vegaOrderReadService.findBy(criteria);
        }
        if (!ordersR.isSuccess()) {
            //直接返回交给herd处理
            return ordersR;
        }
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
                        pickShopOrderOperation(byShopOrderId.get(orderGroup.getShopOrder().getId()), flow));

            } catch (Exception e) {
                log.error("fail to find order operations by orderGroup {}, cause:{}, skip it",
                        orderGroup, Throwables.getStackTraceAsString(e));
            }
        }
        return Response.ok(orderGroupPaging);
    }

    /**
     * 从sku订单中取出最大状态，用最大状态的操作做为总单的操作
     * @param skuOrders sku订单列表
     * @return 店铺订单操作列表
     */
    private Set<OrderOperation> pickShopOrderOperation(Collection<SkuOrder> skuOrders, Flow flow) {
        Integer shopOrderStatus = pickOrderstatus((List<SkuOrder>)skuOrders);

        //查询店铺操作,所有子订单共有的操作才能在订单级别操作
        ArrayListMultimap<OrderOperation, Long> groupSkuOrderIdByOperation = ArrayListMultimap.create();
        for (SkuOrder skuOrder : skuOrders) {
            Set<OrderOperation> orderOperations = flow.availableOperations(skuOrder.getStatus());
            for (OrderOperation orderOperation : orderOperations) {
                groupSkuOrderIdByOperation.put(orderOperation, skuOrder.getId());
            }
        }
        Set<OrderOperation> commonSkuOrderOperation = Sets.newHashSet();
        for (OrderOperation operation : groupSkuOrderIdByOperation.keySet()) {
            if (com.google.common.base.Objects.equal(groupSkuOrderIdByOperation.get(operation).size(), skuOrders.size())) {
                commonSkuOrderOperation.add(operation);
            }
        }
        Set<OrderOperation> shopOperation = flow.availableOperations(shopOrderStatus);
        Set<OrderOperation> shopOrderOperation = Sets.newHashSet();
        shopOrderOperation.addAll(shopOperation);

        //确认收货节点，如果某个子单申请了退货，则总单级别没有申请退货按钮
        //如果某个子单申请了退货，总单可以继续入库，所以继续返回子单最大状态下的操作
        if(!Objects.equal(commonSkuOrderOperation.size(),shopOrderOperation.size())){
            if(shopOrderOperation.contains(VegaOrderEvent.RETURN_APPLY.toOrderOperation())){
                shopOrderOperation.remove(VegaOrderEvent.RETURN_APPLY.toOrderOperation());
                return shopOrderOperation;
            }
            return shopOrderOperation;
        }
        return commonSkuOrderOperation;


    }

    private Integer pickOrderstatus(List<SkuOrder> skuOrders) {
        Integer result = Integer.MIN_VALUE;
        for (SkuOrder skuOrder : skuOrders) {
            if(skuOrder.getStatus() > result){
                result = skuOrder.getStatus();
            }
        }
        return result;
    }


}
