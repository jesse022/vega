package com.sanlux.item.impl.service;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.dto.RichSkuWithItem;
import com.sanlux.item.dto.RichSkus;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.impl.dao.SkuExtDao;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.VegaSkuReadService;
import com.sanlux.youyuncai.model.VegaItemSync;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:cp
 * Created on 8/11/16.
 */
@Service
@RpcProvider
@Slf4j
public class VegaSkuReadServiceImpl implements VegaSkuReadService {

    private final ItemDao itemDao;

    private final SkuDao skuDao;

    private final ShopSkuDao shopSkuDao;

    private final SkuExtDao skuExtDao;

    @Autowired
    public VegaSkuReadServiceImpl(ItemDao itemDao,
                                  SkuDao skuDao,
                                  ShopSkuDao shopSkuDao,
                                  SkuExtDao skuExtDao) {
        this.itemDao = itemDao;
        this.skuDao = skuDao;
        this.shopSkuDao = shopSkuDao;
        this.skuExtDao = skuExtDao;
    }


    @Override
    public Response<RichSkus> findSkusWithShopSkuPrice(Long shopId, Long itemId) {
        try {
            Item item = itemDao.findById(itemId);
            if (item == null) {
                log.error("item not found where id={}", itemId);
                return Response.fail("item.not.found");
            }

            List<Sku> skus = skuDao.findByItemId(itemId);
            if (CollectionUtils.isEmpty(skus)) {
                log.error("skus not found where itemId={}", itemId);
                return Response.fail("sku.not.found");
            }

            List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopId, itemId);
            Map<Long, ShopSku> shopSkuBySkuIdIndex = Maps.uniqueIndex(shopSkus, new Function<ShopSku, Long>() {
                @Override
                public Long apply(ShopSku shopSku) {
                    return shopSku.getSkuId();
                }
            });

            List<RichSkus.SkuWithShopSkuPrice> skuWithShopSkuPrices = Lists.newArrayListWithCapacity(skus.size());
            for (Sku sku : skus) {
                RichSkus.SkuWithShopSkuPrice skuWithShopSkuPrice = new RichSkus.SkuWithShopSkuPrice();
                skuWithShopSkuPrice.setSku(sku);

                ShopSku shopSku = shopSkuBySkuIdIndex.get(sku.getId());
                if (shopSku != null) {
                    skuWithShopSkuPrice.setShopSkuPrice(shopSku.getPrice());
                }
                skuWithShopSkuPrices.add(skuWithShopSkuPrice);
            }

            RichSkus richSkus = new RichSkus();
            richSkus.setItem(item);
            richSkus.setSkuWithShopSkuPrices(skuWithShopSkuPrices);
            return Response.ok(richSkus);
        } catch (Exception e) {
            log.error("fail to find skus with shop sku price by shopId={},itemId={},cause:{}",
                    shopId, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }

    @Override
    public Response<Paging<Sku>> paging (Integer pageNo, Integer pageSize) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(skuExtDao.paging(pageInfo.getOffset(), pageInfo.getLimit()));
        } catch (Exception e) {
            log.error("fail to paging sku by pageNo:{}, pageSize:{}, cause:{}",
                    pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("paging sku fail");
        }
    }

    @Override
    public Response<List<Sku>> findFrozenItemsByShopId(Long shopId) {
        try {
            if(Arguments.isNull(shopId)){
                return Response.ok(Collections.emptyList());
            }
            List<Sku> skus = skuExtDao.findFrozenItemsByShopId(shopId);
            return Response.ok(skus);
        } catch (Exception e) {
            log.error("fail to find frozen skus by shopId={},status={},cause:{}",
                    shopId, DefaultItemStatus.ITEM_FREEZE, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }

    @Override
    public Response<List<Sku>> findSpeciallyStatusSkus(Integer status) {
        try {
            if(Arguments.isNull(status)){
                return Response.ok(Collections.emptyList());
            }
            List<Sku> skus = skuExtDao.findSpeciallyStatusSkus(status);
            return Response.ok(skus);
        } catch (Exception e) {
            log.error("fail to find skus by status={},cause:{}", status, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }

    @Override
    public Response<List<Sku>> findAllByItemIds(List<Long> itemIds) {
        try {
            if(Arguments.isNull(itemIds)){
                return Response.ok(Collections.emptyList());
            }
            List<Sku> skus = skuExtDao.findAllByItemIds(itemIds);
            return Response.ok(skus);
        } catch (Exception e) {
            log.error("fail to find all skus by itemIds={},cause:{}", itemIds, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }

    @Override
    public Response<Paging<Sku>> pagingBySkuSync(Integer pageNo, Integer pageSize,
                                                 Integer status, Integer channel, Integer syncStatus) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("channel", Arguments.isNull(channel) ? VegaItemSync.Channel.YOU_YUN_CAI.value() : channel);
            criteria.put("type", VegaItemSync.Type.ITEM.value());
            if (Arguments.notNull(status)) {
                criteria.put("status", status);
            }
            if (Arguments.notNull(syncStatus)) {
                criteria.put("syncStatus", syncStatus);
            }
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(skuExtDao.pagingBySkuSync(pageInfo.getOffset(), pageInfo.getLimit(), criteria));
        } catch (Exception e) {
            log.error("failed to page sku by status = {}, cause : {}",
                    status, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.sku.failed");
        }
    }

    @Override
    public Response<List<RichSkuWithItem>> findSkusWithItemAndShopSkuPrice(List<Long> skuIds) {
        try {
            List<Sku> skus = skuDao.findByIds(skuIds);
            if (CollectionUtils.isEmpty(skus)) {
                log.error("skus not found where ids={}", skuIds);
                return Response.fail("sku.not.found");
            }

            List<RichSkuWithItem> richSkuWithItemList = Lists.newArrayList();
            skus.stream().forEach(sku -> {
                Item item = itemDao.findById(sku.getItemId());
                if (item == null) {
                    log.error("item not found where id={}", sku.getItemId());
                    return;
                }

                ShopSku shopSku = shopSkuDao.findByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, sku.getId());
                if (shopSku == null) {
                    log.error("shopSku not found where  shopId={},skuId={}", DefaultId.PLATFROM_SHOP_ID, sku.getId());
                    return;
                }

                RichSkuWithItem richSkuWithItem = new RichSkuWithItem();
                richSkuWithItem.setSkuId(sku.getId());
                richSkuWithItem.setItem(item);
                richSkuWithItem.setSku(sku);
                richSkuWithItem.setShopSkuPrice(shopSku.getPrice());

                richSkuWithItemList.add(richSkuWithItem);
            });

            return Response.ok(richSkuWithItemList);
        } catch (Exception e) {
            log.error("fail to find skus with item and shop sku price by shopId={},skuIds={},cause:{}",
                    DefaultId.PLATFROM_SHOP_ID, skuIds, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }

    public Response<List<Sku>> findAllSkuWithPrice() {
        try {
            List<Sku> skus = skuExtDao.findAllSkuWithPrice();
            return Response.ok(skus);
        } catch (Exception e) {
            log.error("fail to find all sku with price,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sku.find.fail");
        }
    }
}
