package com.sanlux.item.impl.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.storage.impl.manager.DefaultStorageManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/4/16
 * Time: 5:04 PM
 */
@Slf4j
public class VegaStorageManager extends DefaultStorageManager {

    private final ItemDao itemDao;

    private final SkuDao skuDao;

    public VegaStorageManager(ItemDao itemDao, SkuDao skuDao) {
        super(itemDao, skuDao);
        this.itemDao = itemDao;
        this.skuDao = skuDao;
    }



    /**
     *
     * 覆盖父类中的减库存方法,这里减库存后库存为0商品依然为上架状态,不下架。
     * 减少对应产品id (可以是skuId, 也可以是itemId, 甚至也可以是spuId等)及仓库id的库存
     *
     * @param productId   产品id (可以是skuId, 也可以是itemId, 甚至也可以是spuId等)
     * @param productType 产品类型, 决定productId指sku, item还是spu
     * @param warehouseCode 仓库编码
     * @param delta       库存变更数量
     */
    @Override
    @Transactional
    public void decreaseBy(Long productId, Integer productType, String warehouseCode, Integer delta) {
        //TODO 考虑productType和warehouseId
        Sku sku = skuDao.findById(productId);
        if (sku == null) {
            log.error("sku(id={}) not found", productId);
            throw new ServiceException("sku.not.found");
        }
        final Long skuId = sku.getId();

        final Long itemId = sku.getItemId();
        Item item = itemDao.findById(itemId);

        //检查是否有库存
        checkStockIfEnough(item, sku, delta);

        skuDao.updateStockQuantity(skuId, delta);
        itemDao.updateSaleQuantity(itemId, delta);
    }

    private void checkStockIfEnough(Item item, Sku sku, Integer delta) {
        if (item.getStockQuantity() - delta < 0) {
            log.error("stock quantity not enough where item id={},expect quantity:{},but actual quantity:{}",
                    item.getId(), delta, item.getStockQuantity());
            throw new ServiceException("item.stock.quantity.not.enough");
        }
        if (sku.getStockQuantity() - delta < 0) {
            log.error("stock quantity not enough where sku id={},expect quantity:{},but actual quantity:{}",
                    sku.getId(), delta, sku.getStockQuantity());
            throw new ServiceException("sku.stock.quantity.not.enough");
        }
    }
}
