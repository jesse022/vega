package com.sanlux.category.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.sanlux.category.dto.VegaCategoryAuthDto;
import com.sanlux.category.impl.cache.VegaCategoryAuthByShopIdCacher;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.common.helper.ShopTypeHelper;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.shop.impl.dao.VegaShopExtraDao;
import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.category.impl.dao.BackCategoryDao;
import io.terminus.parana.item.impl.dao.ItemDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by cuiwentao
 * on 16/8/18
 */
@Slf4j
@Service
@RpcProvider
public class VegaCategoryAuthByShopIdCacherServiceImpl implements VegaCategoryAuthByShopIdCacherService {

    private final VegaCategoryAuthByShopIdCacher vegaCategoryAuthByShopIdCacher;

    private final ItemDao itemDao;

    private final BackCategoryDao backCategoryDao;

    private final VegaShopExtraDao vegaShopExtraDao;

    @RpcConsumer
    private VegaCategoryByItemIdCacherService vegaCategoryByItemIdCacherService;



    @Autowired
    public VegaCategoryAuthByShopIdCacherServiceImpl(VegaCategoryAuthByShopIdCacher vegaCategoryAuthByShopIdCacher,
                                                     BackCategoryDao backCategoryDao,
                                                     ItemDao itemDao,
                                                     VegaShopExtraDao vegaShopExtraDao) {
        this.vegaCategoryAuthByShopIdCacher = vegaCategoryAuthByShopIdCacher;
        this.backCategoryDao = backCategoryDao;
        this.itemDao = itemDao;
        this.vegaShopExtraDao = vegaShopExtraDao;
    }

    @Override
    public Response<Optional<CategoryAuthe>> findByShopId(Long shopId) {
        try {
            CategoryAuthe categoryAuthe = vegaCategoryAuthByShopIdCacher.findByShopId(shopId);
            return Response.ok(Optional.fromNullable(categoryAuthe));
        } catch (Exception e) {
            log.error("find categoryAuth by shopId: {} failed, cause:{}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.auth.find.fail");
        }
    }


    @Override
    public Response<Optional<Long>> findShopIdForReceiveOrder(List<Long> shopIds, List<Long> itemIds) {
        try {
            //根据ItemIDs 查找到一级类目ID
            Set<Long> categoryFirstLevelIds = findFirseLevelCategoryIdsForItemIds(itemIds);

            //第一步:先匹配二级经销商
            for (Long shopId : shopIds) {
                //过滤掉非正常状态的店铺
                VegaShopExtra vegaShopExtra = vegaShopExtraDao.findByShopId(shopId);
                if (Arguments.isNull(vegaShopExtra)
                        || !Objects.equals(vegaShopExtra.getShopStatus(), VegaShopStatus.NORMAL.value())) {
                    continue;
                }
                if (ShopTypeHelper.isFirstDealerShop(vegaShopExtra.getShopType())) {
                    continue;
                }

                CategoryAuthe categoryAuthe = vegaCategoryAuthByShopIdCacher.findByShopId(vegaShopExtra.getShopPid());
                if (categoryAuthe == null) {
                    continue;
                }
                Set<Long> categoryIds = Sets.newHashSet();
                for (VegaCategoryAuthDto auth : categoryAuthe.getAuthList()) {
                    categoryIds.add(auth.getCategoryId());
                }
                if (categoryIds.containsAll(categoryFirstLevelIds)) {
                    return Response.ok(Optional.of(shopId));
                }
                shopIds.remove(shopId);//删除当前二级经销商shopId
            }

            //第二步:匹配一级经销商
            for (Long shopId : shopIds) {
                //过滤掉非正常状态的店铺
                VegaShopExtra vegaShopExtra = vegaShopExtraDao.findByShopId(shopId);
                if (Arguments.isNull(vegaShopExtra)
                        || !Objects.equals(vegaShopExtra.getShopStatus(), VegaShopStatus.NORMAL.value())) {
                    continue;
                }
                if (ShopTypeHelper.isSecondDealerShop(vegaShopExtra.getShopType())) {
                    continue;
                }
                CategoryAuthe categoryAuthe = vegaCategoryAuthByShopIdCacher.findByShopId(shopId);
                if (categoryAuthe == null) {
                    continue;
                }
                Set<Long> categoryIds = Sets.newHashSet();
                for (VegaCategoryAuthDto auth : categoryAuthe.getAuthList()) {
                    categoryIds.add(auth.getCategoryId());
                }
                if (categoryIds.containsAll(categoryFirstLevelIds)) {
                    return Response.ok(Optional.of(shopId));
                }
            }

            return Response.ok(Optional.<Long>absent());
        } catch (Exception e) {
            log.error("find receive order shopId by shopIds: {} and itemIds: {} failed, cause:{}",
                    shopIds, itemIds, Throwables.getStackTraceAsString(e));
            return Response.fail("find.shopId.for.receive.order.fail");
        }
    }

