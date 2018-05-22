package com.sanlux.item.impl.service;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.item.dto.RichShopSku;
import com.sanlux.item.dto.ShopSkuCriteria;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Author:cp
 * Created on 8/4/16.
 */
@Service
@RpcProvider
@Slf4j
public class ShopSkuReadServiceImpl implements ShopSkuReadService {

    private final ShopSkuDao shopSkuDao;

    private final SkuDao skuDao;

    @Autowired
    public ShopSkuReadServiceImpl(ShopSkuDao shopSkuDao,
                                  SkuDao skuDao) {
        this.shopSkuDao = shopSkuDao;
        this.skuDao = skuDao;
    }

    @Override
    public Response<Optional<ShopSku>> findByShopIdAndSkuId(Long shopId, Long skuId) {
        try {
            ShopSku shopSku = shopSkuDao.findByShopIdAndSkuId(shopId, skuId);
            return Response.ok(Optional.fromNullable(shopSku));
        } catch (Exception e) {
            log.error("fail to find shop sku by shopId={},skuId={},cause:{}",
                    shopId, skuId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.find.fail");
        }
    }

    @Override
    public Response<List<RichShopSku>> findShopSkuDetail(Long shopId, Long itemId) {
        try {
            List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopId, itemId);
            if (CollectionUtils.isEmpty(shopSkus)) {
                log.error("shop skus not found where shopId={},itemId={}",
                        shopId, itemId);
                return Response.fail("shop.sku.not.found");
            }

            List<Long> skuIds = Lists.transform(shopSkus, new Function<ShopSku, Long>() {
                @Override
                public Long apply(ShopSku shopSku) {
                    return shopSku.getSkuId();
                }
            });

            List<Sku> skus = skuDao.findByIds(skuIds);
            if (CollectionUtils.isEmpty(skus)) {
                log.error("skus not found by ids:{}", skuIds);
                return Response.fail("sku.not.found");
            }
            Map<Long, Sku> skuByIdIndex = Maps.uniqueIndex(skus, new Function<Sku, Long>() {
                @Override
                public Long apply(Sku sku) {
                    return sku.getId();
                }
            });

            List<RichShopSku> richShopSkus = Lists.newArrayListWithCapacity(shopSkus.size());
            for (ShopSku shopSku : shopSkus) {
                RichShopSku richShopSku = new RichShopSku();
                richShopSku.setShopSku(shopSku);
                richShopSku.setSku(skuByIdIndex.get(shopSku.getSkuId()));
                richShopSkus.add(richShopSku);
            }

            return Response.ok(richShopSkus);
        } catch (Exception e) {
            log.error("fail to find shop sku detail by shopId={},itemId={},cause:{}",
                    shopId, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.find.fail");
        }
    }


    @Override
    public Response<Optional<List<ShopSku>>> findByShopIdAndItemId(Long shopId, Long itemId) {
        try {
            List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopId, itemId);
            return Response.ok(Optional.fromNullable(shopSkus));
        } catch (Exception e) {
            log.error("fail to find shop sku by shopId={},itemId={},cause:{}",
                    shopId, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.find.fail");
        }
    }


    @Override
    public Response<List<ShopSku>> findByShopIdAndSkuIds(Long shopId, List<Long> skuIds) {
        try {
            List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndSkuIds(shopId, skuIds);
            return Response.ok(shopSkus);
        } catch (Exception e) {
            log.error("fail to find shop skus by shopId:{}, skuIds:{},cause:{}",
                    shopId, skuIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.find.fail");
        }
    }

    @Override
    public Response<Paging<ShopSku>> pagingByShopId(Integer pageNo, Integer pageSize, Long shopId) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("shopId", shopId);
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(shopSkuDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria));
        } catch (Exception e) {
            log.error("failed to page shopSku by shopId = {}, cause : {}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.shop.sku.failed");
        }
    }

    @Override
    public Response<Optional<List<ShopSku>>> findByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        try {
            List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemIds(shopId, itemIds);
            return Response.ok(Optional.fromNullable(shopSkus));
        } catch (Exception e) {
            log.error("fail to find shop sku by shopId={},itemIds={},cause:{}",
                    shopId, itemIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.sku.find.fail");
        }
    }

    @Override
    public Response<Paging<ShopSku>> shopSkuPaging (ShopSkuCriteria shopSkuCriteria) {
        try {
            return Response.ok(shopSkuDao.paging(shopSkuCriteria.toMap()));
        }catch (Exception e) {
            log.error("failed to page shopSku by shopSkuCriteria = {}, cause : {}",
                    shopSkuCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.shop.sku.failed");
        }
    }
}
