package com.sanlux.user.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.user.impl.dao.ShopUserExtrasDao;
import com.sanlux.user.model.ShopUserExtras;
import com.sanlux.user.service.ShopUserExtrasReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jesselu on 2017/9/4.
 */
@Slf4j
@Service
@RpcProvider
public class ShopUserExtrasReadServiceImpl implements ShopUserExtrasReadService {
    private final ShopUserExtrasDao shopUserExtrasDao;

    @Autowired
    public ShopUserExtrasReadServiceImpl(ShopUserExtrasDao shopUserExtrasDao) {
        this.shopUserExtrasDao = shopUserExtrasDao;
    }

    @Override
    public Response<Optional<ShopUserExtras>> findByUserId(Long userId) {
        try {
            ShopUserExtras shopUserExtras = shopUserExtrasDao.findByUserId(userId);
            return Response.ok(Optional.fromNullable(shopUserExtras));
        } catch (Exception e) {
            log.error("fail to find shop user extra by userId:{},cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.user.find.fail");
        }
    }
}
