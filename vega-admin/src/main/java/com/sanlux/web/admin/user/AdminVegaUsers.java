package com.sanlux.web.admin.user;

import com.sanlux.user.service.UserRankReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by liangfujie on 16/10/17
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/vega/user")
public class AdminVegaUsers {

    @RpcConsumer
    private UserRankReadService userRankReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;


    @RequestMapping(value = "/integration/mobile", method = RequestMethod.GET)
    public User findIntegrationByMobile(String mobile) {

        if (Arguments.isNull(mobile)) {
            log.error("mobile is null");
            throw new JsonResponseException("integration.find.user.mobile.null");

        } else {


            Response<Boolean> existResponse = userReadService.checkExist(mobile, LoginType.MOBILE);
            if (!existResponse.isSuccess()) {
                log.error("check user exist failed ");
                throw new JsonResponseException(existResponse.getError());
            } else {
                if (existResponse.getResult().equals(Boolean.TRUE)) {
                    Response<User> response = userReadService.findBy(mobile, LoginType.MOBILE);
                    if (!response.isSuccess()) {
                        log.error("find user by mobile failed, mobile{},cause{}", mobile, response.getError());
                        throw new JsonResponseException(response.getError());

                    } else {
                        User user = response.getResult();
                        user.setPassword(null);
                        return user;
                    }
                } else {
                    log.error("user not exist , mobile",mobile);
                    return null;//前端要求,如果用户不存在不报错,返回null,特殊标注
                }
            }
        }

    }


}
