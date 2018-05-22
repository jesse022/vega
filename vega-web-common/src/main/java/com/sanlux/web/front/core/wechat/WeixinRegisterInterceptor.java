/*
 *
 *  * Copyright (c) 2015 杭州端点网络科技有限公司
 *
 */

package com.sanlux.web.front.core.wechat;

import io.terminus.parana.common.utils.UserOpenIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by haolin on 1/5/15
 */
@Slf4j
@Component
public class WeixinRegisterInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private WxRequestor wxRequestor;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 手机微信登陆
        String requestUri = req.getRequestURI();
        if (WxConstants.isWxClient(req) &&
                (requestUri.equals("/register") || requestUri.equals("/register-mobile"))){
            Object openId = req.getSession().getAttribute(WxConstants.OPEN_ID);
            if (openId == null){
                String redirectUrl = "http://" + req.getRemoteHost() + requestUri;
                // 未登录
                Object code = req.getParameter(WxConstants.CODE);
                if (code == null){
                    // 重定向到微信认证
                    wxRequestor.toAuthorize(redirectUrl, resp,"23");
                    return false;
                } else {
                    // 拿openId
                    Map<String, Object> mapResp = wxRequestor.getOpenId(String.valueOf(code));
                    if (mapResp.get("errcode") != null){
                        log.error("failed to get openid, cause: {}", mapResp);
                    } else {
                        String openIdStr = String.valueOf(mapResp.get(WxConstants.OPEN_ID));
                        req.getSession().setAttribute(WxConstants.OPEN_ID, openIdStr);
                        UserOpenIdUtil.putOpenId(openIdStr);
                    }
                }
            }
            else{
                UserOpenIdUtil.putOpenId(openId.toString());
            }
        }
        return true;
    }
}
