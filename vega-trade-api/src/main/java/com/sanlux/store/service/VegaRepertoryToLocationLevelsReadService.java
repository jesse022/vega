package com.sanlux.store.service;

import io.terminus.common.model.Response;
import io.terminus.parana.store.model.RepertoryToLocationLevel;

import java.util.List;

/**
 * Created by lujm on 2017/3/6.
 */
public interface VegaRepertoryToLocationLevelsReadService {
    Response<List<RepertoryToLocationLevel>> findChildrenByIds(Long pid, Long repertoryId,List<Long> ids);
}
