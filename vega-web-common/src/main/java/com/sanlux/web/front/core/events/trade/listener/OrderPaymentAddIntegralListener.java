/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.user.service.UserRankReadService;
import com.sanlux.user.service.UserRankWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.model.OrderPayment;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.web.core.events.trade.OrderPaymentEvent;
import io.terminus.parana.web.core.events.trade.listener.OrderStatusUpdater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

/**
 * 1、订单支付增加用户积分 只有普通用户下单会加积分
 * 2、订单支付增加用户成长值 只有普通用户下单会加成长值
 * Date: 2016-05-18
 */
@Slf4j
@Component
public class OrderPaymentAddIntegralListener {

    @RpcConsumer
    private PaymentReadService paymentReadService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;

    @Autowired
    private OrderStatusUpdater orderStatusUpdater;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private UserRankReadService userRankReadService;
    @RpcConsumer
    private UserRankWriteService userRankWriteService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPayment(OrderPaymentEvent orderPaymentEvent) {
        Long paymentId = orderPaymentEvent.getPaymentId();
        Response<Payment> rPayment = paymentReadService.findById(paymentId);
        if(!rPayment.isSuccess()){
            log.error("failed to find Payment(id={}), error code:{}", paymentId, rPayment.getError());
            return;
        }
        final Payment payment = rPayment.getResult();
        if(!Objects.equal(payment.getStatus(), OrderStatus.PAID.getValue())){ //不是支付成功事件, 直接返回吧
            return;
        }
        Response<List<OrderPayment>> rOrderPayments = paymentReadService.findOrderIdsByPaymentId(paymentId);
        if (!rOrderPayments.isSuccess()) {
            log.error("failed to find orderIds for payment(id={}), error code:{}", paymentId, rOrderPayments.getError());
            return;
        }
        List<OrderPayment> orderPayments = rOrderPayments.getResult();

        if (CollectionUtils.isEmpty(orderPayments)) {
            return;
        }

        //获取下单人
        Long shopOrderId = orderPayments.get(0).getOrderId();
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(shopOrderId);
        if (!shopOrderRes.isSuccess()) {
            log.error("add user integral fail because failed to shop order by id: ({}) ,error:{}", shopOrderId, shopOrderRes.getError());
            return;
        }
        ShopOrder shopOrder = shopOrderRes.getResult();

        //获取下单人信息
        Response<User> userRes = userReadService.findById(shopOrder.getBuyerId());
        if(!userRes.isSuccess()){
            log.error("add user integral fail because failed to find buyer by user id: ({}) ,error:{}", shopOrder.getBuyerId(), userRes.getError());
            return;
        }
        if(!OrderUserType.NORMAL_USER.equals(UserTypeHelper.getOrderUserTypeByRoles(userRes.getResult().getRoles()))){
            log.warn("add user integral skip because current buyer type is normal user ");
            return;
        }


        //增加用户积分 和 成长值
        //1、计算此次消费可兑换的积分
        Response<Shop> shopResponse = shopReadService.findById(DefaultId.PLATFROM_SHOP_ID);
        if (!shopResponse.isSuccess()) {
            log.error("add user integral fail because failed to find shop by id: ({}) ,error:{}", DefaultId.PLATFROM_SHOP_ID, shopResponse.getError());
            return;
        }

        Shop shop = shopResponse.getResult();
        if(CollectionUtils.isEmpty(shop.getTags())){
            log.error("add user integral fail because failed to find shop by id: ({}) ,error:{}", DefaultId.PLATFROM_SHOP_ID, shopResponse.getError());
            return;
        }
        //积分比例
        Long integralScale = Long.valueOf(shop.getTags().get(SystemConstant.INTEGRAL_SCALE));
        Long feeIntegral = getIntegual(payment.getFee(),integralScale);

        //成长值比例
        Long growthValue = Long.valueOf(shop.getTags().get(SystemConstant.GROWTH_VALUE));
        Long feeGrowthValue = getIntegual(payment.getFee(),growthValue);

        //2、获取下单用户的当前积分
        Response<UserRank> userRankRes = userRankReadService.findUserRankByUserId(shopOrder.getBuyerId());
        if (!userRankRes.isSuccess()) {
            log.error("add user integral fail because failed to find user rank by user id: ({}) ,error:{}", shopOrder.getBuyerId(), userRankRes.getError());
            return;
        }
        UserRank userRank = userRankRes.getResult();
        Long userCurrentIntegral = userRank.getIntegration()+feeIntegral;
        Long userCurrentGrowthValue = userRank.getGrowthValue()+feeGrowthValue;

        //3、获取用户最新积分所对应的等级
        Response<Optional<Rank>> rankRes = rankReadService.findRankByIntegral(userCurrentGrowthValue);
        if (!rankRes.isSuccess()) {
            log.error("add user integral fail because failed to find rank by integral: ({}) ,error:{}", userCurrentIntegral, rankRes.getError());
            return;
        }

        if(!rankRes.getResult().isPresent()){
            log.error("add user integral fail because not find rank by integral: ({}) ", userCurrentGrowthValue);
            return;
        }

        Rank rank = rankRes.getResult().get();
        UserRank updateUserRank = new UserRank();
        updateUserRank.setUserId(shopOrder.getBuyerId());
        updateUserRank.setUserName(shopOrder.getBuyerName());
        updateUserRank.setRankId(rank.getId());
        updateUserRank.setRankName(rank.getName());
        updateUserRank.setGrowthValue(userCurrentGrowthValue);
        updateUserRank.setIntegration(userCurrentIntegral);

        //4、更新用户最新的积分和等级信息和成长值信息
        Response<Boolean> updateRes = userRankWriteService.updateUserRank(updateUserRank);
        if (!updateRes.isSuccess()) {
            log.error("add user integral fail because failed to update user rank ,error:{}", updateRes.getError());
        }
    }


    /**
     * 计算消费金额可兑换多少积分
     * @param fee 消费金额
     * @param integralScale 积分兑换比例
     * @return 积分
     */
    private Long getIntegual(Long fee,Long integralScale){
        BigDecimal feeBg = new BigDecimal(fee);
        BigDecimal integralScaleBg = new BigDecimal(integralScale);
        BigDecimal bd = feeBg.divide(integralScaleBg, 0, BigDecimal.ROUND_DOWN);//保留小数点后面2位，向上取整！
        return bd.longValue();
    }



}
