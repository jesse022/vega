package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.item.impl.dao.ItemDetailExtDao;
import com.sanlux.item.impl.dao.ItemExtDao;
import com.sanlux.item.impl.manager.VegaItemManager;
import com.sanlux.item.service.VegaItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.common.Digestors;
import io.terminus.parana.item.dto.FullItem;
import io.terminus.parana.item.impl.dao.ItemAttributeDao;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ItemAttribute;
import io.terminus.parana.item.model.ItemDetail;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/10/24
 */
@Service
@RpcProvider
@Slf4j
public class VegaItemWriteServiceImpl implements VegaItemWriteService {

    private final ItemExtDao itemExtDao;

    private final VegaItemManager vegaItemManager;

    private final ItemDetailExtDao itemDetailExtDao;

    private final ItemAttributeDao itemAttributeDao;

    private final ItemDao itemDao;

    private final SkuDao skuDao;

    @Autowired
    public VegaItemWriteServiceImpl(ItemExtDao itemExtDao,
                                    VegaItemManager vegaItemManager,
                                    ItemDetailExtDao itemDetailExtDao,
                                    ItemAttributeDao itemAttributeDao,
                                    ItemDao itemDao,
                                    SkuDao skuDao) {
        this.itemExtDao = itemExtDao;
        this.vegaItemManager = vegaItemManager;
        this.itemDetailExtDao = itemDetailExtDao;
        this.itemAttributeDao = itemAttributeDao;
        this.itemDao = itemDao;
        this.skuDao = skuDao;
    }

    @Override
    public Response<Integer> updateImageByCategoryIdAndShopId(Long categoryId, Long shopId, String mainImage) {
        try {
            return Response.ok(itemExtDao.updateImageByCategoryIdAndShopId(categoryId, shopId, mainImage));
        }catch (Exception e) {
            log.error("update item image by categoryId:{} and shopId:{} fail, cause:{}",
                    categoryId, shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("update.item.image.by.categoryId.and.shopId.fail");
        }
    }

    @Override
    public Response<Boolean> batchUpdateRichText (List<Long> itemIds, String richText) {
        try {
            List<ItemDetail> itemDetails = itemDetailExtDao.findItemDetailsByItemIds(itemIds);
            Map<Long, ItemDetail> itemDetailIndexByItemId = Maps.uniqueIndex(itemDetails, ItemDetail::getItemId);
            List<Item> items = itemDao.findByIds(itemIds);
            Map<Long, Item> itemIndexById = Maps.uniqueIndex(items, Item::getId);
            List<ItemAttribute> itemAttributes = itemAttributeDao.findByItemIds(itemIds);
            Map<Long, ItemAttribute> itemAttributeIndexByItemId = Maps.uniqueIndex(itemAttributes, ItemAttribute::getItemId);

            List<Item> itemsToUpdate = Lists.newArrayList();
            List<ItemDetail> itemDetailToUpdate = Lists.newArrayList();
            for (Long itemId : itemIds) {
                ItemDetail itemDetail = itemDetailIndexByItemId.get(itemId);
                itemDetail.setDetail(richText);
                Item item = itemIndexById.get(itemId);
                String itemInfoMd5 = Digestors.itemDigest(item, itemDetail, itemAttributeIndexByItemId.get(itemId));
                item.setItemInfoMd5(itemInfoMd5);
                itemsToUpdate.add(item);
                itemDetailToUpdate.add(itemDetail);
            }
            return Response.ok(vegaItemManager.batchUpdateRichText(items, itemDetails));
        } catch (Exception e) {
            log.error("batch update item detail fail. itemIds:{}, richText:{}, cause:{}",
                    itemIds, richText, Throwables.getStackTraceAsString(e));
            return Response.fail("batch.update.item.detail.fail");
        }
    }

    @Override
    public Response<Boolean> batchDeleteItemsByShopId(Long shopId, List<Long> itemIds){
        try {
            if(vegaItemManager.batchDeleteItemsByShopId(shopId, itemIds)){
                return Response.ok(Boolean.TRUE);
            }
            log.error("failed to update status of items(ids={}) of shop(id={})", itemIds, shopId);
            return Response.fail("item.update.fail");
        } catch (Exception e) {
            log.error("failed to update status of items(ids={}) of shop(id={}), cause:{}", itemIds, shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateStockQuantity(Long skuId, Integer delta) {
        try {

            vegaItemManager.decreaseBy(skuId,delta);

            return Response.ok();
        }catch (ServiceException e) {
            log.error("update sku(id:{}) stock:{}  fail, cause:{}",
                    skuId, delta, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }catch (Exception e) {
            log.error("update sku(id:{}) stock:{}  fail, cause:{}",
                    skuId, delta, Throwables.getStackTraceAsString(e));
            return Response.fail("update.item.fail");
        }
    }

    @Override
    public Response<Boolean> updateSaleQuantity(Long skuId, Integer delta) {
        try {

            Sku sku = skuDao.findById(skuId);
            if(Arguments.isNull(sku)){
                log.error("sku(id:{}) not exist",skuId);
                return Response.fail("sku.not.exist");
            }
            Item item = itemDao.findById(sku.getItemId());
            if(Arguments.isNull(item)){
                log.error("item(id:{}) not exist",sku.getItemId());
                return Response.fail("item.not.exist");
            }

            itemDao.updateSaleQuantity(item.getId(),delta);
            return Response.ok();
        }catch (Exception e) {
            log.error("update sku(id:{}) stock:{}  fail, cause:{}",
                    skuId, delta, Throwables.getStackTraceAsString(e));
            return Response.fail("update.item.fail");
        }
    }

    @Override
    public Response<Boolean> batchUpdateSellerPrice(List<FullItem> fullItems) {
        try {
            vegaItemManager.batchUpdateSellerPrice(fullItems);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("fail to batch update item seller price (FullItem:{}), cause:{}",
                    fullItems, Throwables.getStackTraceAsString(e));
            return Response.fail("batch.update.item.seller.price.fail");
        }
    }
}
