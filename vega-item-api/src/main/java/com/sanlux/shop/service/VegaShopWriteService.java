package com.sanlux.shop.service;

import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;

import java.util.List;

/**
 * @author : panxin
 */
public interface VegaShopWriteService {

    /**
     * 创建店铺
     *
     * @param shop 店铺基本信息
     * @param shopExtra 店铺extra信息
     * @return 新建店铺ID
     */
    Response<Long> create(Shop shop, VegaShopExtra shopExtra);

    /**
     * 创建店铺, 同时创建信用额度履历
     * @param shop 店铺信息
     * @param shopExtra 店铺信息
     * @param resume 履历信息
     * @return ID
     */
    Response<Long> create(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume);

    /**
     * 更新店铺信息
     *
     * @param shop 店铺信息
     * @param shopExtra 店铺extra信息
     * @return 更新结果
     */
    Response<Boolean> update(Shop shop, VegaShopExtra shopExtra);

    /**
     * 更新店铺状态信息
     *
     * @param shop 店铺信息
     * @param shopExtra 店铺extra信息
     * @return 更新结果
     */
    Response<Boolean> updateShopStatus(Shop shop, VegaShopExtra shopExtra);

    /**
     * 根据店铺ID修改店铺状态
     *
     * @param shopId 公司ID
     * @param status 状态
     * @return 是否修改成功
     */
    Response<Boolean> changeShopStatusById(Long shopId, Integer status, String noPassReason);

    /**
     * 根据店铺ID修改店铺授权状态
     *
     * @param shopId 公司ID
     * @param authorize 授权状态
     * @return 是否修改成功
     */
    Response<Boolean> changeShopAuthorizeById(Long shopId, Integer authorize);

    /**
     * 根据店铺ID修改店铺是否老会员标记
     *
     * @param shopId 公司ID
     * @param status 标记值
     * @return 是否修改成功
     */
    Response<Boolean> changeShopMemberTypeById(Long shopId, Integer status);

    /**
     * 一级经销商为二级经销商设置采购折扣
     *
     * @param shopId 二级公司ID
     * @param discount 折扣
     * @return 是否设置成功
     */
    Response<Boolean> changePurchaseDiscount(Long shopId, Integer discount);

    /**
     * 设置会员默认倍率
     *
     * @param shopId 公司ID
     * @param discount 默认倍率
     * @return 是否设置成功
     */
    Response<Boolean> changeDefaultMemberDiscount(Long shopId, String discount);

    /**
     * 根据店铺ID修改信用额度状态(可用/不可用)
     * @param shopId 店铺ID
     * @param isAvailable 是否可用
     * @return 修改结果
     */
    Response<Boolean> changeCreditStatusByShopId(Long shopId, Boolean isAvailable);

    /**
     * 修改倍率下限
     * @param shopId 店铺ID
     * @param discountLowerLimit 下限值
     * @return 修改结果
     */
    Response<Boolean> changeDiscountLowerLimit(Long shopId, Integer discountLowerLimit);

    /**
     * 批量冻结店铺信用额度
     * @param shopIds 店铺ID
     * @return 操作结果
     */
    Response<Boolean> batchFrozeShopCredit(List<Long> shopIds);

    /**
     * 编辑店铺信息
     * @param shop 店铺信息
     * @param shopExtra 店铺extra
     * @param resume 履历信息
     * @return 更新结果
     */
    Response<Boolean> update(Shop shop, VegaShopExtra shopExtra, CreditAlterResume resume);
}
