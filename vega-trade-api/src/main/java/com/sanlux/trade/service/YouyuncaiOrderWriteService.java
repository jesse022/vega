package com.sanlux.trade.service;

import com.sanlux.trade.model.YouyuncaiOrder;
import io.terminus.common.model.Response;


/**
 * 友云采订单写服务接口
 * Created by lujm on 2018/3/9.
 */
public interface YouyuncaiOrderWriteService {
    /**
     * 创建
     * @param youyuncaiOrder 友云采订单信息
     * @return 是否成功
     */
    Response<Boolean> create(YouyuncaiOrder youyuncaiOrder);

    /**
     * 更新
     * @param youyuncaiOrder 友云采订单信息
     * @return 是否成功
     */
    Response<Boolean> update(YouyuncaiOrder youyuncaiOrder);
}
