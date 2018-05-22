package com.sanlux.item.impl.manager;

import com.sanlux.item.impl.dao.ShopItemDeliveryFeeDao;
import com.sanlux.item.model.ShopItemDeliveryFee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/11/24
 */
@Component
@Slf4j
public class ShopItemDeliveryFeeManager {

    private final ShopItemDeliveryFeeDao shopItemDeliveryFeeDao;

    @Autowired
    public ShopItemDeliveryFeeManager(ShopItemDeliveryFeeDao shopItemDeliveryFeeDao) {
        this.shopItemDeliveryFeeDao = shopItemDeliveryFeeDao;
    }

    @Transactional
    public Boolean batchCreateAndUpdate (List<ShopItemDeliveryFee> toCreate,
                                         List<ShopItemDeliveryFee> toUpdate) {
        for (ShopItemDeliveryFee shopItemDeliveryFee : toCreate) {
            shopItemDeliveryFeeDao.create(shopItemDeliveryFee);
        }
        for (ShopItemDeliveryFee update : toUpdate) {
            shopItemDeliveryFeeDao.update(update);
        }
        return Boolean.TRUE;
    }
}
