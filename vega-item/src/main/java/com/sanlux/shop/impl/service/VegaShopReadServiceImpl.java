/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.impl.service;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.impl.dao.VegaShopExtraDao;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

import static io.terminus.common.utils.Arguments.isNullOrEmpty;

/**
 * @author : panxin
 */
@Slf4j
@Service
@RpcProvider
public class VegaShopReadServiceImpl implements VegaShopReadService{

    @Autowired
    private ShopDao shopDao;
    @Autowired
    private VegaShopExtraDao vegaShopExtraDao;

    @Override
    public Response<VegaShop> findByShopId(Long shopId) {
        try {
            // 查找shop
            Shop shop = shopDao.findById(shopId);
            if (shop == null) {
                log.error("shop shopId = ({}) not found", shopId);
                return Response.fail("shop.not.exists");
            }
            // 查找shopExtra
            VegaShopExtra shopExtra = vegaShopExtraDao.findByShopId(shopId);
            if (shopExtra == null) {
                log.error("VegaShopExtra shopId = ({}) not found", shopId);
                return Response.fail("shop.extra.not.exists");
            }

            return Response.ok(new VegaShop(shop, shopExtra));
        }catch (Exception e) {
            log.error("failed to find shop by shopId = ({}), cause : {}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.by.id.failed");
        }
    }
    @Override
    public Response<List<VegaShop>> findByShopIds(List<Long> shopIds) {
        try {
            // 查找shop
            List<Shop> shops = shopDao.findByIds(shopIds);
            if (shops == null) {
                log.error("shop shopId = ({}) not found", shopIds);
                return Response.fail("shop.not.exists");
            }
            // 查找shopExtra
            List<VegaShopExtra> shopExtras = vegaShopExtraDao.findByShopIds(shopIds);
            if (shopExtras == null) {
                log.error("VegaShopExtra shopId = ({}) not found", shopIds);
                return Response.fail("shop.extra.not.exists");
            }
            if(shops.size()==shopExtras.size()) {
                List<VegaShop> vegaShops = Lists.newArrayListWithCapacity(shops.size());
                for (int i = 0; i < shops.size(); i++) {
                    Shop shop = shops.get(i);
                    VegaShopExtra shopExtra = shopExtras.get(i);
                    VegaShop vegaShop = new VegaShop(shop, shopExtra);
                    vegaShops.add(vegaShop);
                }
                return Response.ok(vegaShops);
            }else{
                log.error("get VegaShop fail, shopIds:{}", shopIds);
                return Response.fail("get.VegaShop.fail");
            }
        }catch (Exception e) {
            log.error("failed to find shop by shopId = ({}), cause : {}", shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.by.id.failed");
        }
    }

    @Override
    public Response<List<VegaShop>> findFirstDealerByShopIds(List<Long> shopIds) {
        try {
            List<VegaShopExtra> shopExtras = vegaShopExtraDao.findFirstDealerByShopIds(shopIds);
            if (shopExtras == null) {
                log.error("VegaShopExtra shopId = ({}) not found", shopIds);
                return Response.fail("shop.extra.not.exists");
            }
            List<VegaShop> vegaShops = Lists.newArrayListWithCapacity(shopExtras.size());
            for (VegaShopExtra vegaShopExtra : shopExtras) {
                Shop shop = shopDao.findById(vegaShopExtra.getShopId());
                if (shop == null) {
                    log.error("shop shopId = ({}) not found", vegaShopExtra.getShopId());
                    return Response.fail("shop.not.exists");
                }
                VegaShop vegaShop = new VegaShop(shop,vegaShopExtra);
                vegaShops.add(vegaShop);
            }
            return Response.ok(vegaShops);
        } catch (Exception e) {
            log.error("failed to find shop by shopId = ({}), cause : {}", shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.by.id.failed");
        }
    }
    @Override
    public Response<VegaShop> findByUserId(Long userId) {
        try {
            // 查找shop
            Shop shop = shopDao.findByUserId(userId);
            if (shop == null) {
                log.error("shop userId = ({}) not found", userId);
                return Response.fail("shop.not.exists");
            }
            // 查找shopExtra
            VegaShopExtra shopExtra = vegaShopExtraDao.findByShopId(shop.getId());
            if (shopExtra == null) {
                log.error("VegaShopExtra userId = ({}) not found", userId);
                return Response.fail("shop.extra.not.exists");
            }

            return Response.ok(new VegaShop(shop, shopExtra));
        }catch (Exception e) {
            log.error("failed to find shop by shopId = ({}), cause : {}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.by.userId.failed");
        }
    }

    @Override
    public Response<VegaShopExtra> findVegaShopExtraByShopId(Long shopId) {
        try {
            VegaShopExtra shopExtra =  vegaShopExtraDao.findByShopId(shopId);
            if (shopExtra == null) {
                log.error("failed to find shopExtra by shopId = {}, cause shop extra not exists", shopId);
                return Response.fail("shop.extra.not.exists");
            }
            return Response.ok(shopExtra);
        }catch (Exception e) {
            log.error("failed to find shopExtra by shopId = {}, cause : {}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.extra.find.failed");
        }
    }

    @Override
    public Response<List<VegaShopExtra>> findVegaShopExtrasByShopPid(Long shopPid) {
        try {
            List<VegaShopExtra> shopExtras =  vegaShopExtraDao.listAllShopByPid(shopPid);
            if (CollectionUtils.isEmpty(shopExtras)) {
                log.error("failed to find shopExtras by shopPid = {}, cause shop extra not exists", shopPid);
                return Response.fail("shop.extra.not.exists");
            }
            return Response.ok(shopExtras);
        }catch (Exception e) {
            log.error("failed to find shopExtras by shopPid = {}, cause : {}", shopPid, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.extra.find.failed");
        }
    }

    @Override
    public Response<VegaShopExtra> findVegaShopExtraByUserId(Long userId) {
        try {
            VegaShopExtra shopExtra =  vegaShopExtraDao.findByUserId(userId);
            if (shopExtra == null) {
                log.error("failed to find shopExtra by userId = {}, cause shop extra not exists", userId);
                return Response.fail("shop.extra.not.exists");
            }
            return Response.ok(shopExtra);
        }catch (Exception e) {
            log.error("failed to find shopExtra by userId = {}, cause : {}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.extra.find.failed");
        }
    }

    @Override
    public Response<Paging<VegaShop>> paging(VegaShopCriteria criteria) {
        try{
            VegaShop vegaShop = null;
            List<VegaShop> resultList = Lists.newArrayList();
            Paging<VegaShop> resultPaging = new Paging<>();
            // 有ID, 其他条件不管
            if (criteria.getId() != null) {
                return pagingByShopId(criteria);
            }
            if(criteria.getShopAuthorize() !=null || criteria.getIsOldMember() !=null){
                //一级经销商根据是否授权条件查询时
                criteria.setShopType(criteria.getType());
                criteria.setShopStatus(criteria.getStatus());
                criteria.setShopName(criteria.getName());

                Paging<VegaShopExtra> vegaShopExtraPaging = vegaShopExtraDao.paging(criteria.toMap());

                for (VegaShopExtra vegaShopExtra : vegaShopExtraPaging.getData()) {
                    Shop shop = shopDao.findById(vegaShopExtra.getShopId());
                    vegaShop = new VegaShop();
                    vegaShop.setShopExtra(vegaShopExtra);
                    if (shop != null) {
                        vegaShop.setShop(shop);
                    }
                    resultList.add(vegaShop);
                }
                resultPaging.setTotal(vegaShopExtraPaging.getTotal());
                resultPaging.setData(resultList);
                return Response.ok(resultPaging);
            }

            // shop 信息
            Paging<Shop> shopPaging = shopDao.paging(criteria.toMap());

            for (Shop shop : shopPaging.getData()) {
                VegaShopExtra extra = vegaShopExtraDao.findByShopId(shop.getId());
                vegaShop = new VegaShop();
                vegaShop.setShop(shop);
                if (extra != null) {
                    vegaShop.setShopExtra(extra);
                }
                resultList.add(vegaShop);
            }

            resultPaging.setTotal(shopPaging.getTotal());
            resultPaging.setData(resultList);
            return Response.ok(resultPaging);
        }catch (Exception e) {
            log.error("failed to paging shop by criteria : ({}), cause: {}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.shop.failed");
        }
    }

    @Override
    public Response<Paging<VegaShop>> pagingSecondaryShop(VegaShopCriteria criteria) {
        try{
            // 有ID, 其他条件不管
            if (criteria.getId() != null) {
                return pagingSecondaryDealerById(criteria);
            }
            // 有用户ID, 其他条件不管
            if (criteria.getUserId() != null) {
                return pagingSecondaryDealerByUserId(criteria);
            }
            // 其他条件, 店铺名称/状态查询
            return pagingSecondaryDealer(criteria);
        }catch (Exception e) {
            log.error("failed to paging shop by criteria : ({}), cause: {}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.shop.failed");
        }
    }

    @Override
    public Response<List<VegaShopExtra>> findFirstLevelShopByName(String name) {
        try {
            List<VegaShopExtra> shopExtraList = vegaShopExtraDao.listShopByNameAndType(name, VegaShopType.DEALER_FIRST.value());
            if (isNullOrEmpty(shopExtraList)) {
                log.info("find vega shop extra by name = ({}), result is empty.", name);
                return Response.ok(Collections.emptyList());
            }
            return Response.ok(shopExtraList);
        }catch (Exception e) {
            log.error("failed to find vega shop extra by name = ({}), cause : {}",
                    name, Throwables.getStackTraceAsString(e));
            return Response.fail("vegaShopExtra.find.by.name.failed");
        }
    }

    @Override
    public Response<List<VegaShopExtra>> findShopByPidAndName(Long parentShopId, String childShopName) {
        try {
            List<VegaShopExtra> shopExtraList = vegaShopExtraDao.listShopByPidAndName(parentShopId, childShopName);
            if (isNullOrEmpty(shopExtraList)) {
                log.info("find vega shop extra by pid = ({}) and name = ({}), result is empty.", parentShopId, childShopName);
                return Response.ok(Collections.emptyList());
            }
            return Response.ok(shopExtraList);
        }catch (Exception e) {
            log.error("failed to find vega shop extra by pid = ({}) and name = ({}), cause : {}",
                    parentShopId, childShopName, Throwables.getStackTraceAsString(e));
            return Response.fail("vegaShopExtra.find.by.name.failed");
        }
    }

    @Override
    public Response<List<VegaShopExtra>> findSupplierByName(String name) {
        try {
            List<VegaShopExtra> shopExtraList = vegaShopExtraDao.listShopByNameAndType(name, VegaShopType.SUPPLIER.value());
            if (isNullOrEmpty(shopExtraList)) {
                log.info("find vega shop extra by name = ({}), result is empty.", name);
                return Response.ok(Collections.emptyList());
            }
            return Response.ok(shopExtraList);
        }catch (Exception e) {
            log.error("failed to find vega shop extra by name = ({}), cause : {}",
                    name, Throwables.getStackTraceAsString(e));
            return Response.fail("vegaShopExtra.find.by.name.failed");
        }
    }


    @Override
    public Response<Boolean> checkShopStatusByShopId(Long shopId) {
        try {
            Shop shop = shopDao.findById(shopId);
            if (shop != null && shop.getStatus() == VegaShopStatus.NORMAL.value()) {
                return Response.ok(Boolean.TRUE);
            }
            return Response.ok(Boolean.FALSE);
        }catch (Exception e) {
            log.error("failed to check shop status by shopId = ({}), cause : {}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("check.shop.status.fail");
        }
    }

    @Override
    public Response<Optional<VegaShop>> finParentShopById(Long childShopId) {
        try {
            VegaShopExtra shopExtra =  vegaShopExtraDao.findByShopId(childShopId);
            if (shopExtra == null) {
                log.error("failed to find shopExtra by shopId = {}, cause shop extra not exists", childShopId);
                return Response.ok(Optional.absent());
            }

            Long pid = shopExtra.getShopPid();
            shopExtra =  vegaShopExtraDao.findByShopId(pid);
            if (shopExtra == null) {
                log.error("failed to find shopExtra by shopPid = {}, cause shop extra not exists", pid);
                return Response.ok(Optional.absent());
            }

            Shop shop = shopDao.findById(shopExtra.getShopId());
            if (shop == null) {
                log.error("failed to find shop by shopId = {}, cause shop not exists", shopExtra.getId());
                return Response.ok(Optional.absent());
            }

            VegaShop vegaShop = new VegaShop(shop, shopExtra);
            return Response.ok(Optional.of(vegaShop));
        }catch (Exception e) {
            log.error("failed to find parent shop by childId = {}, cause : {}",
                    childShopId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.parent.shop.failed");
        }
    }

    @Override
    public Response<List<Long>> findSupplierIds() {
        try {
            return Response.ok(vegaShopExtraDao.listSupplierIds());
        }catch (Exception e) {
            log.error("failed to find supplierIds by type = {}, cause: {}",
                    VegaShopType.SUPPLIER.value(), Throwables.getStackTraceAsString(e));
            return Response.fail("find.supplier.ids.failed");
        }
    }

    @Override
    public Response<Paging<Long>> pagingDealerShopIds(Integer pageSize, Integer pageNo) {
        try {
            return Response.ok(vegaShopExtraDao.pagingIds(pageNo, pageSize));
        }catch (Exception e) {
            log.error("failed to paging shopIds by type = {}, cause: {}",
                    VegaShopType.SUPPLIER.value(), Throwables.getStackTraceAsString(e));
            return Response.fail("paging.shop.ids.failed");
        }
    }

    @Override
    public Response<List<VegaShopExtra>> findSuggestionByName(String name) {
        try {
            return Response.ok(vegaShopExtraDao.suggestionByName(name));
        }catch (Exception e) {
            log.error("failed to find shop by name = {}, cause : {}", name, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.failed");
        }
    }

    /**
     * 根据店铺ID查询店铺信息
     *
     * @param criteria 查询条件
     * @return 分页信息
     */
    private Response<Paging<VegaShop>> pagingSecondaryDealerById(VegaShopCriteria criteria) {
        VegaShop vegaShop = null;
        List<VegaShop> resultList = Lists.newArrayList();
        Paging<VegaShop> resultPaging = new Paging<>();
        Shop shop = shopDao.findById(criteria.getId());
        if (shop == null) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        VegaShopExtra extra = vegaShopExtraDao.findByShopId(shop.getId());
        if (extra == null) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        // 非二级经销商或者非当前一级经销商的二级经销商则返回空数据
        if (!Objects.equal(extra.getShopType(), VegaShopType.DEALER_SECOND.value()) ||
                !Objects.equal(extra.getShopPid(), criteria.getShopPid())) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        vegaShop = new VegaShop(shop, extra);
        vegaShop.setParentPurchaseDiscount(getParentPurchaseDiscount(extra.getShopPid()));
        resultList.add(vegaShop);

        resultPaging.setTotal(1L);
        resultPaging.setData(resultList);
        return Response.ok(resultPaging);
    }

    /**
     * 根据用户ID查询店铺信息
     *
     * @param criteria 查询条件
     * @return 分页信息
     */
    private Response<Paging<VegaShop>> pagingSecondaryDealerByUserId(VegaShopCriteria criteria) {
        VegaShop vegaShop = null;
        List<VegaShop> resultList = Lists.newArrayList();
        Paging<VegaShop> resultPaging = new Paging<>();
        Shop shop = shopDao.findByUserId(criteria.getUserId());
        if (shop == null) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        VegaShopExtra extra = vegaShopExtraDao.findByShopId(shop.getId());
        if (extra == null) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        // 非二级经销商或者非当前一级经销商的二级经销商则返回空数据
        if (!Objects.equal(extra.getShopType(), VegaShopType.DEALER_SECOND.value()) ||
                !Objects.equal(extra.getShopPid(), criteria.getShopPid())) {
            return Response.ok(Paging.<VegaShop>empty());
        }
        vegaShop = new VegaShop(shop, extra);
        vegaShop.setParentPurchaseDiscount(getParentPurchaseDiscount(extra.getShopPid()));
        resultList.add(vegaShop);

        resultPaging.setTotal(1L);
        resultPaging.setData(resultList);
        return Response.ok(resultPaging);
    }

    /**
     * 根据店铺名称查询店铺信息
     *
     * @param criteria 查询条件
     * @return 分页信息
     */
    private Response<Paging<VegaShop>> pagingSecondaryDealer(VegaShopCriteria criteria) {
        VegaShop vegaShop = null;
        List<VegaShop> resultList = Lists.newArrayList();
        Paging<VegaShop> resultPaging = new Paging<>();

        Paging<VegaShopExtra> extraPaging = vegaShopExtraDao.paging(criteria.toMap());
        for (VegaShopExtra extra : extraPaging.getData()) {
            Shop shop = shopDao.findById(extra.getShopId());
            vegaShop = new VegaShop();
            vegaShop.setShop(shop);
            vegaShop.setShopExtra(extra);
            vegaShop.setParentPurchaseDiscount(getParentPurchaseDiscount(extra.getShopPid()));
            resultList.add(vegaShop);
        }

        resultPaging.setTotal(extraPaging.getTotal());
        resultPaging.setData(resultList);
        return Response.ok(resultPaging);
    }

    /**
     * 只根据ID查询
     * @param criteria 查询条件
     * @return data
     */
    private Response<Paging<VegaShop>> pagingByShopId(VegaShopCriteria criteria) {
        Paging<VegaShop> resultPaging = new Paging<>();
        List<VegaShop> resultList = Lists.newArrayList();
        Shop shop = shopDao.findById(criteria.getId());
        if (shop == null) {
            log.error("failed to find shop by criteria = {} cause shop is not exists", criteria);
            return Response.ok(Paging.empty());
        }
        // 店铺类型必须和查询条件相同
        if (criteria.getType() != null && !Objects.equal(shop.getType(), criteria.getType())) {
            log.error("failed to find shop by criteria = {} cause shop is not exists", criteria);
            return Response.ok(Paging.empty());
        }
        VegaShopExtra extra = vegaShopExtraDao.findByShopId(shop.getId());
        if (extra == null) {
            log.error("failed to find shop by criteria = {} cause shop extra is not exists", criteria);
            return Response.ok(Paging.empty());
        }
        VegaShop vegaShop = new VegaShop(shop, extra);
        resultList.add(vegaShop);

        resultPaging.setTotal(1L);
        resultPaging.setData(resultList);
        return Response.ok(resultPaging);
    }

    @Override
    public Response<Long> countSecondDealerApproval () {
        try {
            Integer shopType=VegaShopType.DEALER_SECOND.value();//二级经销商"3"
            Integer shopStatus=VegaShopStatus.WAIT.value();//待审核"0"
            return Response.ok(vegaShopExtraDao.countSecondDealerApproval(shopType,shopStatus));
        } catch (Exception e) {
            log.error("fail to count second dealer approval, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("count.second.dealer.approval.fail");
        }
    }

    /**
     * 获取上级经销商倍率
     *
     * @param pid 上级经销商ID
     * @return 倍率值
     */
    private Integer getParentPurchaseDiscount(Long pid) {
        VegaShopExtra shopExtra = vegaShopExtraDao.findByShopId(pid);
        return shopExtra.getPurchaseDiscount();
    }
}
