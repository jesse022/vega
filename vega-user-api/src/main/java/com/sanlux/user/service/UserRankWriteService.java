package com.sanlux.user.service;

import com.sanlux.user.dto.UserRank;
import com.sanlux.user.model.Rank;
import io.terminus.common.model.Response;

/**
 * Code generated by terminus code gen
 * Desc: 用户等级表写服务
 * Date: 2016-08-03
 */

public interface UserRankWriteService {


    /**
     * 经销商修改用户等级积分信息
     * @param userId 用户id
     * @param rankId 等级id
     * @param operateId 操作人id
     * @param operateName 操作人用户名
     * @return 是否操作成功
     */
    Response<Boolean> updateUserRank(Long userId, Long rankId, Long operateId, String operateName);

    /**
     * 用户消费 更新等级信息及积分信息
     * @param userRank 用户等级信息及积分信息
     * @return 是否操作成功
     */
    Response<Boolean> updateUserRank(UserRank userRank);

    Response<Boolean> initUserRank(Long userId);




}