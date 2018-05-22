package com.sanlux.user.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.NationDao;
import com.sanlux.user.model.Nation;
import com.sanlux.user.service.NationReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/2/23.
 */
@Slf4j
@Service
@RpcProvider
public class NationReadServiceImpl implements NationReadService {
    private final NationDao nationDao;

    @Autowired
    public NationReadServiceImpl(NationDao nationDao) {
        this.nationDao = nationDao;
    }

    /**
     * 通过父code查询七鱼客服地区对照表信息
     * @param code
     * @return
     */
    @Override
    public Response<Optional<Nation>> findByCode(String code) {
        try {
            return Response.ok(Optional.fromNullable(nationDao.findByCode(code)));
        } catch (Exception e) {
            log.error("find nation by code failed, code:{}, cause:{}", code, Throwables.getStackTraceAsString(e));
            return Response.fail("nation.find.fail");
        }
    }

}
