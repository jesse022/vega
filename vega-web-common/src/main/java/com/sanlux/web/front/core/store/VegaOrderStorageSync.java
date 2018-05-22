/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopItemReadService;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.item.service.ShopSkuWriteService;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.service.VegaShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.store.dto.SkusDto;
import io.terminus.parana.store.service.LocationReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaOrderStorageSync {

    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;
    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;
    @RpcConsumer
    private ShopItemReadService shopItemReadService;
    @RpcConsumer
    private LocationReadService locationReadService;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private ItemReadService itemReadService;

    public void syncDealerShopSku() {
        Integer pageNo = 1;
        Integer pageSize = 20;

        while (true) {
            VegaShopCriteria criteria = new VegaShopCriteria();
            criteria.setShopType(VegaShopType.DEALER_FIRST.value());
            criteria.setType(VegaShopType.DEALER_FIRST.value());
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);

            Response<Paging<VegaShop>> resp = vegaShopReadService.paging(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to find shopId, cause : {}", resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            Paging<VegaShop> paging = resp.getResult();

            List<VegaShop> shops = paging.getData(); // 店铺信息
            List<Long> shopIds = Lists.newArrayList(); // 店铺IDs
            Map<Long, Long> shopUsers = Maps.newHashMap(); // 店铺->用户 (key->value)
            for (VegaShop vegaShop : shops) {
                Shop shop = vegaShop.getShop();
                shopIds.add(shop.getId());
                shopUsers.put(shop.getId(), shop.getUserId());
            }

            // 同步库存
            if (!shopIds.isEmpty()) {
                doSyncShopSku(shopIds, shopUsers);
            }

            Long total = paging.getTotal();
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }
    }

    /**
     * 手动同步库存
     * @param shopIds 店铺ID
     * @param shopUsers 店铺->用户 (key->value)
     */
    public void manualSync(List<Long> shopIds, Map<Long, Long> shopUsers) {
        doSyncShopSku(shopIds, shopUsers);
    }

    private void doSyncShopSku(List<Long> shopIds, Map<Long, Long> shopUsers) {
        for (Long shopId : shopIds) {
            sync(shopId, shopUsers.get(shopId));
        }
    }

    private void sync(Long shopId, Long ownerId) {
        // Response<Map<Long, Integer>> resp = locationReadService.findSkuTotalList(ownerId, skuIds);
        Response<List<SkusDto>> resp = locationReadService.findAllSkus(ownerId);
        if (!resp.isSuccess()) {
            log.error("failed to find sku quantity by shopId = {}, ownerId = {}, cause : {}",
                    shopId, ownerId, resp.getError());
            // 继续同步下一个店铺的库存, 下次Job再更新就好, 也可手动同步
        }else {
            List<SkusDto> stock = resp.getResult();
            List<List<SkusDto>>  allStocks = createList(stock);

            for(List<SkusDto> skusDtos : allStocks){
                List<ShopSku> syncShopSkus = Lists.newArrayList();
                List<Long> syncItemIds = Lists.newArrayList();

                List<Long> skuIds = Lists.transform(skusDtos, SkusDto::getSkuId);
                List<Sku> skuList = findSkuByIds(skuIds);
                Map<Long, Sku> skuMap = Maps.uniqueIndex(skuList, Sku::getId);

                skusDtos.stream().forEach(skusDto -> {
                    Long skuId = skusDto.getSkuId();
                    if (skuId != null) {
                        ShopSku shopSku = new ShopSku();
                        shopSku.setShopId(shopId);
                        shopSku.setSkuId(skusDto.getSkuId());
                        shopSku.setStockQuantity(skusDto.getTotal());

                        Long itemId = skuMap.get(skusDto.getSkuId()).getItemId();
                        if (!syncItemIds.contains(itemId)) {
                            syncItemIds.add(itemId);
                        }
                        shopSku.setItemId(itemId);
                        shopSku.setStatus(DefaultItemStatus.ITEM_ONSHELF);
                        shopSku.setPrice(1);
                        shopSku.setStockType(0);

                        syncShopSkus.add(shopSku);
                    }
                });


                // 查询店铺商品信息
                List<ShopItem> shopItemList = findShopItems(shopId);
                List<ShopSku> shopSkuList = findShopSkus(shopId);

                // 当前店铺已添加的Items
                List<Long> alreadyExistsItems = Lists.newArrayList();
                alreadyExistsItems.addAll(shopItemList.stream().map(ShopItem::getItemId).collect(Collectors.toList()));

                // 当前店铺已添加的SKUs
                List<Long> alreadyExistsSkus = Lists.newArrayList();
                alreadyExistsSkus.addAll(shopSkuList.stream().map(ShopSku::getSkuId).collect(Collectors.toList()));

                // 需要新建的items
                List<ShopItem> shopItem4Create = Lists.newArrayList();
                syncItemIds.stream().filter(itemId -> !alreadyExistsItems.contains(itemId)).forEach(itemId -> {
                    Item item = findItemById(itemId);

                    ShopItem shopItem = new ShopItem();
                    shopItem.setItemName(item.getName());
                    shopItem.setItemId(itemId);
                    shopItem.setStatus(0); // TODO
                    shopItem.setShopId(shopId);

                    shopItem4Create.add(shopItem);
                });

                // 不存在的新建, 存在的更新
                List<ShopSku> shopSku4Create = Lists.newArrayList();
                List<ShopSku> shopSku2Update = Lists.newArrayList();
                for (ShopSku ss : syncShopSkus) {
                    if (alreadyExistsSkus.contains(ss.getSkuId())) {
                        shopSku2Update.add(ss);
                    }else {
                        shopSku4Create.add(ss);
                    }
                }

                updateExistsStock(shopItem4Create, shopSku4Create, shopSku2Update);
            }
        }
    }

    /**
     * 查询sku信息
     * @param skuIds skuIds
     * @return 信息
     */
    private List<Sku> findSkuByIds(List<Long> skuIds) {
        Response<List<Sku>> resp = skuReadService.findSkusByIds(skuIds);
        if (!resp.isSuccess()) {
            log.error("failed to find sku by skuIds = {}, cause : {}", skuIds, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 查询sku信息
     * @param itemId itemId
     * @return 信息
     */
    private Item findItemById(Long itemId) {
        Response<Item> resp = itemReadService.findById(itemId);
        if (!resp.isSuccess()) {
            log.error("failed to find item by itemId = {}, cause : {}", itemId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 查询店铺商品
     * @param shopId 店铺ID
     * @return 信息
     */
    private List<ShopItem> findShopItems(Long shopId) {
        Integer pageNo = 1;
        Integer pageSize = 200;
        List<ShopItem> shopItems = Lists.newArrayList();
        while(true) {
            Response<Paging<ShopItem>> resp = shopItemReadService.findBy(shopId, null, null, pageNo, pageSize);
            if (!resp.isSuccess()) {
                log.error("failed to find shopItem by shopId = {}, cause : {}", shopId, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<ShopItem> paging = resp.getResult();
            List<ShopItem> shopItemList = paging.getData();
            if (!shopItemList.isEmpty()) {
                shopItems.addAll(shopItemList);
            }

            Long total = paging.getTotal();
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }
        return shopItems;
    }

    /**
     * 查询店铺sku
     * @param shopId 店铺ID
     * @return 信息
     */
    private List<ShopSku> findShopSkus(Long shopId) {
        Integer pageNo = 1;
        Integer pageSize = 200;
        List<ShopSku> shopSkus = Lists.newArrayList();
        while(true) {
            Response<Paging<ShopSku>> resp = shopSkuReadService.pagingByShopId(pageNo, pageSize, shopId);
            if (!resp.isSuccess()) {
                log.error("failed to find shopItem by shopId = {}, cause : {}", shopId, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<ShopSku> paging = resp.getResult();
            List<ShopSku> shopSkuList = paging.getData();
            if (!shopSkuList.isEmpty()) {
                shopSkus.addAll(shopSkuList);
            }

            Long total = paging.getTotal();
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }
        return shopSkus;
    }

    /**
     * 更新已存在的SKU粗存
     * @param shopItem4Create 信息
     * @param shopSku4Create 信息
     * @param shopSku2Update 信息
     */
    private void updateExistsStock(List<ShopItem> shopItem4Create,
                                   List<ShopSku> shopSku4Create,
                                   List<ShopSku> shopSku2Update) {
        Response<Boolean> resp = shopSkuWriteService
                .batchCreateAndUpdate(shopItem4Create, shopSku4Create, shopSku2Update);
        if (!resp.isSuccess()) {
            log.error("failed to batch create and update stock toCreateShopItems = {}, toCreateShopSkus = {}, " +
                    "toUpdateShopSkus = {} cause : {}", shopItem4Create, shopSku4Create, shopSku2Update,
                    resp.getError());
            // let it continue
        }
    }

    /**
     * 数组拆分函数,解决大数据量导致超时问题
     *
     * @param targe 需要拆分的数组
     * @return List
     */
    public static List<List<SkusDto>> createList(List<SkusDto> targe) {
        int size = 5000; //拆分每组大小
        List<List<SkusDto>> listArr = Lists.newArrayList();
        //获取被拆分的数组个数
        int arrSize = targe.size() % size == 0 ? targe.size() / size : targe.size() / size + 1;
        for (int i = 0; i < arrSize; i++) {
            List<SkusDto> sub = Lists.newArrayList();
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
