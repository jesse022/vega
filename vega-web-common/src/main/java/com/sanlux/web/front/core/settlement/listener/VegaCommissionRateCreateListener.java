package com.sanlux.web.front.core.settlement.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.enums.CommissionBusinessType;
import io.terminus.parana.settle.model.CommissionRule;
import io.terminus.parana.settle.service.CommissionRuleWriteService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.web.core.events.shop.ShopCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * DATE: 16/8/10 上午12:26 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class VegaCommissionRateCreateListener {

    @RpcConsumer
    private CommissionRuleWriteService commissionRuleWriteService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @Autowired
    private EventBus eventBus;

    @Value("${settle.commission.rate.default:0}")
    private Integer defaultRate;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onCreated(ShopCreatedEvent event) {
        try {
            log.info("handle ShopCreatedEvent={}", event);

            Response<Shop> rShop = shopReadService.findById(event.getShopId());
            if (!rShop.isSuccess()) {
                log.error("find shop by id fail, shopId={}, cause={}", event.getShopId(), rShop.getError());
                return;
            }
            Shop shop = rShop.getResult();

            CommissionRule commissionRule = new CommissionRule();
            commissionRule.setBusinessId(event.getShopId());
            commissionRule.setBusinessName(shop.getName());
            commissionRule.setBusinessType(CommissionBusinessType.SHOP.value());
            commissionRule.setRate(defaultRate);
            commissionRule.setCreatedAt(new Date());
            commissionRule.setDescription("默认店铺佣金费率");

            Response<Long> rCreate = commissionRuleWriteService.createCommissionRule(commissionRule);
            if (!rCreate.isSuccess()) {
                log.error("createCommissionRule fail, commissionRule={}, cause={}", commissionRule, rCreate.getError());
            }
        }catch (Exception e){
            log.error("handle ShopCreatedEvent fail, event={}, cause={}", event, Throwables.getStackTraceAsString(e));
        }
    }
}
