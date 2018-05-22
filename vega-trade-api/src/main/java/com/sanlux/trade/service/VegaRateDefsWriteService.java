package com.sanlux.trade.service;

import io.terminus.common.model.Response;

/**
 * 三力士相关费率定义写服务
 *
 * Created by lujm on 2017/11/16
 */

public interface VegaRateDefsWriteService {

    /**
     * 更新费率信息
     * @param id 主键Id
     * @param rateKey 费率值
     * @return 是否成功
     */
    Response<Boolean> updateRateKey(Long id, Long rateKey);

}