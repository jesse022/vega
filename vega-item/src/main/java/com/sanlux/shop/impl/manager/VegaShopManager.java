/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.impl.manager;

import com.google.common.base.Optional;
import com.sanlux.category.impl.dao.CategoryAutheDao;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.shop.impl.dao.CreditAlterResumeDao;
import com.sanlux.shop.impl.dao.ShopExtDao;
import com.sanlux.shop.impl.dao.VegaShopExtraDao;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * @author : panxin
 */
@Slf4j
@Component
public class VegaShopManager {

    @Autowired
    private ShopDao shopDao;
    @Autowired
    private VegaShopExtraDao shopExtraDao;
    @Autowired
    private CreditAlterResumeDao creditAlterResumeDao;
    @Autowired
    private CategoryAutheDao categoryAutheDao;
    @Autowired
    private ShopExtDao shopExtDao;

    /**
     * 创建店铺信息
     *
     * @param shop 店铺信息
     * @param shopExtra 店铺extra
     * @return 店铺ID
     */
    @Transactional
    public Long createShop(Shop shop, VegaShopExtra shopExtra) {
        shopDao.create(shop);

        shopExtra.setShopId(shop.getId());
        shopExtraDao.create(shopExtra);

        return shop.getId();
    }

    /**
     * 更新店铺信息
     *
     * @param shop 店铺信息
     * @param shopExtra 店铺extra
     * @return 店铺ID
     */
    @Transactional
    public Long updateShop(Shop shop, VegaShopExtra shopExtra) {
        shopDao.update(shop);

        shopExtra.setShopId(shop.getId());
        shopExtra.setShopName(shop.getName());
        shopExtra.setShopStatus(shop.getStatus());
        shopExtraDao.updateByShopId(shopExtra);

        return shop.getId();
    }

    /**
     * 更新店铺状态信息
     *
     * @param shop 店铺信息
     * @param shopExtra 店铺extra
     * @return 店铺ID
     */
    @Transactional
    public Long updateShopStatusAndType(Shop shop, VegaShopExtra shopExtra) {
        shopExtDao.updateShopStatus(shop);

        shopExtra.setShopId(shop.getId());
        shopExtra.setShopName(shop.getName());
        shopExtraDao.updateStatusByShopId(shopExtra);

        return shop.getId();
    }

    /**
     * 更新店铺状态
     *
     * @param shopId 店铺ID
     * @param status 状态
     */
    @Transactional
    public void updateShopStatus(Long shopId, Integer status, Map<String, String> extra) {
        shopDao.updateStatus(shopId, status);

        if (extra != null) {
            Shop shop = new Shop();
            shop.setId(shopId);
            shop.setExtra(extra);
            shopDao.update(shop);
        }

        shopExtraDao.updateStatus(shopId, status);
    }

    public Long createShop(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume) {
        shopDao.create(shop);

        shopExtra.setShopId(shop.getId());
        shopExtra.setShopName(shop.getName());
        shopExtra.setShopStatus(shop.getStatus());
        shopExtraDao.create(shopExtra);

        if (resume != null) {
            resume.setShopId(shop.getId());
            resume.setShopName(shop.getName());
            creditAlterResumeDao.create(resume);
        }

        return shop.getId();
    }

    public Boolean updateShop(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume) {
        Long shopId = shop.getId();

        shopDao.update(shop);
        shopDao.updateStatus(shopId, shop.getStatus());

        shopExtra.setShopId(shopId);
        shopExtra.setShopName(shop.getName());
        shopExtra.setShopStatus(shop.getStatus());
        shopExtraDao.updateByShopId(shopExtra);

        if (resume != null) {
            resume.setShopId(shopId);
            resume.setShopName(shop.getName());
            creditAlterResumeDao.create(resume);
        }

        Optional<CategoryAuthe> categoryAuthe = categoryAutheDao.findByShopId(shopId);
        if (categoryAuthe.isPresent()
                && !Objects.equals(categoryAuthe.get().getDiscountLowerLimit(), shopExtra.getDiscountLowerLimit())) {
            categoryAutheDao.updateDiscountLowerLimitByShopId(shopId, shopExtra.getDiscountLowerLimit());
        }

        return Boolean.TRUE;
    }
}
