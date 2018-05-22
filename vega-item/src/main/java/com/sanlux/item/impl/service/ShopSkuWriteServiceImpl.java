package com.sanlux.item.impl.service;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.dto.ShopSkuPrice;
import com.sanlux.item.dto.VegaShopItemSkuDto;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.impl.dao.ShopItemDao;
import com.sanlux.item.impl.dao.ShopItemDeliveryFeeDao;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.impl.manager.ShopItemManager;
import com.sanlux.item.impl.utils.UploadHelper;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopItemDeliveryFee;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopItemDeliveryFeeWriteService;
import com.sanlux.item.service.ShopSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.delivery.impl.dao.DeliveryFeeTemplateDao;
import io.terminus.parana.delivery.model.DeliveryFeeTemplate;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Author:cp
 * Created on 8/4/16.
 */
@Service
@RpcProvider
@Slf4j
public class ShopSkuWriteServiceImpl implements ShopSkuWriteService {

    private final ShopSkuDao shopSkuDao;

    private final ShopItemDao shopItemDao;

    private final ItemDao itemDao;

    private final ShopItemManager shopItemManager;

    private final ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    private final DeliveryFeeTemplateDao deliveryFeeTemplateDao;

    @RpcConsumer
    private ShopItemDeliveryFeeWriteService shopItemDeliveryFeeWriteService;

    @Autowired
    public ShopSkuWriteServiceImpl(ShopSkuDao shopSkuDao,
                                   ShopItemDao shopItemDao,
                                   ItemDao itemDao,
                                   ShopItemManager shopItemManager,
                                   ShopItemDeliveryFeeDao shopItemDeliveryFeeDao,
                                   DeliveryFeeTemplateDao deliveryFeeTemplateDao) {
        this.shopSkuDao = shopSkuDao;
        this.shopItemDao = shopItemDao;
        this.itemDao = itemDao;
        this.shopItemManager = shopItemManager;
        this.shopItemDeliveryFeeDao = shopItemDeliveryFeeDao;
        this.deliveryFeeTemplateDao = deliveryFeeTemplateDao;
    }

