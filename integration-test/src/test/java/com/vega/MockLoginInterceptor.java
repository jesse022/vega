package com.vega;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2016-05-31 8:36 PM  <br>
 * Author: xiao
 */


import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class MockLoginInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ParanaUser paranaUser = new ParanaUser();
        paranaUser.setShopId(1L);
        paranaUser.setType(2);
        paranaUser.setId(1L);
        paranaUser.setName("buyer");
        UserUtil.putCurrentUser(paranaUser);
        return true;
    }
}