package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.dto.ShopItemDeliveryFeeTemplate;
import com.sanlux.item.impl.dao.ShopItemDao;
import com.sanlux.item.impl.dao.ShopItemDeliveryFeeDao;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopItemDeliveryFee;
import com.sanlux.item.service.VegaDeliveryFeeTemplateReadService;
import com.sanlux.common.enums.VegaShopType;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.delivery.impl.dao.DeliveryFeeTemplateDao;
import io.terminus.parana.delivery.impl.dao.ItemDeliveryFeeDao;
import io.terminus.parana.delivery.model.DeliveryFeeTemplate;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:cp
 * Created on 8/17/16.
 */
@Service
@RpcProvider
@Slf4j
public class VegaDeliveryFeeTemplateReadServiceImpl implements VegaDeliveryFeeTemplateReadService {

    private final ShopDao shopDao;

    private final DeliveryFeeTemplateDao deliveryFeeTemplateDao;

    private final ItemDeliveryFeeDao itemDeliveryFeeDao;

    private final ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    private final ShopItemDao shopItemDao;

    @Autowired
    public VegaDeliveryFeeTemplateReadServiceImpl(ShopDao shopDao,
                                                  DeliveryFeeTemplateDao deliveryFeeTemplateDao,
                                                  ItemDeliveryFeeDao itemDeliveryFeeDao,
                                                  ShopItemDeliveryFeeDao shopItemDeliveryFeeDao,
                                                  ShopItemDao shopItemDao) {
        this.shopDao = shopDao;
        this.deliveryFeeTemplateDao = deliveryFeeTemplateDao;
        this.itemDeliveryFeeDao = itemDeliveryFeeDao;
        this.shopItemDeliveryFeeDao = shopItemDeliveryFeeDao;
        this.shopItemDao = shopItemDao;
    }

    @Override
    public Response<Boolean> checkIfHasItemBindTemplate(Long deliveryFeeTemplateId) {
        try {
            DeliveryFeeTemplate deliveryFeeTemplate = deliveryFeeTemplateDao.findById(deliveryFeeTemplateId);
            if (deliveryFeeTemplate == null) {
                log.error("delivery fee template(id={}) not found", deliveryFeeTemplateId);
                return Response.fail("delivery.fee.template.not.found");
            }

            Shop shop = shopDao.findById(deliveryFeeTemplate.getShopId());
            if (shop == null) {
                log.error("shop not found where id={}", deliveryFeeTemplate.getShopId());
                return Response.fail("shop.not.found");
            }

            VegaShopType vegaShopType = VegaShopType.from(shop.getType());
            switch (vegaShopType) {
                case SUPPLIER:
                    return Response.ok(itemDeliveryFeeDao.hasBoundTemplate(deliveryFeeTemplateId));
                default:
                    return Response.ok(shopItemDeliveryFeeDao.hasBoundTemplate(deliveryFeeTemplateId));
            }
        } catch (Exception e) {
            log.error("fail to check if has item bind delivery fee template(id={}),cause:{}",
                    deliveryFeeTemplateId, Throwables.getStackTraceAsString(e));
            return Response.fail("delivery.fee.template.check.if.has.item.bind.fail");
        }
    }

    @Override
    public Response<ShopItemDeliveryFeeTemplate> findShopItemDeliveryFeeTemplate(Long shopId, Long itemId) {
        try {
            ShopItem shopItem = shopItemDao.findByShopIdAndItemId(shopId, itemId);
            if (shopItem == null) {
                log.error("shop item not found where shopId={},itemId={}",
                        shopId, itemId);
                return Response.fail("shop.item.not.found");
            }
            ShopItemDeliveryFee shopItemDeliveryFee = shopItemDeliveryFeeDao.findByShopIdAndItemId(shopId, itemId);
            List<DeliveryFeeTemplate> deliveryFeeTemplates = deliveryFeeTemplateDao.findByShopId(shopId);

            ShopItemDeliveryFeeTemplate shopItemDeliveryFeeTemplate = new ShopItemDeliveryFeeTemplate();
            shopItemDeliveryFeeTemplate.setShopItem(shopItem);
            shopItemDeliveryFeeTemplate.setShopItemDeliveryFee(shopItemDeliveryFee);
            shopItemDeliveryFeeTemplate.setDeliveryFeeTemplates(deliveryFeeTemplates);
            return Response.ok(shopItemDeliveryFeeTemplate);
        } catch (Exception e) {
            log.error("fail to find shop item delivery fee template where shopId={},itemId={},cause:{}",
                    shopId, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.delivery.fee.template.find.fail");
        }
    }
}
