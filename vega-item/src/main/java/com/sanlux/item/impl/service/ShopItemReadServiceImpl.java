package com.sanlux.item.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.ShopItemDao;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.service.ShopItemReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Author:cp
 * Created on 8/10/16.
 */
@Service
@RpcProvider
@Slf4j
public class ShopItemReadServiceImpl implements ShopItemReadService {

    private final ShopItemDao shopItemDao;

    @Autowired
    public ShopItemReadServiceImpl(ShopItemDao shopItemDao) {
        this.shopItemDao = shopItemDao;
    }

    @Override
    public Response<Optional<ShopItem>> findByShopIdAndItemId(Long shopId, Long itemId) {
        try {
            ShopItem shopItem = shopItemDao.findByShopIdAndItemId(shopId, itemId);
            return Response.ok(Optional.fromNullable(shopItem));
        } catch (Exception e) {
            log.error("fail to find shop item by shopId={},itemId={},cause:{}",
                    shopId, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.find.fail");
        }
    }

    @Override
    public Response<List<ShopItem>> findByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        try {
            if (CollectionUtils.isEmpty(itemIds)) {
                log.error("itemIds can not be empty");
                return Response.fail("item.ids.empty");
            }
            List<ShopItem> shopItems = shopItemDao.findByShopIdAndItemIds(shopId, itemIds);
            return Response.ok(shopItems);
        } catch (Exception e) {
            log.error("fail to find shop items by shopId={},itemIds={},cause:{}",
                    shopId, itemIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.find.fail");
        }
    }

    @Override
    public Response<Paging<ShopItem>> findBy(Long shopId, Long itemId, String itemName, Integer pageNo, Integer pageSize) {
        try {
            if (shopId == null) {
                log.error("shop id can not be null");
                return Response.fail("shop.id.can.not.be.null");
            }

            PageInfo pageInfo = PageInfo.of(pageNo, pageSize);

            ShopItem criteria = new ShopItem();
            criteria.setShopId(shopId);
            if (itemId != null) {
                criteria.setItemId(itemId);
            }
            if (StringUtils.hasText(itemName)){
                criteria.setItemName(itemName);
            }

            Paging<ShopItem> shopItemPaging = shopItemDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria);
            return Response.ok(shopItemPaging);
        } catch (Exception e) {
            log.error("fail to find shop item by shopId={},itemId={},itemName={},pageNo={},pageSize={},cause:{}",
                    shopId, itemId, itemName, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.find.fail");
        }
    }
}
