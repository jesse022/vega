package com.sanlux.shop.service;

import com.google.common.base.Optional;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author : panxin
 */
public interface VegaShopReadService {

    /**
     * 通过店铺ID查找shop
     * @param shopId 店铺ID
     * @return shop信息
     */
    Response<VegaShop> findByShopId(Long shopId);

    /**
     * 通过店铺IDS查找shop
     * @param shopIds 店铺IDS
     * @return shop信息
     */
    Response<List<VegaShop>> findByShopIds(List<Long> shopIds);

    /**
     * 通过店铺IDS查找一级经销商店铺
     * @param shopIds 店铺IDS
     * @return shop信息
     */
    Response<List<VegaShop>> findFirstDealerByShopIds(List<Long> shopIds);

    /**
     * 通过用户ID查找shop
     * @param userId 用户ID
     * @return shop信息
     */
    Response<VegaShop> findByUserId(Long userId);

    /**
     * 通过店铺ID查找shopExtra
     * @param shopId 店铺ID
     * @return shopExtra信息
     */
    Response<VegaShopExtra> findVegaShopExtraByShopId(Long shopId);

    /**
     * 根据店铺PID获取所有下级经销商
     * @param shopPid 上级店铺ID
     * @return List<VegaShopExtra>
     */
    Response<List<VegaShopExtra>> findVegaShopExtrasByShopPid(Long shopPid);

    /**
     * 通过用户ID查找shopExtra
     * @param userId 用户ID
     * @return shopExtra信息
     */
    Response<VegaShopExtra> findVegaShopExtraByUserId(Long userId);

    /**
     * 所有店铺信息分页
     *
     * @param criteria 查询条件
     * @return 分页信息
     */
    Response<Paging<VegaShop>> paging(VegaShopCriteria criteria);

    /**
     * 一级经销商查看二级经销商店铺分页
     *
     * @param criteria 查询条件
     * @return 分页信息
     */
    Response<Paging<VegaShop>> pagingSecondaryShop(VegaShopCriteria criteria);

    /**
     * 根据名字模糊查询一级经销商信息
     *
     * @param name 公司名称
     * @return 信息列表
     */
    Response<List<VegaShopExtra>> findFirstLevelShopByName(String name);

    /**
     * 根据一级经销商店铺ID查询一级二级二级店铺信息
     *
     * @param childShopName 下级经销商店铺名称
     * @return data
     */
    Response<List<VegaShopExtra>> findShopByPidAndName(Long parentShopId, String childShopName);

    /**
     * 查找供应商
     * @param name 供应商名
     * @return 供应商信息
     */
    Response<List<VegaShopExtra>> findSupplierByName(String name);

    /**
     * 检查店铺状态是否正常
     * @param shopId 店铺ID
     * @return Boolean
     */
    Response<Boolean> checkShopStatusByShopId(Long shopId);

    /**
     * 查找上级店铺ID
     * @param childShopId 店铺ID
     * @return 上级店铺信息
     */
    Response<Optional<VegaShop>> finParentShopById(Long childShopId);

    /**
     * 查找供应商店铺IDs
     * @return 店铺IDs
     */
    Response<List<Long>> findSupplierIds();

    /**
     * 查找经销商店铺IDs
     * @return 店铺IDs
     */
    Response<Paging<Long>> pagingDealerShopIds(Integer pageSize, Integer pageNo);

    /**
     * 所有店铺 suggestion
     * @param name 店铺名
     * @return 信息
     */
    Response<List<VegaShopExtra>> findSuggestionByName(String name);

    /**
     * 获取待审核二级经销商数量
     * @return Long
     */
    Response<Long> countSecondDealerApproval ();
}
