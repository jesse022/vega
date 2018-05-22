package com.sanlux.item.impl.service;

import com.sanlux.item.impl.dao.ShopItemDeliveryFeeDao;
import com.sanlux.item.model.ShopItemDeliveryFee;
import com.sanlux.item.service.ShopItemDeliveryFeeReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.delivery.dto.RichItemDeliveryFee;
import io.terminus.parana.delivery.impl.component.ItemDeliveryFeeDetailMaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:cp
 * Created on 8/25/16.
 */
@Service
@RpcProvider
@Slf4j
public class ShopItemDeliveryFeeReadServiceImpl implements ShopItemDeliveryFeeReadService {

    private final ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    private final ItemDeliveryFeeDetailMaker itemDeliveryFeeDetailMaker;

    @Autowired
    public ShopItemDeliveryFeeReadServiceImpl(ShopItemDeliveryFeeDao shopItemDeliveryFeeDao,
                                              ItemDeliveryFeeDetailMaker itemDeliveryFeeDetailMaker) {
        this.shopItemDeliveryFeeDao = shopItemDeliveryFeeDao;
        this.itemDeliveryFeeDetailMaker = itemDeliveryFeeDetailMaker;
    }

    @Override
    public Response<List<RichItemDeliveryFee>> findDeliveryFeeDetailByShopIdAndItemIds(Long shopId, List<Long> itemIds) {
        try {
            List<ShopItemDeliveryFee> shopItemDeliveryFees = shopItemDeliveryFeeDao.findByShopIdAndItemIds(shopId, itemIds);
            List<RichItemDeliveryFee> richItemDeliveryFees = itemDeliveryFeeDetailMaker.makeFromItemDeliveryFees(shopItemDeliveryFees);
            return Response.ok(richItemDeliveryFees);
        } catch (Exception e) {
            log.error("fail to find shop item delivery fee detail by shopId={},itemIds={},cause:{}",
                    shopId, itemIds);
            return Response.fail("shop.item.delivery.fee.detail.find.fail");
        }
    }
}
