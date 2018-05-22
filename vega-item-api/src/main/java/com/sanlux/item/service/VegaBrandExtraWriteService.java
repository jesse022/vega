package com.sanlux.item.service;

import com.sanlux.item.model.VegaBrandExtra;
import io.terminus.common.model.Response;

/**
 * 品牌扩展信息写服务类
 * Created by lujm on 2018/1/22.
 */
public interface VegaBrandExtraWriteService {
    /**
     * 创建品牌扩展信息
     * @param vegaBrandExtra 品牌扩展信息
     * @return 主键id
     */
    Response<Long> create(VegaBrandExtra vegaBrandExtra);

    /**
     * 根据品牌Id更新品牌扩展信息
     * @param brandId 品牌Id
     * @param detail  品牌详情
     * @return 是否成功
     */
    Response<Boolean> updateByBrandId(Long brandId, String detail);

    /**
     * 根据品牌Id删除缓存
     * @param brandId 品牌Id
     * @return 是否成功
     */
    Response<Boolean> invalidByBranId(Long brandId);

}
