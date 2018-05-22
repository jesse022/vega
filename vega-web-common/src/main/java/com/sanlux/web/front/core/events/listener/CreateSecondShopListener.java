package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.category.service.CategoryAutheWriteService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultName;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.web.front.core.events.CreateSecondShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/9/20
 */
@Component
@Slf4j
public class CreateSecondShopListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private CategoryAutheWriteService categoryAutheWriteService;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onCreateSecondShopEvent (CreateSecondShopEvent event) {

        Long shopId = event.getShopId();

        VegaShopExtra vegaShopExtra = findVegaShopExtraByShopId(shopId);
        if (vegaShopExtra == null || vegaShopExtra.getShopPid() == null) {
            log.error("shop extra not exist or shop do not have shop Pid, shopId:{}", shopId);
            return;
        }

        CategoryAuthe categoryPauthe = findCategoryAuth(vegaShopExtra.getShopPid());
        if (categoryPauthe == null) {
            log.error("find category auth null, shopId:{}", shopId);
            return;
        }

        List<VegaCategoryDiscountDto> discounts = categoryPauthe.getDiscountList();

        // 清空一级在类目上的折扣,折扣树给二级使用
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

        CategoryAuthe categoryAuthe = new CategoryAuthe();
        categoryAuthe.setShopId(shopId);
        categoryAuthe.setShopName("");
        categoryAuthe.setDiscountLowerLimit(vegaShopExtra.getDiscountLowerLimit());
        categoryAuthe.setDiscountList(discounts);

        Response<Long> createResp = categoryAutheWriteService.createOrUpdateCategoryAuthe(categoryAuthe);
        if (!createResp.isSuccess()) {
            log.error("create category auth fail, shopId:{}, cause:{}", shopId, createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
    }


    private CategoryAuthe findCategoryAuth(Long shopId) {
        Response<Optional<CategoryAuthe>> resp = categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("read auth discount fail, shopId:{}, cause:{}", shopId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        if (!resp.getResult().isPresent()) {
            log.warn("category auth null, shopId:{}", shopId);
            return null;
        }
        return resp.getResult().get();
    }


    private VegaShopExtra findVegaShopExtraByShopId(Long shopId) {
        Response<VegaShopExtra> shopExtraResponse = vegaShopReadService.findVegaShopExtraByShopId(shopId);
        if (!shopExtraResponse.isSuccess()) {
            log.error("find vega shop extra fail, shopId:{}, cause:{}", shopId, shopExtraResponse.getError());
            return null;
        }
        return shopExtraResponse.getResult();
    }
}