    private Set<Long> findFirseLevelCategoryIdsForItemIds (List<Long> itemIds) {
        Set<Long> categoryFirstLevelIds = Sets.newHashSet();
        for (Long itemId : itemIds) {
            Response<Long> categoryIdsResponse = vegaCategoryByItemIdCacherService.findByItemId(itemId);
            if (!categoryIdsResponse.isSuccess()) {
                log.error("find first and second level category by itemId:{} fail,cause:{}",
                        itemId, categoryIdsResponse.getError());
                throw new ServiceException(categoryIdsResponse.getError());
            }
            categoryFirstLevelIds.add(categoryIdsResponse.getResult());
        }
        return categoryFirstLevelIds;
    }


    @Override
    public Response<Optional<Long>> findShopIdForItem(List<Long> shopIds, Long itemId) {
        try {
            Long firstCategoryId = findFirstCategoryIdByItemId(itemId);

            for (Long shopId : shopIds) {

                //过滤掉非正常状态的店铺
                VegaShopExtra vegaShopExtra = vegaShopExtraDao.findByShopId(shopId);
                if (Arguments.isNull(vegaShopExtra)
                        || !Objects.equals(vegaShopExtra.getShopStatus(), VegaShopStatus.NORMAL.value())){
                    continue;
                }

                CategoryAuthe categoryAuthe = new CategoryAuthe();
                if (ShopTypeHelper.isFirstDealerShop(vegaShopExtra.getShopType())) {
                    categoryAuthe = vegaCategoryAuthByShopIdCacher.findByShopId(shopId);
                } else if (ShopTypeHelper.isSecondDealerShop(vegaShopExtra.getShopType())) {
                    categoryAuthe = vegaCategoryAuthByShopIdCacher.findByShopId(vegaShopExtra.getShopPid());
                }

                if (categoryAuthe == null) {
                    continue;
                }
                Set<Long> categoryIds = Sets.newHashSet();
                for (VegaCategoryAuthDto authDto : categoryAuthe.getAuthList()) {
                    categoryIds.add(authDto.getCategoryId());
                }
                if (categoryIds.contains(firstCategoryId)) {
                    return Response.ok(Optional.of(shopId));
                }
            }

            return Response.ok(Optional.<Long>absent());
        }catch (Exception e) {
            log.error("find shopId for item by shopIds: {} and itemId: {} failed, cause:{}",
                    shopIds, itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.shopId.for.item.fail");
        }
    }

    private Long findFirstCategoryIdByItemId(Long itemId) {
        Response<Long> categoryIdsResponse = vegaCategoryByItemIdCacherService.findByItemId(itemId);
        if (!categoryIdsResponse.isSuccess()) {
            log.error("find first and second level category by itemId:{} fail,cause:{}",
                    itemId, categoryIdsResponse.getError());
            throw new ServiceException(categoryIdsResponse.getError());
        }
        return categoryIdsResponse.getResult();
    }

    @Override
    public Response<Boolean> invalidByShopIds(List<Long> shopIds) {
        try {
            if(vegaCategoryAuthByShopIdCacher.invalidByShopIds(shopIds)) {
                return Response.ok(Boolean.TRUE);
            }
            return Response.fail("category.auth.invalid.fail");
        } catch (Exception e) {
            log.error("invalid categoryAuth by shopIds: {} failed, cause:{}",
                    shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("category.auth.invalid.fail");
        }
    }


}