    @Override
    public Response<Long> create(ShopSku shopSku) {
        try {
            Item item = itemDao.findById(shopSku.getItemId());
            if (item == null) {
                log.error("item not found where id={}", shopSku.getItemId());
                return Response.fail("item.not.found");
            }
            shopSku.setCategoryId(item.getCategoryId());
            ShopItem existedShopItem = shopItemDao.findByShopIdAndItemId(shopSku.getShopId(), shopSku.getItemId());
            if (existedShopItem != null) {
                shopSkuDao.create(shopSku);
            } else {

                ShopItem shopItem = new ShopItem();
                shopItem.setItemId(item.getId());
                shopItem.setItemName(item.getName());
                shopItem.setStatus(item.getStatus());
                shopItem.setShopId(shopSku.getShopId());
                shopItemManager.create(shopItem, shopSku);
            }
            return Response.ok(shopSku.getId());
        } catch (Exception e) {
            log.error("fail to create shopSku:{},cause:{}",
                    shopSku, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateStockQuantity(Long shopId, Long skuId, Integer delta) {
        try {
            ShopSku shopSku = findShopSku(shopId, skuId);
            boolean success = shopSkuDao.updateStockQuantity(shopSku.getId(), delta);
            return Response.ok(success);
        } catch (ServiceException e) {
            log.error("fail to update shopSku stock quantity for shopId={},skuId={},delta={},cause:{}",
                    shopId, skuId, delta, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("fail to update shopSku stock quantity for shopId={},skuId={},delta={},cause:{}",
                    shopId, skuId, delta, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.stock.quantity.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatus(Long shopId, Long skuId, Integer status) {
        try {
            ShopSku shopSku = findShopSku(shopId, skuId);
            boolean success = shopSkuDao.updateStatus(shopSku.getId(), status);
            return Response.ok(success);
        } catch (ServiceException e) {
            log.error("fail to update shopSku status for shopId={},skuId={},cause:{}",
                    shopId, skuId, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("fail to update shopSku status for shopId={},skuId={},cause:{}",
                    shopId, skuId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.status.update.fail");
        }
    }

    @Override
    public Response<Boolean> batchSetPrice(List<ShopSkuPrice> shopSkuPrices) {
        try {
            shopItemManager.setShopSkuPrice(shopSkuPrices);
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            log.error("fail to batch set shop sku price:{},cause:{}",
                    shopSkuPrices, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("fail to batch set shop sku price:{},cause:{}",
                    shopSkuPrices, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.price.set.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long id) {
        try {
            return Response.ok(shopItemManager.delete(id));
        } catch (Exception e) {
            log.error("delete shopSku failed, id:{}, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.delete.fail");
        }
    }

    @Override
    public Response<Boolean> batchUpdateStockByShopIdAndSkuId(List<ShopSku> shopSkus) {
        try {
            return Response.ok(shopSkuDao.batchUpdateByShopIdAndSkuId(shopSkus) == shopSkus.size());
        }catch (Exception e) {
            log.error("failed to batch update stock toUpdateShopSkus = {}, cause : {}",
                    shopSkus, Throwables.getStackTraceAsString(e));
            return Response.fail("batch.update.stock.failed");
        }
    }

    @Override
    public Response<Boolean> batchCreateAndUpdate(List<ShopItem> toCreateShopItems, List<ShopSku> toCreateShopSkus, List<ShopSku> toUpdateShopSkus) {
        try {
            List<Long> itemIds = Lists.transform(toCreateShopSkus, ShopSku::getItemId);
            List<Item> items = itemDao.findByIds(itemIds);
            Map<Long, Item> itemMap = Maps.uniqueIndex(items, Item::getId);
            toCreateShopSkus.forEach(shopSku -> shopSku.setCategoryId(itemMap.get(shopSku.getItemId()).getCategoryId()));
            return Response.ok(shopItemManager.batchCreateAndUpdate(toCreateShopItems, toCreateShopSkus, toUpdateShopSkus));
        }catch (Exception e) {
            log.error("failed to batch create and update stock toCreateShopItems = {}, toCreateShopSkus = {}, " +
                    "toUpdateShopSkus = {} cause : {}", toCreateShopItems, toCreateShopSkus, toUpdateShopSkus,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("batch.update.stock.failed");
        }
    }

    @Override
    public Response<Boolean> uploadToImportRaw (Long shopId, UploadRaw rawData) {
        if (rawData == null) {
            log.error("upload item excel fail, data error, shopId:{}", shopId);
            return Response.fail("upload.item.excel.fail.data.error");
        }

        try {

            UploadHelper uploadHelper = new UploadHelper(rawData, Sets.newHashSet("sku_id"));
            Integer lineNum = uploadHelper.lineCount();
            if (lineNum <= 0) {
                // TODO: 暂时 0 代表终结
                return Response.ok(Boolean.FALSE);
            }

            List<VegaShopItemSkuDto> vegaShopItemSkus = Lists.newArrayList();
            for (int loop = 0; loop < lineNum; loop ++) {
                UploadHelper.LineHelper line = uploadHelper.getLine(loop);

                VegaShopItemSkuDto vegaShopItemSku = new VegaShopItemSkuDto();

                vegaShopItemSku.setShopId(shopId);
                vegaShopItemSku.setItemId(Long.valueOf(line.getValue("item_id", true, UploadHelper.STRING_VALUE_PROCESSOR).trim()));
                vegaShopItemSku.setItemName(line.getValue("item_name", true, UploadHelper.STRING_VALUE_PROCESSOR).trim());
                vegaShopItemSku.setSkuId(Long.valueOf(line.getValue("sku_id", true, UploadHelper.STRING_VALUE_PROCESSOR).trim()));
                String deliveryFeeId = line.getValue("delivery_fee_template_id", false, UploadHelper.STRING_VALUE_PROCESSOR);
                if (!Strings.isNullOrEmpty(deliveryFeeId)) {
                    vegaShopItemSku.setDeliveryFeeTemplateId(Long.valueOf(deliveryFeeId));
                }
                String stock = line.getValue("stock_quantity", false, UploadHelper.STRING_VALUE_PROCESSOR);
                if (!Strings.isNullOrEmpty(stock)) {
                    vegaShopItemSku.setStockQuantity(Integer.valueOf(stock));
                    vegaShopItemSkus.add(vegaShopItemSku);

                }
            }


            List<ShopItem> shopItems = getShopItems(vegaShopItemSkus);
            List<Long> itemIds = Lists.transform(shopItems, ShopItem::getItemId);
            List<ShopSku> shopSkus = getShopSkus(vegaShopItemSkus);
            List<Long> shopSkuIds = Lists.transform(shopSkus, ShopSku::getSkuId);
            Map<Long, ShopSku> shopSkuMap = Maps.uniqueIndex(shopSkus, ShopSku::getSkuId);

            if (!CollectionUtils.isEmpty(itemIds)) {
                batchCreateAndUpdateFunction(shopItems,itemIds,shopSkus,shopSkuIds, shopSkuMap,vegaShopItemSkus,shopId,2);
            }

            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("upload to import raw failed, shopId={}, data(count={} rows), error={}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("upload to import raw failed, shopId={}, data(count={} rows), cause:{}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), Throwables.getStackTraceAsString(e));
            return Response.fail("upload.item.excel.fail");
        }
    }

    @Override
    public Response<Boolean> batchUploadByCategoryAuth (Long shopId,List<Sku> skus) {
        try {
            final Long deliveryFeeTemplateId;
            DeliveryFeeTemplate deliveryFeeTemplate = deliveryFeeTemplateDao.findDefaultByShopId(shopId);
            if (deliveryFeeTemplate == null) {
                log.error("delivery fee default template(shopId={}) not found", shopId);
                deliveryFeeTemplateId = null;
            } else {
                deliveryFeeTemplateId = deliveryFeeTemplate.getId();
            }

            List<VegaShopItemSkuDto> vegaShopItemSkus = Lists.newArrayList();
            skus.forEach(sku -> {
                VegaShopItemSkuDto vegaShopItemSku = new VegaShopItemSkuDto();
                vegaShopItemSku.setShopId(shopId);
                vegaShopItemSku.setItemId(sku.getItemId());
                vegaShopItemSku.setItemName(sku.getName());
                vegaShopItemSku.setSkuId(sku.getId());
                vegaShopItemSku.setDeliveryFeeTemplateId(deliveryFeeTemplateId);
                vegaShopItemSku.setStockQuantity(0);//库存默认为0
                vegaShopItemSkus.add(vegaShopItemSku);
            });


            List<ShopItem> shopItems = getShopItems(vegaShopItemSkus);
            List<Long> itemIds = Lists.transform(shopItems, ShopItem::getItemId);
            List<ShopSku> shopSkus = getShopSkus(vegaShopItemSkus);
            List<Long> shopSkuIds = Lists.transform(shopSkus, ShopSku::getSkuId);
            Map<Long, ShopSku> shopSkuMap = Maps.uniqueIndex(shopSkus, ShopSku::getSkuId);

            if (!CollectionUtils.isEmpty(itemIds)) {
                batchCreateAndUpdateFunction(shopItems,itemIds,shopSkus,shopSkuIds, shopSkuMap,vegaShopItemSkus,shopId,1);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("batch update item by category auth failed, shopId={}, cause:{}", shopId, Throwables.getStackTraceAsString(e));
            return Response.ok(Boolean.FALSE);
        }
    }

    private List<ShopItemDeliveryFee> getDeliveryFee (Long shopId, List<VegaShopItemSkuDto> vegaShopItemSkus) {
        vegaShopItemSkus.removeIf(vegaShopItemSkuDto1 -> vegaShopItemSkuDto1.getDeliveryFeeTemplateId() == null);
        Multimap<Long, VegaShopItemSkuDto> shopItemSkuDtoMultimap =
                Multimaps.index(vegaShopItemSkus, VegaShopItemSkuDto::getItemId);
        List<Long> itemIds = Lists.transform(vegaShopItemSkus, VegaShopItemSkuDto::getItemId);

        List<ShopItemDeliveryFee> deliveryFees = Lists.newArrayList();
        for (Long itemId : new HashSet<>(itemIds)) {
            ShopItemDeliveryFee shopItemDeliveryFee = new ShopItemDeliveryFee();


            Collection<VegaShopItemSkuDto> collection = shopItemSkuDtoMultimap.get(itemId);
            Iterator<VegaShopItemSkuDto> iterator = collection.iterator();
            VegaShopItemSkuDto vegaShopItemSkuDto = iterator.next();
            shopItemDeliveryFee.setShopId(shopId);
            shopItemDeliveryFee.setItemId(vegaShopItemSkuDto.getItemId());
            shopItemDeliveryFee.setDeliveryFeeTemplateId(vegaShopItemSkuDto.getDeliveryFeeTemplateId());

            deliveryFees.add(shopItemDeliveryFee);
        }
        return new ArrayList<>(deliveryFees);
    }

    private List<ShopSku> getShopSkus (List<VegaShopItemSkuDto> vegaShopItemSkus) {
        List<ShopSku> shopSkus = Lists.newArrayList();

        vegaShopItemSkus.forEach(vegaShopItemSkuDto -> {
            ShopSku shopSku = new ShopSku();

            shopSku.setShopId(vegaShopItemSkuDto.getShopId());
            shopSku.setItemId(vegaShopItemSkuDto.getItemId());
            shopSku.setSkuId(vegaShopItemSkuDto.getSkuId());
            shopSku.setStockQuantity(vegaShopItemSkuDto.getStockQuantity());
            shopSku.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);
            shopSku.setPrice(0);
            shopSku.setStockType(0);

            shopSkus.add(shopSku);
        });
        return shopSkus;
    }

    private List<ShopItem> getShopItems(List<VegaShopItemSkuDto> vegaShopItemSkus) {
        Multimap<Long, VegaShopItemSkuDto> shopItemSkuDtoMultimap =
                Multimaps.index(vegaShopItemSkus, VegaShopItemSkuDto::getItemId);
        List<Long> itemIds = Lists.transform(vegaShopItemSkus, VegaShopItemSkuDto::getItemId);

        List<ShopItem> shopItems = Lists.newArrayList();
        for (Long itemId : new HashSet<>(itemIds)) {
            ShopItem shopItem = new ShopItem();

            Collection<VegaShopItemSkuDto> collection = shopItemSkuDtoMultimap.get(itemId);
            Iterator<VegaShopItemSkuDto> iterator = collection.iterator();
            VegaShopItemSkuDto vegaShopItemSkuDto = iterator.next();
            shopItem.setShopId(vegaShopItemSkuDto.getShopId());
            shopItem.setItemId(vegaShopItemSkuDto.getItemId());
            shopItem.setItemName(vegaShopItemSkuDto.getItemName());
            shopItem.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);

            shopItems.add(shopItem);
        }
        return shopItems;
    }

    private ShopSku findShopSku(Long shopId, Long skuId) {
        ShopSku shopSku = shopSkuDao.findByShopIdAndSkuId(shopId, skuId);
        if (shopSku == null) {
            log.error("shopSku not found where shopId={},skuId={}",
                    shopId, skuId);
            throw new ServiceException("shop.sku.not.found");
        }
        return shopSku;
    }

    /**
     * 封装批量创建更新经销商商品信息函数
     *
     * @param shopItems shopItems
     * @param itemIds itemIds
     * @param shopSkus shopSkus
     * @param shopSkuIds shopSkuIds
     * @param shopSkuMap shopSkuMap
     * @param vegaShopItemSkus vegaShopItemSkus
     * @param shopId shopId
     * @param type 类型 1:一级经销商授权类目修改触发   2:一级经销商中心更新库存
     */
    private void batchCreateAndUpdateFunction(List<ShopItem> shopItems, List<Long> itemIds, List<ShopSku> shopSkus, List<Long> shopSkuIds,
                                              Map<Long, ShopSku> shopSkuMap, List<VegaShopItemSkuDto> vegaShopItemSkus, Long shopId,Integer type) {
        //处理库存
        List<ShopItem> existShopItems = shopItemDao.findByShopIdAndItemIds(shopId, itemIds);
        List<Long> existShopItemIds = Lists.transform(existShopItems, ShopItem::getItemId);
        shopItems.removeIf(shopItem -> existShopItemIds.contains(shopItem.getItemId()));

        List<ShopSku> existShopSkus = shopSkuDao.findByShopIdAndSkuIds(shopId, shopSkuIds);
        List<Long> existShopSkuIds = Lists.transform(existShopSkus, ShopSku::getSkuId);
        if(Objects.equals(type,2)) {
            //一级经销商更新库存时执行,否者不更新库存
            existShopSkus.forEach(shopSku ->
                    shopSku.setStockQuantity(shopSkuMap.get(shopSku.getSkuId()).getStockQuantity()));
        }
        shopSkus.removeIf(shopSku -> existShopSkuIds.contains(shopSku.getSkuId()));

        this.batchCreateAndUpdate(shopItems, shopSkus, existShopSkus);

        //处理运费模板
        List<ShopItemDeliveryFee> shopItemDeliveryFees = getDeliveryFee(shopId, vegaShopItemSkus);
        Map<Long, ShopItemDeliveryFee> shopItemDeliveryFeeMap =
                Maps.uniqueIndex(shopItemDeliveryFees, ShopItemDeliveryFee::getItemId);
        itemIds = Lists.transform(shopItemDeliveryFees, ShopItemDeliveryFee::getItemId);

        if (!CollectionUtils.isEmpty(itemIds)) {
            List<ShopItemDeliveryFee> existDeliveryFees =
                    shopItemDeliveryFeeDao.findByShopIdAndItemIds(shopId, itemIds);
            List<Long> existItemIds = Lists.transform(existDeliveryFees, ShopItemDeliveryFee::getItemId);
            if(Objects.equals(type,2)) {
                //一级经销商更新库存时执行,否者不更新运费模板
                existDeliveryFees.forEach(shopItemDeliveryFee ->
                        shopItemDeliveryFee.setDeliveryFeeTemplateId(shopItemDeliveryFeeMap.get(shopItemDeliveryFee.getItemId()).getDeliveryFeeTemplateId()));
            }
            shopItemDeliveryFees.removeIf(shopItemDeliveryFee -> existItemIds.contains(shopItemDeliveryFee.getItemId()));

            shopItemDeliveryFeeWriteService.batchCreateAndUpdateShopItemDeliveryFee(shopItemDeliveryFees, existDeliveryFees);
        }
    }
}
