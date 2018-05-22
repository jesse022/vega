package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.impl.dao.SkuExtDao;
import com.sanlux.item.impl.manager.VegaItemManager;
import com.sanlux.item.impl.utils.UploadHelper;
import com.sanlux.item.service.VegaSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.item.dto.FullItem;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by cuiwentao
 * on 16/10/28
 */
@Service
@RpcProvider
@Slf4j
public class VegaSkuWriteServiceImpl implements VegaSkuWriteService {

    private final SkuExtDao skuExtDao;

    private final SkuDao skuDao;

    private final ItemDao itemDao;

    private final VegaItemManager vegaItemManager;

    @Autowired
    public VegaSkuWriteServiceImpl(SkuExtDao skuExtDao,
                                   SkuDao skuDao,
                                   ItemDao itemDao,
                                   VegaItemManager vegaItemManager) {
        this.skuExtDao = skuExtDao;
        this.skuDao = skuDao;
        this.itemDao = itemDao;
        this.vegaItemManager = vegaItemManager;
    }

    @Override
    public Response<Map<String, Object>> uploadToImportRaw(Long shopId, UploadRaw rawData) {
        Map<String, Object> returnMap = Maps.newHashMap();
        if (rawData == null) {
            log.error("upload stock manager fail, data error, shopId:{}", shopId);
            return Response.fail("upload.stock.manager.fail.data.error");
        }

        try {

            UploadHelper uploadHelper = new UploadHelper(rawData, Sets.newHashSet("outer_sku_id"));
            Integer lineNum = uploadHelper.lineCount();
            if (lineNum <= 0) {
                // TODO: 暂时 0 代表终结
                returnMap.put("status", Boolean.FALSE);
                returnMap.put("items", Collections.EMPTY_LIST);
                return Response.ok(returnMap);
            }


            List<Sku> skus = Lists.newArrayList();
            for (int loopSku = 0; loopSku < lineNum; loopSku ++) {
                UploadHelper.LineHelper line = uploadHelper.getLine(loopSku);

                Sku sku = new Sku();
                sku.setShopId(shopId);
                sku.setOuterSkuId(line.getValue("outer_sku_id", true, UploadHelper.STRING_VALUE_PROCESSOR));
                sku.setStockQuantity(line.getValue("stock_quantity", true, UploadHelper.INT_VALUE_PROCESSOR));

                skus.add(sku);

            }
            List<String> skuOuterIds = Lists.transform(skus, Sku::getOuterSkuId);
            Map<String, Sku> skuIndexByOuterId = Maps.uniqueIndex(skus, Sku::getOuterSkuId);
            List<Sku> toUpdates = skuExtDao.findByOuterSkuIds(skuOuterIds);
            for (Sku sku : toUpdates) {
                sku.setStockQuantity(skuIndexByOuterId.get(sku.getOuterSkuId()).getStockQuantity());
            }
            Multimap<Long, Sku> skuIndexByItemId = Multimaps.index(toUpdates, Sku::getItemId);
            List<Long> itemIds = Lists.transform(toUpdates, Sku::getItemId);
            List<Item> items = itemDao.findByIds(itemIds);
            for (Item item : items) {
                Collection<Sku> skus1 = skuIndexByItemId.get(item.getId());
                Integer sum = 0;
                for (Sku sku: skus1) {
                    sum += sku.getStockQuantity();
                }
                item.setStockQuantity(sum);
            }
            Boolean isSuccess = vegaItemManager.batchUpdateStockQuantity(items, toUpdates);

            returnMap.put("status", isSuccess);
            returnMap.put("items", items);

            return Response.ok(returnMap);
        } catch (ServiceException e) {
            log.warn("upload to import raw failed, shopId={}, data(count={} rows), error={}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("upload to import raw failed, shopId={}, data(count={} rows), cause:{}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), Throwables.getStackTraceAsString(e));
            return Response.fail("upload.stock.manager.fail");
        }
    }

    @Override
    public Response<Boolean> batchUpdateOuterSkuId (List<Sku> skuList) {
        try {
            return Response.ok(skuExtDao.batchUpdateOuterSkuId(skuList));
        } catch (Exception e) {
            log.error("fail to batch update outer sku id, skuList:{}, cause:{}",
                    skuList, Throwables.getStackTraceAsString(e));
            return Response.fail("batch.update.sku.fail");
        }
    }


    @Override
    public Response<Boolean> batchUpdateSellerPriceByExcel(Long shopId, UploadRaw rawData) {
        if (Arguments.isNull(rawData)) {
            log.error("upload batch update seller price excel fail, data error, shopId:{}", shopId);
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
                UploadHelper.LineHelper line = uploadHelper.getLine(loopSku);
                Sku sku = new Sku();
                sku.setItemId(line.getValue("item_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                sku.setId(line.getValue("sku_id", true, UploadHelper.LONG_VALUE_PROCESSOR));
                sku.setPrice(line.getValue("sku_new_price", true, UploadHelper.INT_VALUE_PROCESSOR));
                skus.add(sku);
            }
            List<Long> itemIds = Lists.transform(skus, Sku::getItemId);
            Set<Long> linkedHashSet = new LinkedHashSet<>(itemIds);
            itemIds = new ArrayList<>(linkedHashSet);
            Map<Long, Sku> skuMap = Maps.uniqueIndex(skus, Sku::getId);

            List<FullItem> fullItems = Lists.newArrayList();
            List<Long> notSuccesItemIds = Lists.newArrayList();
            int num = 0;
            for (Long itemId : itemIds) {
                List<Sku> skuList = skuDao.findByItemId(itemId);
                if (Arguments.isNullOrEmpty(skuList)) {
                    log.error("find skus by itemId:{} fail, cause:{}", itemId);
                    num++;
                    notSuccesItemIds.add(itemId);
                    continue;
                }
                List<Sku> skuArrayList = Lists.newArrayList();

                //过滤当前用户店铺和冻结状态的商品
                List<Sku> skuListFilter = FluentIterable.from(skuList).filter(sku -> Objects.equals(sku.getShopId(), shopId) &&
                        Objects.equals(sku.getStatus(), DefaultItemStatus.ITEM_FREEZE)).toList();
                if (Arguments.isNullOrEmpty(skuListFilter)) {
                    num++;
                    notSuccesItemIds.add(itemId);
                    continue;
                }
                skuListFilter.stream().forEach(sku -> {
                    Sku skuNew = new Sku();
                    skuNew.setId(sku.getId());
                    Integer price = Arguments.isNull(skuMap.get(sku.getId())) ? sku.getPrice() : skuMap.get(sku.getId()).getPrice();
                    skuNew.setPrice(price);
                    skuNew.setShopId(shopId);
                    skuNew.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);
                    skuArrayList.add(skuNew);
                });

                FullItem fullItem = new FullItem();
                Item item = new Item();
                item.setId(itemId);
                item.setShopId(shopId);
                item.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);
                setItemPriceBySkus(item, skuArrayList);
                fullItem.setItem(item);
                fullItem.setSkus(skuArrayList);
                fullItems.add(fullItem);
            }
            if (Arguments.isNullOrEmpty(fullItems)) {
                log.error("fail to batch update item seller price (itemIds:{}), cause:{the item is not belong to this shop or item status is not freeze}", itemIds);
                throw new JsonResponseException(500, "only.freeze.and.me.item.can.batch.update.seller.price");
            }
            vegaItemManager.batchUpdateSellerPrice(fullItems);

            if (num > 0) {
                //部分成功
                throw new InvalidException(500, "{0}.batch.update.item.seller.price.partial.success", Joiners.COMMA.join(notSuccesItemIds));
            }

            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("upload batch update seller price excel failed, shopId={}, data(count={} rows), error={}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("upload batch update seller price excel failed, shopId={}, data(count={} rows), cause:{}",
                    shopId, Iters.nullToEmpty(rawData.getLines()).size(), Throwables.getStackTraceAsString(e));
            return Response.fail("upload.batch.update.seller.price.excel.fail");
        }
    }

    /**
     * 根据skus获取item表price信息
     *
     * @param item item
     * @param skus skus
     */
    private void setItemPriceBySkus(Item item, List<Sku> skus) {
        if(Arguments.isNullOrEmpty(skus) || skus.size()< 1){
            return;
        }
        Integer lowPrice = skus.get(0).getPrice();
        Integer highPrice =skus.get(0).getPrice();
        for(Sku sku : skus){
            if(lowPrice > sku.getPrice()) lowPrice = sku.getPrice();
            if(highPrice < sku.getPrice()) highPrice = sku.getPrice();
        }
        item.setLowPrice(lowPrice);
        item.setHighPrice(highPrice);
    }
}
