package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.category.service.CategoryAutheWriteService;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultName;
import com.sanlux.item.model.ShopSku;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.web.front.core.events.CategoryAuthUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by cuiwentao
 * on 16/8/22
 */
@Component
@Slf4j
public class CategoryAuthUpdateListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private VegaCategoryAuthByShopIdCacherService vegaCategoryAuthByShopIdCacherService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private CategoryAutheWriteService categoryAutheWriteService;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }


    /**
     * 失效缓存
     * @param categoryAuthUpdateEvent categoryAuthUpdateEvent
     */
    @Subscribe
    public void onCategoryAuthUpdate(CategoryAuthUpdateEvent categoryAuthUpdateEvent) {

        Long shopId = categoryAuthUpdateEvent.getShopId();
        Boolean isAuthUpdated = categoryAuthUpdateEvent.getIsAuthUpdated();
        List<Long> shopIds = Lists.newArrayList();
        shopIds.add(shopId);

        if (isAuthUpdated) {
            shopIds.addAll(getChildrenShopId(shopId));
        }

        Response<Boolean> response = vegaCategoryAuthByShopIdCacherService.invalidByShopIds(shopIds);
        if (!response.isSuccess()) {
            log.error("invalid categoryAuth by shopIds: {} failed, cause:{}",
                    shopIds, response.getError());
        }
    }

    /**
     * 创建二级经销商类目树
     * @param event event
     */
    @Subscribe
    public void createOrUpdateChildrenCategoryDiscount(CategoryAuthUpdateEvent event) {

        Boolean isAuthUpdated = event.getIsAuthUpdated();
        if (!isAuthUpdated) {
            return;
        }

        Long shopId = event.getShopId();
        List<VegaCategoryDiscountDto> discounts = event.getCategoryDiscounts();
        MemberDiscountDto secondShopDiscount = MemberDiscountDto.form(DefaultId.SECOND_SHOP_RANK_ID,
                DefaultName.SECOND_SHOP_NAME, DefaultDiscount.MEMBER_RANK_DISCOUNT);
        for (VegaCategoryDiscountDto  vegaCategoryDiscount : discounts) {
            List<MemberDiscountDto> memberDiscountDtos = vegaCategoryDiscount.getCategoryMemberDiscount();
            for (MemberDiscountDto memberDiscount : memberDiscountDtos) {
                memberDiscount.setDiscount(DefaultDiscount.MEMBER_RANK_DISCOUNT);
            }
            if (memberDiscountDtos.contains(secondShopDiscount)) {
                memberDiscountDtos.remove(secondShopDiscount);
            }
        }

        List<Long> shopIds = getChildrenShopId(shopId);

        if(CollectionUtils.isEmpty(shopIds)){
            return;
        }

        Response<List<CategoryAuthe>> response = categoryAutheReadService.findByShopIds(shopIds);
        if (!response.isSuccess()) {
            log.error("find children categoryAuth fail, shopIds:{}, cause:{}", shopIds, response.getError());
            return;
        }

        List<CategoryAuthe> categoryAuthes = response.getResult();
        for (CategoryAuthe categoryAuthe : categoryAuthes) {
            categoryAuthe.setDiscountList(discounts);
        }

        List<Long> hasCategoryShopId = Lists.transform(categoryAuthes, new Function<CategoryAuthe, Long>() {
            @Override
            public Long apply(CategoryAuthe categoryAuthe) {
                return categoryAuthe.getShopId();
            }
        });
        shopIds.removeAll(hasCategoryShopId);

        if (!CollectionUtils.isEmpty(shopIds)) {
            for (Long noCategoryShopId : shopIds) {
                CategoryAuthe categoryAuthe = new CategoryAuthe();
                categoryAuthe.setShopId(noCategoryShopId);
                categoryAuthe.setShopName("");
                categoryAuthe.setDiscountList(discounts);

                VegaShopExtra vegaShopExtra = findShopExtraByShopId(noCategoryShopId);
                if (vegaShopExtra != null) {
                    categoryAuthe.setDiscountLowerLimit(vegaShopExtra.getDiscountLowerLimit());
                }

                categoryAuthes.add(categoryAuthe);
            }
        }

        for (CategoryAuthe categoryAuthe : categoryAuthes) {
            Response<Long> resp = categoryAutheWriteService.createOrUpdateCategoryAuthe(categoryAuthe);
            if (!resp.isSuccess()) {
                log.error("create or update category auth fail, shopId:{}, cause:{}",
                        categoryAuthe.getShopId(), resp.getError());
            }
        }

    }

    private List<Long> getChildrenShopId (Long shopId) {
        Response<List<VegaShopExtra>> vegaShopExtrasResp =
                vegaShopReadService.findVegaShopExtrasByShopPid(shopId);
        if (!vegaShopExtrasResp.isSuccess()) {
            log.error("find children shop fail,shopPid:{}, cause:{}",shopId, vegaShopExtrasResp.getError());
            return Collections.<Long>emptyList();
        }
        return Lists.transform(vegaShopExtrasResp.getResult(), new Function<VegaShopExtra, Long>() {
            @Override
            public Long apply(VegaShopExtra vegaShopExtra) {
                return vegaShopExtra.getShopId();
            }
        });
    }

    private VegaShopExtra findShopExtraByShopId(Long shopId) {
        Response<VegaShopExtra> vegaShopExtraResponse = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!vegaShopExtraResponse.isSuccess()) {
            log.error("find vega shop extra by shopId fail, shopId:{},cause:{}",
                    shopId, vegaShopExtraResponse.getError());
            return null;
        }
        return vegaShopExtraResponse.getResult();
    }

}
