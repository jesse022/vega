package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.RankDao;
import com.sanlux.user.model.Rank;
import com.sanlux.user.dto.UserRank;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.impl.service.UserWriteServiceImpl;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/16/16
 * Time: 5:15 PM
 */
@Primary
@Service
@RpcProvider
@Slf4j
public class VegaUserWriteServiceImpl implements UserWriteService<User> {


    private final UserDao userDao;

    private final RankDao rankDao;

    @Autowired
    public VegaUserWriteServiceImpl(UserDao userDao,RankDao rankDao) {
        this.userDao = userDao;
        this.rankDao=rankDao;
    }

    @Override
    public Response<Long> create(User user) {
        try {
            if (StringUtils.hasText(user.getPassword())) {  //对密码加盐加密
                user.setPassword(EncryptUtil.encrypt(user.getPassword()));
            }
            //todo
            UserRank userRank = new UserRank();
            Rank rank=rankDao.findBaseRank();
            if(rank==null){
                log.error("failed to create {}, cause:{}",user,"rank.find.base.fail");
                return Response.fail("rank.find.base.fail");
            }
            else {
                userRank.setRankName(rank.getName());
                userRank.setRankId(rank.getId());
                userRank.setGrowthValue(0L);
                userRank.setUserName(user.getName());
                userRank.setUserId(user.getId());
                String userRankJson = JsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(userRank);
                user.setExtraJson(userRankJson);
                userDao.create(user);
                return Response.ok(user.getId());
            }
        } catch (DuplicateKeyException e) {
            log.error("failed to create {}, cause:{}", user, Throwables.getStackTraceAsString(e));
            return Response.fail("user.loginId.duplicate");
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", user, Throwables.getStackTraceAsString(e));
            return Response.fail("user.create.fail");
        }
    }

    public Response<Boolean> update(User user) {
        try {
            this.userDao.update(user);
            return Response.ok(Boolean.TRUE);
        } catch (Exception var3) {
            log.error("failed to update {}, cause:{}", user, Throwables.getStackTraceAsString(var3));
            return Response.fail("user.update.fail");
        }
    }

}
