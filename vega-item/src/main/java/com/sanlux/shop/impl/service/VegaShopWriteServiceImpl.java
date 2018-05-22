/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.impl.service;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.sanlux.shop.impl.dao.VegaShopExtraDao;
import com.sanlux.shop.impl.manager.VegaShopManager;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author : panxin
 */
@Slf4j
@Service
@RpcProvider
public class VegaShopWriteServiceImpl implements VegaShopWriteService {

    @Autowired
    private VegaShopManager vegaShopManager;

    @Autowired
    private ShopDao shopDao;

    @Autowired
    private VegaShopExtraDao vegaShopExtraDao;

    @Override
    public Response<Long> create(Shop shop, VegaShopExtra shopExtra) {
        try {
            vegaShopManager.createShop(shop, shopExtra);
            return Response.ok(shop.getId());
        }catch (Exception e) {
            log.error("failed to create shop : ({}), shopExtra : ({}), cause : {}",
                    shop, shopExtra, Throwables.getStackTraceAsString(e));
            return Response.fail("create.shop.failed");
        }
    }

    @Override
    public Response<Long> create(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume) {
        try {
            vegaShopManager.createShop(shop, shopExtra, resume);
            return Response.ok(shop.getId());
        }catch (Exception e) {
            log.error("failed to create shop : ({}), shopExtra : ({}), credit resume : ({}), cause : {}",
                    shop, shopExtra, resume, Throwables.getStackTraceAsString(e));
            return Response.fail("create.shop.failed");
        }
    }

    @Override
    public Response<Boolean> update(Shop shop, VegaShopExtra shopExtra) {
        try {
            vegaShopManager.updateShop(shop, shopExtra);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to update shop : ({}), shopExtra : ({}), cause : {}",
                    shop, shopExtra, Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.failed");
        }
    }

    @Override
    public Response<Boolean> updateShopStatus(Shop shop, VegaShopExtra shopExtra) {
        try {
            vegaShopManager.updateShopStatusAndType(shop, shopExtra);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to update shop : ({}), shopExtra : ({}), cause : {}",
                    shop, shopExtra, Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.failed");
        }
    }

    @Override
    public Response<Boolean> changeShopStatusById(Long shopId, Integer status, String noPassReason) {
        try {
            Map<String, String> extra = null;
            if (!Strings.isNullOrEmpty(noPassReason)) {
                Shop shop = shopDao.findById(shopId);
                extra = shop.getExtra() == null ? Maps.newHashMap() : shop.getExtra();
                extra.put("noPassReason", noPassReason);
            }
            vegaShopManager.updateShopStatus(shopId, status, extra);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to change shop status by shopId = {}, cause : {}.", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.shop.status.failed");
        }
    }

    /**
     * 修改店铺授权状态
     * @param shopId 公司ID
     * @param authorize 授权状态
     * @return 是否成功
     */
    @Override
    public Response<Boolean> changeShopAuthorizeById(Long shopId, Integer authorize) {
        try {
            VegaShopExtra shopExtra = new VegaShopExtra();
            shopExtra.setShopId(shopId);
            shopExtra.setShopAuthorize(authorize);
            Boolean result = vegaShopExtraDao.updateByShopId(shopExtra);
            if (!result) {
                log.error("failed to change shop authorize by shopId = {}, cause more than one or " +
                        "less than one row affected.", shopId);
                return Response.fail("change.shop.authorize.failed");
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to change shop authorize by shopId = {}, cause : {}.", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.shop.authorize.failed");
        }
    }

    @Override
    public Response<Boolean> changeShopMemberTypeById(Long shopId, Integer status) {
        try {
            VegaShopExtra shopExtra = new VegaShopExtra();
            shopExtra.setShopId(shopId);
            shopExtra.setIsOldMember(status);
            Boolean result = vegaShopExtraDao.updateByShopId(shopExtra);
            if (!result) {
                log.error("failed to change shop is old member by shopId = {}, cause more than one or " +
                        "less than one row affected.", shopId);
                return Response.fail("change.shop.is.old.member.failed");
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to change shop is old member by shopId = {}, cause : {}.", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.shop.is.old.member.failed");
        }
    }

    @Override
    public Response<Boolean> changePurchaseDiscount(Long shopId, Integer discount) {
        try {
            VegaShopExtra shopExtra = new VegaShopExtra();
            shopExtra.setShopId(shopId);
            shopExtra.setPurchaseDiscount(discount);
            Boolean result = vegaShopExtraDao.updateByShopId(shopExtra);
            if (!result) {
                log.error("failed to change shop purchase discount by shopId = {}, cause more than one or " +
                        "less than one row affected.", shopId);
                return Response.fail("change.shop.purchase.discount.failed");
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to change shop status by shopId = {}, cause : {}.", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.shop.purchase.discount.failed");
        }
    }

    @Override
    public Response<Boolean> changeDefaultMemberDiscount(Long shopId, String discount) {
        try {
            VegaShopExtra shopExtra = new VegaShopExtra();
            shopExtra.setShopId(shopId);
            shopExtra.setMemberDiscountJson(discount);
            Boolean result = vegaShopExtraDao.updateByShopId(shopExtra);
            if (!result) {
                log.error("failed to change shop member default discount by shopId = {}, cause more than one or " +
                        "less than one row affected.", shopId);
                return Response.fail("change.shop.member.default.discount.failed");
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to change shop status by shopId = {}, cause : {}.", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("change.shop.member.default.discount.failed");
        }
    }

    @Override
    public Response<Boolean> changeCreditStatusByShopId(Long shopId, Boolean isAvailable) {
        try {
            return Response.ok(vegaShopExtraDao.changeCreditStatusByShopId(shopId, isAvailable));
        }catch (Exception e) {
            log.error("failed to change isCreditAvailable = ({}) by shopId = {}, cause : {}",
                    isAvailable, shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("chang.credit.status.failed");
        }
    }

    @Override
    public Response<Boolean> changeDiscountLowerLimit(Long shopId, Integer discountLowerLimit) {
        try {
            return Response.ok(vegaShopExtraDao.changeDiscountLowerLimit(shopId, discountLowerLimit));
        }catch (Exception e) {
            log.error("failed to change discountLowerLimit = ({}) by shopId = {}, cause : {}",
                    discountLowerLimit, shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("chang.discount.lower.limit.failed");
        }
    }

    @Override
    public Response<Boolean> batchFrozeShopCredit(List<Long> shopIds) {
        try {
            return Response.ok(vegaShopExtraDao.batchFrozeShopCredit(shopIds));
        }catch (Exception e) {
            log.error("failed to batch froze shop credit by shopIds = [{}], cause : {}",
                    shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("froze.shop.credit.failed");
        }
    }

    @Override
    public Response<Boolean> update(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume) {
        try {
            vegaShopManager.updateShop(shop, shopExtra, resume);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("failed to update shop : ({}), shopExtra : ({}), credit alter resume : ({}), cause : {}",
                    shop, shopExtra, resume, Throwables.getStackTraceAsString(e));
            return Response.fail("update.shop.failed");
        }
    }
}
