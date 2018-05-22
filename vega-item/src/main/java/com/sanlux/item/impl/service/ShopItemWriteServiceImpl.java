package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.item.dto.RichShopItem;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.impl.dao.ShopItemDao;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.impl.manager.ShopItemManager;
import com.sanlux.item.impl.utils.UploadHelper;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Author:cp
 * Created on 8/10/16.
 */
@Slf4j
@Service
@RpcProvider
public class ShopItemWriteServiceImpl implements ShopItemWriteService {

    private final ShopItemManager shopItemManager;

    private final ItemDao itemDao;

    private final SkuDao skuDao;

    private final ShopItemDao shopItemDao;

    private final ShopSkuDao shopSkuDao;

    @Autowired
    public ShopItemWriteServiceImpl(ShopItemManager shopItemManager,
                                    ItemDao itemDao,
                                    SkuDao skuDao,
                                    ShopItemDao shopItemDao,
                                    ShopSkuDao shopSkuDao) {
        this.shopItemManager = shopItemManager;
        this.itemDao = itemDao;
        this.skuDao = skuDao;
        this.shopItemDao = shopItemDao;
        this.shopSkuDao = shopSkuDao;
    }

    @Override
    public Response<Long> create(RichShopItem richShopItem) {
        try {
            Item item = itemDao.findById(richShopItem.getShopItem().getItemId());
            if (item == null) {
                log.error("item not found where id={}", richShopItem.getShopItem().getItemId());
                return Response.fail("item.not.found");
            }
            List<ShopSku> shopSkus = richShopItem.getShopSkus();
            shopSkus.forEach(shopSku -> shopSku.setCategoryId(item.getCategoryId()));
            final Long shopItemId = shopItemManager.create(richShopItem.getShopItem(), shopSkus);
            return Response.ok(shopItemId);
        } catch (Exception e) {
            log.error("fail to create richShopItem:{},cause:{}",
                    richShopItem, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.create.fail");
        }
    }

    @Override
    public Response<Boolean> batchCreateAndUpdate(List<ShopItem> shopItems, List<ShopSku> shopSkus) {
        try {
            List<Long> itemIds = Lists.transform(shopItems, ShopItem::getItemId);
            List<Long> shopSkuIds = Lists.transform(shopSkus, ShopSku::getSkuId);
            Map<Long, ShopSku> shopSkuMap = Maps.uniqueIndex(shopSkus, ShopSku::getSkuId);
            List<ShopItem> existShopItems = shopItemDao.findByShopIdAndItemIds(DefaultId.PLATFROM_SHOP_ID, itemIds);
            List<Long> existShopItemIds = Lists.transform(existShopItems, ShopItem::getItemId);
            shopItems.removeIf(shopItem -> existShopItemIds.contains(shopItem.getItemId()));

            List<ShopSku> existShopSkus = shopSkuDao.findByShopIdAndSkuIds(DefaultId.PLATFROM_SHOP_ID, shopSkuIds);
            List<Long> existShopSkuIds = Lists.transform(existShopSkus, ShopSku::getSkuId);
            existShopSkus.forEach(shopSku -> shopSku.setPrice(shopSkuMap.get(shopSku.getSkuId()).getPrice()));
            shopSkus.removeIf(shopSku -> existShopSkuIds.contains(shopSku.getSkuId()));

            return Response.ok(shopItemManager.batchCreateAndUpdate(shopItems, shopSkus, existShopSkus));
        } catch (Exception e) {
            log.error("fail to batch create ShopItems:{}, shopSkus:{}, cause:{}",
                    shopItems, shopSkus, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.batch.create.fail");
        }
    }

    @Override
    public Response<List<Long>> batchUpdatePriceByExcel(UploadRaw rawData) {
        if (Arguments.isNull(rawData)) {
            log.error("upload batch update seller price excel fail, data error");
            return Response.fail("upload.batch.update.seller.price.excel.fail.data.error");
        }
        try {
            UploadHelper uploadHelper = new UploadHelper(rawData, Sets.newHashSet("item_id", "sku_id", "sku_new_price"));
            Integer lineNum = uploadHelper.lineCount();
            if (lineNum <= 0) {
                // 数据为空
                return Response.fail("upload.batch.update.seller.price.excel.fail.data.error");
            }

            List<Sku> skus = Lists.newArrayList();
            for (int loopSku = 0; loopSku < lineNum; loopSku++) {
                Sku sku = new Sku();
                UploadHelper.LineHelper line = uploadHelper.getLine(loopSku);
                sku.setItemId(line.getValue("item_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                sku.setId(line.getValue("sku_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                sku.setPrice(line.getValue("sku_new_price", true, UploadHelper.INT_VALUE_PROCESSOR));
                skus.add(sku);
            }
            List<Long> itemIds = Lists.transform(skus, Sku::getItemId);
            Set<Long> linkedHashSet = new LinkedHashSet<>(itemIds);
            itemIds = new ArrayList<>(linkedHashSet);

            batchUpdatePriceFunction(itemIds, skus);
            return Response.ok(itemIds);
        } catch (ServiceException e) {
            log.warn("upload batch update seller price excel failed, data(count={} rows), error={}",
                    Iters.nullToEmpty(rawData.getLines()).size(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("upload batch update seller price excel failed, data(count={} rows), cause:{}",
                    Iters.nullToEmpty(rawData.getLines()).size(), Throwables.getStackTraceAsString(e));
            return Response.fail("upload.batch.update.seller.price.excel.fail");
        }
    }


    private void batchUpdatePriceFunction(List<Long> itemIds, List<Sku> skus) throws Exception {
        List<Item> itemList = itemDao.findByIds(itemIds);
        Map<Long, Item> itemMap = Maps.uniqueIndex(itemList, Item::getId);
        List<ShopItem> shopItems = Lists.newArrayListWithCapacity(itemIds.size());
        for (Item item : itemList) {
            ShopItem shopItem = new ShopItem();
            shopItem.setShopId(0L);
            shopItem.setItemName(item.getName());
            shopItem.setStatus(item.getStatus());
            shopItem.setItemId(item.getId());
            shopItems.add(shopItem);
        }

        List<Sku> skuList = skuDao.findByItemIds(itemIds);
        Map<Long, Sku> skuMap = Maps.uniqueIndex(skus, Sku::getId);
        List<ShopSku> shopSkus = Lists.newArrayListWithCapacity(skus.size());
        for (Sku sku : skuList) {
            ShopSku shopSku = ShopSku.from(sku);
            shopSku.setShopId(0L);
            if(!Arguments.isNull(skuMap.get(sku.getId()))) {
                shopSku.setPrice(skuMap.get(sku.getId()).getPrice());
            }else {
                log.error("sku(id={}) not found from item(ids={})", sku.getId(), itemIds);
                throw new JsonResponseException("sku.not.found");
            }
            if(!Arguments.isNull(itemMap.get(shopSku.getItemId()))) {
                shopSku.setCategoryId(itemMap.get(shopSku.getItemId()).getCategoryId());
            }
            shopSkus.add(shopSku);
        }

        batchCreateAndUpdate(shopItems, shopSkus);
    }

}
