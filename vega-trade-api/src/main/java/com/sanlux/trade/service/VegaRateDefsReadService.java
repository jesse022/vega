package com.sanlux.trade.service;

import com.sanlux.trade.model.VegaRateDefs;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * 三力士相关费率定义读服务
 *
 * Created by lujm on 2017/11/16
 */

public interface VegaRateDefsReadService {

    /**
     * 根据Id查询费率定义信息详情
     * @param id Id
     * @return 费率定义详情
     */
    Response<VegaRateDefs> findById(Long id);

    /**
     * 根据类型查询费率定义信息
     * @param type 类型
     * @return 费率定义详情
     */
    Response<List<VegaRateDefs>> findByType(Integer type);

    /**
     * 根据类型查询费率定义信息
     * @param type 类型
     * @param name 名称
     * @return 费率定义详情
     */
    Response<VegaRateDefs> findByTypeAndName(Integer type, String name);

    /**
     * 分页查询费率定义信息
     * @return 费率定义详情
     */
    Response<Paging<VegaRateDefs>> paging(Integer pageNo, Integer pageSize);
}
