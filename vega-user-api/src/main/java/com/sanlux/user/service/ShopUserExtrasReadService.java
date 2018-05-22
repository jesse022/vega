package com.sanlux.user.service;

import com.google.common.base.Optional;
import com.sanlux.user.model.ShopUserExtras;
import io.terminus.common.model.Response;


/**
 * 经销商专属会员扩展信息表读服务
 *
 * Created by lujm on 2017/9/1.
 */

public interface ShopUserExtrasReadService {

    /**
     * 根据用户ID获取经销商会员扩展信息
     * @param userId 用户ID
     * @return 专属会员扩展信息
     */
    Response<Optional<ShopUserExtras>> findByUserId(Long userId);

}
