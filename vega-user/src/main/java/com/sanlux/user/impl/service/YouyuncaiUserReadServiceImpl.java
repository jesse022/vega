package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.UserProfileExtDao;
import com.sanlux.user.service.YouyuncaiUserReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.user.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lujm on 2018/3/14.
 */
@Slf4j
@Service
@RpcProvider
public class YouyuncaiUserReadServiceImpl implements YouyuncaiUserReadService {

    private final UserProfileExtDao userProfileExtDao;

    @Autowired
    public YouyuncaiUserReadServiceImpl(UserProfileExtDao userProfileExtDao) {
        this.userProfileExtDao = userProfileExtDao;
    }

    public Response<List<UserProfile>> findYouyuncaiUserByName(String name) {
        try {
            return Response.ok(userProfileExtDao.findByExtraJson(name));
        } catch (Exception e) {
            log.error("find user profile by extraJson failed, name:{}, cause:{}", name, Throwables.getStackTraceAsString(e));
            return Response.fail("user.profile.find.fail");
        }
    }
}
