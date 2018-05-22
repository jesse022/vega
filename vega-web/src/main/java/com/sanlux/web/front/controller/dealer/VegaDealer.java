package com.sanlux.web.front.controller.dealer;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.category.service.CategoryAutheWriteService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.service.RankReadService;
import com.sanlux.web.front.core.events.CategoryAuthUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Created by cuiwentao
 * on 16/8/8
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/category")
public class VegaDealer {

    @RpcConsumer
    private CategoryAutheWriteService categoryAutheWriteService;
    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @Autowired
    private EventBus eventBus;


    /**
     * 类目授权折扣写服务
     * @param vegaCategoryDiscountDto 授权类目表
     * @return 授权类目表ID
     */
    @RequestMapping(value = "/auth", method = RequestMethod.PUT)
    public Boolean updateAuth(@RequestBody VegaCategoryDiscountDto vegaCategoryDiscountDto) {

        Long shopId = findShopId();

        Response<Boolean> resp = categoryAutheWriteService.updateCategoryDiscount(shopId, vegaCategoryDiscountDto);
        if (!resp.isSuccess()) {
            log.error("create or update auth discount fail,shopId:{}, cause:{}",shopId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        eventBus.post(CategoryAuthUpdateEvent.form(shopId, Boolean.FALSE, Collections.<VegaCategoryDiscountDto>emptyList()));

        return resp.getResult();
    }

    /**
     * 类目授权折扣读服务
     * @return 授权类目表
     */
    @RequestMapping(value = "/auth", method = RequestMethod.GET)
    public CategoryAuthe findCategoryAutheByCurrentUser() {

        Long shopId = findShopId();
        Response<Optional<CategoryAuthe>> resp = categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("read auth discount fail, shopId:{}, cause:{}", shopId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        if (!resp.getResult().isPresent()) {
            log.error("category not auth, shopId:{}", shopId);
            throw new JsonResponseException("category.not.auth");
        }

        return resp.getResult().get();
    }

    private Long findShopId () {
        ParanaUser user = UserUtil.getCurrentUser();
        Response<Shop> shopResponse = shopReadService.findByUserId(user.getId());
        if(!shopResponse.isSuccess()) {
            log.error("current user(userId:{}) donot have shop, cause:{}",user.getId(), shopResponse.getError());
            throw new JsonResponseException(shopResponse.getError());
        }

        return shopResponse.getResult().getId();
    }
}
