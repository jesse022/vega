package com.sanlux.web.front.core.events.listener;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import com.sanlux.item.service.ShopSkuWriteService;
import com.sanlux.item.service.VegaItemReadService;
import com.sanlux.web.front.core.events.ShopCategoryAuthEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 一级经销商授权类目更改事件
 *
 * Created by lujm on 2017/5/8.
 */
@Slf4j
@Component
public class ShopCategoryAuthListener {
    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private CategoryAutheReadService categoryAutheReadService;

    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;

    @RpcConsumer
    private VegaItemReadService vegaItemReadService;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }


    @Subscribe
    public void onShopCategoryAuthEvent(ShopCategoryAuthEvent event) {
        final Long shopId = event.getShopId();
        batchUploadByCategoryAuth(shopId);
    }

    /**
     * 根据店铺ID创建或更新商品信息
     *
     * @param shopId 店铺ID
     */
    private void batchUploadByCategoryAuth(Long shopId) {
        try {
            List<Long> categoryIds = findAuthCategoryIdsByShopId(shopId);
            int size=5;
            if(!CollectionUtils.isEmpty(categoryIds)&&categoryIds.size()>10){
                size=categoryIds.size()/10;
            }

            List<Sku> skusAll = Lists.newArrayList();
            List<List<Long>> categoryIdList = createList(categoryIds, size);
            for (List<Long> cIds : categoryIdList) {
                List<Item> items = Lists.newArrayList();
                items.addAll(findItemsByCategoryIds(cIds));
                List<Sku> skus = Lists.newArrayList();
                if (!CollectionUtils.isEmpty(items)) {
                    List<Long> itemIds = Lists.transform(items, Item::getId);
                    Map<Long, Item> itemIndexById = Maps.uniqueIndex(items, Item::getId);

                    Response<List<Sku>> skusResp = skuReadService.findSkusByItemIds(itemIds);
                    if (!skusResp.isSuccess()) {
                        log.error("find skus by itemIds:{} fail, cause:{}", itemIds, skusResp.getError());
                        continue;
                    }
                    skus = skusResp.getResult();
                    skus.forEach(sku -> sku.setName(itemIndexById.get(sku.getItemId()).getName()));
                }
                skusAll.addAll(skus);
            }

            List<List<Sku>> skuLists = createList(skusAll);
            for(List<Sku> skus : skuLists){
                if (!CollectionUtils.isEmpty(skus)) {
                    Response<Boolean> response = shopSkuWriteService.batchUploadByCategoryAuth(shopId, skus);
                    if (!response.isSuccess()) {
                        log.error("batch update item by category auth failed, cause:{}", response.getError());
                        continue;//继续下一批
                    }
                }
            }

        } catch (Exception e) {
            log.error("batch update item by category auth failed, shopId:{},cause:{} ", shopId, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 根据店铺ID获取类目信息
     *
     * @param shopId 店铺ID
     * @return 类目IDs
     */
    private List<Long> findAuthCategoryIdsByShopId(Long shopId) {
        Response<Optional<CategoryAuthe>> autheResp =
                categoryAutheReadService.findCategoryAutheByShopId(shopId);
        if (!autheResp.isSuccess()) {
            log.error("find category auth by shopId:{} fail, cause:{}",
                    shopId, autheResp.getError());
            return Collections.emptyList();
        }
        if (!autheResp.getResult().isPresent()) {
            log.error("find category auth by shopId:{} empty", shopId);
            return Collections.emptyList();
        }
        List<Long> authCategoryIds = Lists.newArrayList();
        List<VegaCategoryDiscountDto> discountList = autheResp.getResult().get().getDiscountList();
        discountList.forEach(vegaCategoryDiscountDto -> {
            if (vegaCategoryDiscountDto.getIsUse()) {
                authCategoryIds.add(vegaCategoryDiscountDto.getCategoryId());
            }
        });
        List<Long> categoryIds = Lists.newArrayList();
        for (Long categoryId : authCategoryIds) {
            Response<BackCategory> bcResp = backCategoryReadService.findById(categoryId);
            if (!bcResp.isSuccess()) {
                log.error("find backCategory by id:{} fial, cause:{}", categoryId, bcResp.getError());
                return Collections.emptyList();
            }
            if (!bcResp.getResult().getHasChildren()) {
                categoryIds.add(categoryId);
            } else {
                categoryIds.addAll(getChildCategoryIds(categoryId));
            }
        }
        return categoryIds;
    }

    private List<Long> getChildCategoryIds(Long categoryId) {
        Response<List<BackCategory>> bcResp = backCategoryReadService.findChildrenByPid(categoryId);
        if (!bcResp.isSuccess()) {
            log.error("find back categories by pid:{} fail, cause:{}", categoryId, bcResp.getError());
            return Collections.emptyList();
        }
        List<Long> categoryIds = Lists.newArrayList();
        for (BackCategory bc : bcResp.getResult()) {
            if (!bc.getHasChildren()) {
                categoryIds.add(bc.getId());
            } else {
                categoryIds.addAll(getChildCategoryIds(bc.getId()));
            }
        }
        return categoryIds;
    }


    private List<Item> findItemsByCategoryIds(List<Long> categoryIds) {
        Response<List<Item>> itemsResp =
                vegaItemReadService.findItemsByCategoryIds(categoryIds);
        if (!itemsResp.isSuccess()) {
            log.error("find items by categoryIds:{} fail, cause:{}",
                    categoryIds, itemsResp.getError());
            return Collections.emptyList();
        }
        return itemsResp.getResult();
    }

    private static List<List<Long>> createList(List<Long> targe, int size) {
        List<List<Long>> listArr = Lists.newArrayList();
        //获取被拆分的数组个数
        int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
        for (int i = 0; i < arrSize; i++) {
            List<Long> sub = Lists.newArrayList();
            //把指定索引数据放入到list中
            for (int j = i * size; j <= size * (i + 1) - 1; j++) {
                if (j <= targe.size() - 1) {
                    sub.add(targe.get(j));
                }
            }
            listArr.add(sub);
        }
        return listArr;
    }

    /**
     * 数组拆分函数,解决大数据量导致超时问题
     *
     * @param targe 需要拆分的数组
     * @return List
     */
    public static List<List<Sku>> createList(List<Sku> targe) {
        int size = 3000; //拆分每组大小
        List<List<Sku>> listArr = Lists.newArrayList();
        //获取被拆分的数组个数
        int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
        for (int i = 0; i < arrSize; i++) {
            List<Sku> sub = Lists.newArrayList();
            //把指定索引数据放入到list中
            for (int j = i * size; j <= size * (i + 1) - 1; j++) {
                if (j <= targe.size() - 1) {
                    sub.add(targe.get(j));
                }
            }
            listArr.add(sub);
        }
        return listArr;
    }
}
