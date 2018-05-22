package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.userExtDao;
import com.sanlux.user.service.UserExtWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/3/13.
 */
@Slf4j
@Service
@RpcProvider
public class UserExtWriteServiceImpl implements UserExtWriteService {

    private final userExtDao userExtDao;
    @Autowired
    public UserExtWriteServiceImpl(userExtDao userExtDao) {
        this.userExtDao = userExtDao;
    }

    @Override
    public Response<Boolean> update(User user) {
        try {
            this.userExtDao.updateUserRoles(user);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to update {}, cause:{}", user,Throwables.getStackTraceAsString(e));
            return Response.fail("user.update.fail");
        }
    }
}
