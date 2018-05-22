package com.sanlux.item.service;

import com.sanlux.item.model.TaskJob;
import io.terminus.common.model.Response;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
public interface VegaTaskJobReadService {

    Response<TaskJob> findByKey (String key);
}
