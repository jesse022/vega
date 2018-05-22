package com.sanlux.shop.service;

import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import io.terminus.common.model.Response;

/**
 * 经销商基础信息扩展表写服务类
 * Created by lujm on 2017/12/19.
 */
public interface VegaShopExtraBasicInfoWriteService {
    /**
     * 创建店铺基础信息扩展表
     *
     * @param vegaShopExtraBasicInfo 店铺基础信息扩展表信息
     * @return 主键Id
     */
    Response<Long> create(VegaShopExtraBasicInfo vegaShopExtraBasicInfo);

    /**
     * 更新店铺基础信息扩展表信息
     *
     * @param vegaShopExtraBasicInfo 店铺基础信息扩展表信息
     * @return 主键Id
     */
    Response<Long> updateByShopId(VegaShopExtraBasicInfo vegaShopExtraBasicInfo);
}
