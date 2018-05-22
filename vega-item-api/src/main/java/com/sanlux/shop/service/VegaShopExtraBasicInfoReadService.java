package com.sanlux.shop.service;

import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import io.terminus.common.model.Response;

/**
 * 经销商基础信息扩展表读服务类
 * Created by lujm on 2017/12/19.
 */
public interface VegaShopExtraBasicInfoReadService {
    /**
     * 通过店铺ID查找经销商基础信息扩展表
     * @param shopId 店铺ID
     * @return VegaShopExtraBasicInfo
     */
    Response<VegaShopExtraBasicInfo> findByShopId(Long shopId);

    /**
     * 通过店铺ID查找经销商基础信息扩展表
     * @param macAddress 店铺客户端唯一地址
     * @return VegaShopExtraBasicInfo
     */
    Response<VegaShopExtraBasicInfo> findByMacAddress(String macAddress);
}
