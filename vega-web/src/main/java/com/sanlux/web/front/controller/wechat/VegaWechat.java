package com.sanlux.web.front.controller.wechat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.sanlux.web.front.component.wechat.SHA1Helper;
import com.sanlux.web.front.core.wechat.WxRequestor;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Copyright (c) 2015 杭州端点网络科技有限公司
 * Date:2016-01-20
 * Time:15:16
 * Author: 2015年 <a href="zhougl@terminus.io">周高磊</a>
 * Desc:
 */
@Slf4j
@Controller
@RequestMapping("/api/vega")
public class VegaWechat {
    @Autowired
    private WxRequestor wxRequestor;


    @Value("${pay.wechatpay.jsapi.token.appId}")
    private String appId;

    @RequestMapping(value = "/wechat/signature", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response sign(@RequestParam("url") String url) {
        if (url.isEmpty()) {
            log.error("fail to generateEncrypted wechat js api signature, cause url cannot be empty!");
            return Response.fail("URL cannot be empty!");
        }
        // 生成签名的url中忽略 ’#‘ 的anchor部分
        if (url.contains("#")) {
            url = url.substring(0, url.lastIndexOf("#"));
        }

        // 用微信access token 换取ticket
        Response getTicket = getTicket();
        if (!getTicket.isSuccess()) {
            return getTicket;
        }
        String ticket = (String) getTicket.getResult();
        // 随机串
        String nonce = RandomStringUtils.randomAlphanumeric(32);
        // 时间戳
        String timestamp = String.valueOf(new Date().getTime() / 1000);

        // 按字典排序
        String[] paramArray = new String[]{
                "url=" + url,
                "noncestr=" + nonce,
                "timestamp=" + timestamp,
                "jsapi_ticket=" + ticket
        };

        // 获取 signature
        String signature = SHA1Helper.getSortedSHA1("&", paramArray);

        Map<String, String> result = Maps.newHashMap();
        result.put("signature", signature);
        result.put("timestamp", timestamp);
        result.put("url", url);
        result.put("nonce", nonce);
        result.put("appId", appId);
        return Response.ok(result);
    }


    private Response getTicket() {
        String ticket = wxRequestor.doGetTicket();
        if (ticket != null) {
            return Response.ok(ticket);
        }

        Map<String, Object> getJsApiTicket = wxRequestor.getJsApiTicket();
        if (!Objects.equals(0, getJsApiTicket.get("errcode"))) {
            String errmsg = String.valueOf(getJsApiTicket.get("errmsg"));
            log.error("fail to get js api ticket, cause:{}", errmsg);
            return Response.fail(errmsg);
        }

        // 获取ticket 与过期时间
        ticket = (String) getJsApiTicket.get("ticket");
        Integer expires = (Integer) MoreObjects.firstNonNull(getJsApiTicket.get("expires_in"), 7200);
        wxRequestor.doSaveTicket(ticket, expires);
        return Response.ok(ticket);
    }
}
