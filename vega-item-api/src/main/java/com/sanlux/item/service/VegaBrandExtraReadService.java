package com.sanlux.item.service;

import com.sanlux.item.model.VegaBrandExtra;
import io.terminus.common.model.Response;

/**
 * 品牌扩展信息读服务类
 * Created by lujm on 2018/1/22.
 */
public interface VegaBrandExtraReadService {

    /**
     * 根据品牌Id读取扩展信息表
     * @param brandId 品牌Id
     * @return 扩展信息表
     */
    Response<VegaBrandExtra> findBrandExtraByCacher(Long brandId);

    /**
     * 根据品牌Id读取扩展信息表
     * @param brandId 品牌Id
     * @return 扩展信息表
     */
    Response<VegaBrandExtra> findByBrandId(Long brandId);
}
