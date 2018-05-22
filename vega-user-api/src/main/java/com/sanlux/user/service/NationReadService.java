package com.sanlux.user.service;

import com.google.common.base.Optional;
import com.sanlux.user.model.Nation;
import io.terminus.common.model.Response;

/**
 * Created by lujm on 2017/2/23.
 */
public interface NationReadService {
    /**
     * 通过父code查询七鱼客服地区对照表信息
     * @param code
     * @return
     */
    Response<Optional<Nation>> findByCode(String code);
}
