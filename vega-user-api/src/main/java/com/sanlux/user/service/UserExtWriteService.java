package com.sanlux.user.service;

import io.terminus.common.model.Response;
import io.terminus.parana.user.model.User;

/**
 * Created by lujm on 2017/3/13.
 */
public interface UserExtWriteService {
    /**
     * 更新用户表信息
     * @param user 用户表信息
     * @return Boolean
     */
    Response<Boolean> update(User user);
}
