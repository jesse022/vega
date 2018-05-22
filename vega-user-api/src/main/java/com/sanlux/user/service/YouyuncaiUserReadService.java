package com.sanlux.user.service;

import io.terminus.common.model.Response;
import io.terminus.parana.user.model.UserProfile;

import java.util.List;

/**
 * Created by lujm on 2018/3/14.
 */
public interface YouyuncaiUserReadService {

    /**
     * 根据友云采企业或机构名称查询集乘网用户详情
     * @param  name 名称
     * @return 查询结果
     */
    Response<List<UserProfile>> findYouyuncaiUserByName(String name);
}
