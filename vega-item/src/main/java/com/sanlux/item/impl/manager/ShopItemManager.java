package com.sanlux.item.impl.manager;

import com.google.common.collect.Lists;
import com.sanlux.item.dto.ShopSkuPrice;
import com.sanlux.item.impl.dao.ShopItemDao;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Author:cp
 * Created on 8/9/16.
 */
@Component
@Slf4j
public class ShopItemManager {

    private final ShopItemDao shopItemDao;

    private final ShopSkuDao shopSkuDao;

    private final SkuDao skuDao;

    @Autowired
    public ShopItemManager(ShopItemDao shopItemDao,
                           ShopSkuDao shopSkuDao,
                           SkuDao skuDao) {
        this.shopItemDao = shopItemDao;
        this.shopSkuDao = shopSkuDao;
        this.skuDao = skuDao;
    }

    @Transactional
    public void create(ShopItem shopItem, ShopSku shopSku) {
        shopItemDao.create(shopItem);
        shopSkuDao.create(shopSku);
    }

    @Transactional
    public Long create(ShopItem shopItem, List<ShopSku> shopSkus) {
        shopItemDao.create(shopItem);
        shopSkuDao.creates(shopSkus);
        return shopItem.getId();
    }

    @Transactional
    public void setShopSkuPrice(List<ShopSkuPrice> shopSkuPrices) throws Exception {
        for (ShopSkuPrice shopSkuPrice : shopSkuPrices) {
            ShopSku existedShopSku = shopSkuDao.findByShopIdAndSkuId(shopSkuPrice.getShopId(), shopSkuPrice.getSkuId());
            if (existedShopSku == null) {
                Sku sku = skuDao.findById(shopSkuPrice.getSkuId());
                if (sku == null) {
                    log.error("sku not found where id={}", shopSkuPrice.getSkuId());
                    throw new ServiceException("sku.not.found");
                }
                ShopSku shopSku = ShopSku.from(sku);
                shopSku.setShopId(shopSkuPrice.getShopId());
                shopSku.setExtraPrice(sku.getExtraPrice());
                shopSkuDao.create(shopSku);
            } else {
                ShopSku updatedShopSku = new ShopSku();
                updatedShopSku.setId(existedShopSku.getId());
                updatedShopSku.setPrice(shopSkuPrice.getPrice());
                shopSkuDao.update(updatedShopSku);
            }
        }
    }

    @Transactional
    public Boolean delete(Long shopSkuId) {

        ShopSku shopSku = shopSkuDao.findById(shopSkuId);
        List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopSku.getShopId(), shopSku.getItemId());

        shopSkuDao.delete(shopSkuId);
        if (shopSkus.size() == 1) {
            ShopItem shopItem = shopItemDao.findByShopIdAndItemId(shopSku.getShopId(), shopSku.getItemId());
            shopItemDao.delete(shopItem.getId());
            return Boolean.TRUE;
        }

        return Boolean.TRUE;
    }

    @Transactional
    public Boolean batchCreateAndUpdate(List<ShopItem> toCreateShopItems,
                                        List<ShopSku> toCreateShopSkus,
                                        List<ShopSku> toUpdateShopSkus) {
        if (!toCreateShopItems.isEmpty()) {
            shopItemDao.creates(toCreateShopItems);
        }

        if (!toCreateShopSkus.isEmpty()) {
            shopSkuDao.creates(toCreateShopSkus);
        }

        if (!toUpdateShopSkus.isEmpty()) {
            shopSkuDao.batchUpdateByShopIdAndSkuId(toUpdateShopSkus);
        }

        return Boolean.TRUE;
    }


}
