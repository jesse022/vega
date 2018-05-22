package com.sanlux.web.admin.item;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryAuthDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.category.service.CategoryAutheWriteService;
import com.sanlux.common.constants.DefaultDiscount;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultName;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.web.front.core.events.CategoryAuthUpdateEvent;
import com.sanlux.web.front.core.events.FirstShopCreateScopeOrAuthEvent;
import com.sanlux.web.front.core.events.ShopCategoryAuthEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类目授权操作
 * Created by cuiwentao
 * on 16/8/8
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/category")
public class AdminCategory {

    @RpcConsumer
    private CategoryAutheWriteService categoryAutheWriteService;
    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;
    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @Autowired
    private EventBus eventBus;


    /**
     * 类目授权写服务
     * @param categoryAuthe 授权类目表
     * @return 授权类目表ID
     */
    @RequestMapping(value = "/auth", method = RequestMethod.POST)
    public Long createAuth(@RequestBody CategoryAuthe categoryAuthe) {

        Boolean isCreate = Boolean.FALSE;
        if (categoryAuthe.getId() == null) {
            isCreate = Boolean.TRUE;
        }

        Response<Long> writeResp = categoryAutheWriteService.createOrUpdateCategoryAuthe(fillCategory(categoryAuthe));
        if (!writeResp.isSuccess()) {
            log.error("write auth fail shopId:{} ,cause :{}",categoryAuthe.getShopId(), writeResp.getError());
            throw new JsonResponseException(writeResp.getError());
        }
        if (isCreate) {
            eventBus.post(FirstShopCreateScopeOrAuthEvent.from(categoryAuthe.getShopId()));
        }

        eventBus.post(CategoryAuthUpdateEvent.form(categoryAuthe.getShopId(),
                Boolean.TRUE, categoryAuthe.getDiscountList()));

        return writeResp.getResult();
    }


    private VegaShopExtra findShopExtra(Long shopId) {

        Response<VegaShopExtra> shopExtraResponse = vegaShopReadService.findVegaShopExtraByShopId(shopId);

        if (!shopExtraResponse.isSuccess()) {
            log.error("read shop extra fail, shopId:{},cause:{}", shopId, shopExtraResponse.getError());
            throw new JsonResponseException(shopExtraResponse.getError());
        }
        return shopExtraResponse.getResult();

    }


    private List<MemberDiscountDto> makeDiscountDto() {
        Response<List<Rank>> rankResponse = rankReadService.findAll();
        if(!rankResponse.isSuccess()) {
            log.error("read auth discount fail,cause:{}", rankResponse.getError());
            throw new JsonResponseException(rankResponse.getError());
        }
        List<MemberDiscountDto> discountList = Lists.newArrayList();
        for (Rank rank: rankResponse.getResult()) {
            discountList.add(MemberDiscountDto.form(rank.getId(), rank.getName(), DefaultDiscount.MEMBER_RANK_DISCOUNT));
        }
        discountList.add(MemberDiscountDto.form(DefaultId.SECOND_SHOP_RANK_ID, DefaultName.SECOND_SHOP_NAME, DefaultDiscount.MEMBER_RANK_DISCOUNT));
        return discountList;
    }


    /**
     * 重新组装categoryAuthe
     * @param categoryAuthe categoryAuthe
     * @return categoryAuthe
     */
    private CategoryAuthe fillCategory(CategoryAuthe categoryAuthe) {

        List<Long> authCategoryIdList = Lists.transform(categoryAuthe.getAuthList(), VegaCategoryAuthDto::getCategoryId);

        if (categoryAuthe.getId() == null) {
            VegaShopExtra vegaShopExtra = findShopExtra(categoryAuthe.getShopId());
            categoryAuthe.setDiscountLowerLimit(vegaShopExtra.getDiscountLowerLimit());
        }

        List<MemberDiscountDto> memberDiscountDtos = makeDiscountDto();

        List<VegaCategoryDiscountDto> discountDtoList = Lists.newArrayList();

        if (categoryAuthe.getId() != null) {
            CategoryAuthe oldCategoryAuth = findCategoryAutheByShopId(categoryAuthe.getShopId());
            if (Arguments.isNull(oldCategoryAuth)) {
                log.error("find auth empty by shopId:{}", categoryAuthe.getShopId());
                throw new JsonResponseException("find.auth.empty");
            }

            discountDtoList = oldCategoryAuth.getDiscountList();
        }

        List<Long> discountCategoryIds = Lists.transform(discountDtoList, VegaCategoryDiscountDto::getCategoryId);
        for (Long authCategoryId : authCategoryIdList) {

            if (discountCategoryIds.contains(authCategoryId)) {
                continue;
            }
            BackCategory backCategory = findBackCategoryById(authCategoryId);
            discountDtoList.add(VegaCategoryDiscountDto.form(backCategory.getId(), backCategory.getName(),
                    backCategory.getPid(), backCategory.getLevel(), memberDiscountDtos, Boolean.TRUE));

        }

        for (VegaCategoryDiscountDto discount : discountDtoList) {
            discount.setIsUse(Boolean.FALSE);
            if (authCategoryIdList.contains(discount.getCategoryId())) {
                discount.setIsUse(Boolean.TRUE);
            } else {
                for (MemberDiscountDto memberDiscount : discount.getCategoryMemberDiscount()) {
                    memberDiscount.setDiscount(DefaultDiscount.MEMBER_RANK_DISCOUNT);
                }
            }
        }

        categoryAuthe.setDiscountList(discountDtoList);

        return categoryAuthe;
    }


    private BackCategory findBackCategoryById(Long categoryId) {
        Response<BackCategory> backCategoryResp = backCategoryReadService.findById(categoryId);

        if (!backCategoryResp.isSuccess()) {
            log.error("write auth fail because backcategory find fail,categoryId:{}, cause:{}",
                    categoryId, backCategoryResp.getError());
            throw new JsonResponseException(backCategoryResp.getError());
        }

        return backCategoryResp.getResult();
    }

    /**
     * 类目授权读服务
     * @param shopId 店铺ID
     * @return 授权类目表
     */
    @RequestMapping(value = "/auth", method = RequestMethod.GET)
    public CategoryAuthe findCategoryAutheByShopId(@RequestParam Long shopId) {
        Response<Optional<CategoryAuthe>> readAuthResp =  categoryAutheReadService.findCategoryAutheByShopId(shopId);

        if(!readAuthResp.isSuccess()) {
            log.error("read auth by shopId:{} fail, cause:{}", shopId, readAuthResp.getError());
            throw new JsonResponseException(readAuthResp.getError());
        }

        if (!readAuthResp.getResult().isPresent()) {
            return null;
        }
        return readAuthResp.getResult().get();
    }

    /**
     * 运营平台为一级经销商设置类目折扣
     * @param categoryAuthe categoryAuthe
     * @return Boolean
     */
    @RequestMapping(value = "/discount", method = RequestMethod.POST)
    public Long createOrUpdateDiscount(@RequestBody CategoryAuthe categoryAuthe) {
        Response<Long> writeResp = categoryAutheWriteService.createOrUpdateCategoryAuthe(categoryAuthe);
        if (!writeResp.isSuccess()) {
            log.error("write auth fail shopId:{} ,cause :{}",categoryAuthe.getShopId(), writeResp.getError());
            throw new JsonResponseException(writeResp.getError());
        }
        //一级经销商类目折扣缓存失效
        eventBus.post(CategoryAuthUpdateEvent.form(DefaultId.PLATFROM_SHOP_ID,
                Boolean.FALSE, categoryAuthe.getDiscountList()));
        return writeResp.getResult();
    }





}
