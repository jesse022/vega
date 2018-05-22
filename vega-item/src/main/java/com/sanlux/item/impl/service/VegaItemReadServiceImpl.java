package com.sanlux.item.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.impl.dao.ItemExtDao;
import com.sanlux.item.service.VegaItemReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.category.impl.dao.BackCategoryDao;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.item.dto.ItemWithSkus;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/8/31
 */
@Service
@RpcProvider
@Slf4j
public class VegaItemReadServiceImpl implements VegaItemReadService {

    private final ItemDao itemDao;

    private ShopDao shopDao;

    private final BackCategoryDao backCategoryDao;

    private final ItemExtDao itemExtDao;

    private final SkuDao skuDao;


    @Autowired
    public VegaItemReadServiceImpl(ItemDao itemDao,
                                   ShopDao shopDao, BackCategoryDao backCategoryDao,
                                   ItemExtDao itemExtDao,
                                   SkuDao skuDao) {
        this.itemDao = itemDao;
        this.shopDao = shopDao;
        this.backCategoryDao = backCategoryDao;
        this.itemExtDao = itemExtDao;
        this.skuDao = skuDao;
    }

    @Override
    public Response<Paging<Item>> findBy(String itemCode, String itemName,
                                         Integer status, Integer pageNo, Integer pageSize) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("offset", pageInfo.getOffset());
            criteria.put("limit", pageInfo.getLimit());
            if (status != null) {
                criteria.put("status", status);
            }

            if (StringUtils.hasText(itemCode)) {
                criteria.put("itemCode", itemCode);
            }

            if (StringUtils.hasText(itemName)) {
                criteria.put("name", itemName);
            }

            return Response.ok(itemDao.paging(criteria));
        } catch (Exception e) {
            log.error("failed to find items (itemCode:{}, itemName:{}), pageNo={}, pageSize={}, status={}, cause: {}",
                    itemCode, itemName, pageNo, pageSize, status, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<Optional<List<Long>>> findItemIdsByCategoryIdAndShopId(Long categoryId, Long shopId, List<Integer> statuses) {
        try {

            BackCategory backCategory = backCategoryDao.findById(categoryId);
            if (backCategory == null || backCategory.getHasChildren()) {
                log.error("categoryId:{} has children, can not has item");
                return Response.fail("category.can.not.has.item");
            }
            return Response.ok(Optional.fromNullable(itemExtDao.findItemIdsByCategoryIdAndShopId(categoryId, shopId, statuses)));
        } catch (Exception e) {
            log.error("find itemIds by categoryId:{} and shopId:{} fail , cause:{}",
                    categoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.itemIds.by.categoryId.and.shopId.fail");
        }
    }


    @Override
    public Response<Paging<ItemWithSkus>> findItemWithSkusWaitCheck (Integer pageNo, Integer pageSize) {
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("status", DefaultItemStatus.ITEM_WAIT_AUDIT);
            PageInfo page = new PageInfo(pageNo, pageSize);
            criteria.put("offset", page.getOffset());
            criteria.put("limit", page.getLimit());

            Paging<Item> itemPaging = itemDao.paging(criteria);
            if(itemPaging.isEmpty()) {
                return Response.ok(Paging.empty());
            }
            List<Item> items = itemPaging.getData();
            List<Long> itemIds = Lists.transform(items, Item::getId);
            List<ItemWithSkus> itemWithSkuses = Lists.newArrayListWithCapacity(items.size());

            List<Sku> skus = skuDao.findByItemIds(itemIds);
            Multimap<Long, Sku> skuIndexByItemId = Multimaps.index(skus, Sku::getItemId);

            for (Item item : items) {
                ItemWithSkus itemWithSkus = new ItemWithSkus();
                itemWithSkus.setItem(item);
                itemWithSkus.setSkus(new ArrayList<>(skuIndexByItemId.get(item.getId())));
                itemWithSkuses.add(itemWithSkus);
            }

            return Response.ok(new Paging<>(itemPaging.getTotal(), itemWithSkuses));
        } catch (Exception e) {
            log.error("fail to paging item with sku where status = 0,criteria:{}, pageSize:{}, cause:{}",
                    pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("item.with.skus.find.fail");
        }
    }

    @Override
    public Response<Long> countItemCheck () {
        try {
            return Response.ok(itemExtDao.countItemWaitCheck(DefaultItemStatus.ITEM_WAIT_AUDIT));
        } catch (Exception e) {
            log.error("fail to count item where status :{}, cause:{}",
                    DefaultItemStatus.ITEM_WAIT_AUDIT, Throwables.getStackTraceAsString(e));
            return Response.fail("count.item.by.status.fail");
        }
    }

    @Override
    public Response<List<Item>> findItemsByCategoryIds (List<Long> categoryIds) {
        try {
            return Response.ok(itemExtDao.findItemsByCategoryIds(categoryIds));
        } catch (Exception e) {
            log.error("fail to find item by categoryIds:{},cause:{}",
                    categoryIds, Throwables.getStackTraceAsString(e));
            return Response.fail("find.items.by.categoryIds.and.shopId.fail");
        }
    }

    @Override
    public Response<List<Item>> randFindItemsByCategoryId (Long categoryId, Integer limit) {
        try {
            return Response.ok(itemExtDao.randFindItemsByCategoryId(categoryId, limit));
        } catch (Exception e) {
            log.error("fail to find item by categoryId:{},cause:{}",
                    categoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.items.by.categoryIds.and.shopId.fail");
        }
    }


}
