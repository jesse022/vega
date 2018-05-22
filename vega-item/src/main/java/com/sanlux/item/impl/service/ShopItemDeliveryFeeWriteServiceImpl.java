package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.ShopItemDeliveryFeeDao;
import com.sanlux.item.impl.manager.ShopItemDeliveryFeeManager;
import com.sanlux.item.model.ShopItemDeliveryFee;
import com.sanlux.item.service.ShopItemDeliveryFeeWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:cp
 * Created on 8/16/16.
 */
@Service
@RpcProvider
@Slf4j
public class ShopItemDeliveryFeeWriteServiceImpl implements ShopItemDeliveryFeeWriteService {

    private final ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    private final ShopItemDeliveryFeeManager shopItemDeliveryFeeManager;

    @Autowired
    public ShopItemDeliveryFeeWriteServiceImpl(ShopItemDeliveryFeeDao shopItemDeliveryFeeDao,
                                               ShopItemDeliveryFeeManager shopItemDeliveryFeeManager) {
        this.shopItemDeliveryFeeDao = shopItemDeliveryFeeDao;
        this.shopItemDeliveryFeeManager = shopItemDeliveryFeeManager;
    }

    @Override
    public Response<Boolean> createOrUpdateShopItemDeliveryFee(ShopItemDeliveryFee shopItemDeliveryFee) {
        try {
            final Long shopId = shopItemDeliveryFee.getShopId();
            final Long itemId = shopItemDeliveryFee.getItemId();

            if (shopId == null) {
                log.error("shop id miss when create or update shop item delivery");
                return Response.fail("shop.id.can.not.be.null");
            }
            if (itemId == null) {
                log.error("item id miss when create or update shop item delivery");
                return Response.fail("item.id.can.not.be.null");
            }

            ShopItemDeliveryFee existed = shopItemDeliveryFeeDao.findByShopIdAndItemId(shopId, itemId);
            if (existed != null) {
                ShopItemDeliveryFee toUpdated = new ShopItemDeliveryFee();
                toUpdated.setId(existed.getId());
                toUpdated.setDeliveryFeeTemplateId(shopItemDeliveryFee.getDeliveryFeeTemplateId());
                toUpdated.setDeliveryFee(shopItemDeliveryFee.getDeliveryFee());
                shopItemDeliveryFeeDao.update(toUpdated);
            } else {
                shopItemDeliveryFeeDao.create(shopItemDeliveryFee);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to create or update shop item delivery fee:{},cause:{}",
                    shopItemDeliveryFee, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.delivery.fee.create.or.update.fail");
        }
    }

    @Override
    public Response<Boolean> batchCreateAndUpdateShopItemDeliveryFee (List<ShopItemDeliveryFee> toCreate,
                                                                      List<ShopItemDeliveryFee> toUpdate) {
        try {
            return Response.ok(shopItemDeliveryFeeManager.batchCreateAndUpdate(toCreate, toUpdate));
        } catch (Exception e) {
            log.error("fail to batch  create and update shop item delivery fee:{},cause:{}",
                    toCreate, toUpdate, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.item.delivery.fee.create.or.update.fail");
        }
    }
}
