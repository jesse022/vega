/*
 *
 *  * Copyright (c) 2015 杭州端点网络科技有限公司
 *
 */

package com.sanlux.web.front.core.wechat;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by haolin on 1/6/15.
 */
@Slf4j
@SuppressWarnings("all")
@Component
public class WxRequestor {
    private String LIMITED_SCENE_ID_TEMPLATE = "{\"action_name\": \"QR_LIMIT_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": %1$s}}}";
    private String SCENE_ID_TEMPLATE = "{\"action_name\": \"QR_SCENE\", \"expire_seconds\": 604800, \"action_info\": {\"scene\": {\"scene_id\": %1$s}}}";
    private String SCENE_STR_TEMPLATE = "{\"action_name\": \"QR_LIMIT_STR_SCENE\", \"action_info\": {\"scene\": {\"scene_str\": \"%1$s\"}}}";

    private static final String authorizeUrl = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String getOpenIdUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String getTokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
    private static final String getUserInfoUrl = "https://api.weixin.qq.com/cgi-bin/user/info";

    private final String getJsApiTicket = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi";

    // 获取带参数的二维码
    private final String getLimitedQR = "https://api.weixin.qq.com/cgi-bin/qrcode/create";

    @Autowired
    private JedisTemplate jedisTemplate;

    @Value("${pay.wechatpay.jsapi.token.appId}")
    private String appId;

    @Value("${pay.wechatpay.jsapi.token.secret}")
    private String secret;

    private static final JsonMapper jsonMapper = JsonMapper.JSON_NON_DEFAULT_MAPPER;

    /**
     * 发送微信GET请求(默认不带上access_token)
     * @param url 请求url
     * @return map结果集
     */
    public Map<String, Object> get(String url){
        return get(url, Boolean.FALSE);
    }

    /**
     * 发送微信GET请求
     * @param url 请求url
     * @param withAccessToken 是否要带上access_token
     * @return map结果集
     */
    public Map<String, Object> get(String url, boolean withAccessToken){
        Map<String, Object> mapResp = doGet(url, withAccessToken);
        log.info("[weixin]: do get request({}), and get response({}).", url, mapResp);
        Object errcode = mapResp.get("errcode");
        if(Objects.equal("0", errcode)){ //0为请求成功
            errcode=null;
        }
        if (errcode != null && withAccessToken){
            if (Objects.equal("40003", errcode)){
                // token失效
                doGetAccessToken();
                return doGet(url, withAccessToken);
            }
            log.warn("[weixin]: failed to do wx request({}), cause: {}", url, mapResp);
        }
        return mapResp;
    }

    private Map<String, Object> doGet(String url, boolean withAccessToken){
        String requestUrl = withAccessToken ? addAccessToken(url) : url;
        String jsonResp = HttpRequest.get(requestUrl).body();
        Map<String, Object> mapResp = jsonMapper.fromJson(jsonResp, Map.class);
        return mapResp;
    }


    /**
     * 发送微信POST请求
     * @param url 请求url
     * @param withAccessToken 发送请求是是否带上access_token
     * @return map结果集
     */
    public Map<String, Object> post(String url, boolean withAccessToken) {
        return post(url, null, withAccessToken);
    }

    /**
     * 发送微信POST请求
     *
     * @param url             请求url
     * @param data            请求的 request body
     * @param withAccessToken 发送请求是是否带上access_token
     * @return map结果集
     */
    public Map<String, Object> post(String url, String data, boolean withAccessToken) {
        Map<String, Object> mapResp = doPost(url, data, withAccessToken);
        Object errcode = mapResp.get("errcode");
        if (errcode != null && !"0".equals(errcode.toString()) && withAccessToken) {
            if (Objects.equal("41001", String.valueOf(errcode)) || Objects.equal("42001", String.valueOf(errcode))) {
                // 41001 缺少access_token参数，42001 token过期
                doGetAccessToken();
                return doPost(url, data, withAccessToken);
            }
            log.error("[weixin]: failed to do wx request({}), cause: {}", url, mapResp);
        }
        return mapResp;
    }

    /**
     * 发送微信POST请求
     * @param url 请求url
     * @param data
     *@param withAccessToken 发送请求是是否带上access_token  @return map结果集
     */
    private Map<String, Object> doPost(String url, String data, boolean withAccessToken) {
        String requestUrl = withAccessToken ? addAccessToken(url) : url;
        HttpRequest request = HttpRequest.post(requestUrl);
        if (!Strings.isNullOrEmpty(data)) {
            request.send(data);
        }

        String jsonResp = request.body();
        Map<String, Object> mapResp = jsonMapper.fromJson(jsonResp, Map.class);
        log.info("[weixin]: do post request({}), and get response({}).", url, mapResp);
        return mapResp;
    }

    /**
     * 链接中加上access_token
     * @param url 请求url
     * @return 添加access_token后的url
     */
    private String addAccessToken(String url) {
        StringBuilder requestUrl = new StringBuilder(url);
        if (requestUrl.indexOf("?") != -1){
            requestUrl.append("&access_token=").append(getAccessToken());
        } else {
            requestUrl.append("?access_token=").append(getAccessToken());
        }
        return requestUrl.toString();
    }

    /**
     * 获取访问token
     * @return access_token
     */
    private String getAccessToken() {
        String token = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyOfAccessToken(appId));
            }
        });
        if (Strings.isNullOrEmpty(token)){
            token = doGetAccessToken();
        }
        return token;
    }

    /**
     * 获取access_token
     * @return access_token
     */
    private String doGetAccessToken() {
        StringBuilder requestUrl = new StringBuilder(getTokenUrl);
        requestUrl.append("&appid=").append(appId)
                .append("&secret=").append(secret);
        Map<String, Object> mapResp = doGet(requestUrl.toString(), Boolean.FALSE);
        if (mapResp.get("errcode") != null){
            log.error("failed to get wx access token, cause: {}", mapResp);
        } else {
            String token = String.valueOf(mapResp.get("access_token"));
            Integer expires;
            try {
                expires = Integer.valueOf(String.valueOf(mapResp.get("expires_in")));
            } catch (NumberFormatException e){
                expires = 3600;
            }
            doSaveToken(token, expires);
            return token;
        }
        return null;
    }

    private void doSaveToken(final String token, final Integer expires) {
        if (!Strings.isNullOrEmpty(token)){
            jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
                @Override
                public void action(Jedis jedis) {
                    String key = keyOfAccessToken(appId);
                    jedis.setex(key, expires, token);
                }
            });
        }
    }

    private String keyOfAccessToken(String appId) {
        return "wxac:"+appId;
    }

    /**
     * 重定向微信认证页面
     */
    public String toAuthorize(String redirectUrl, HttpServletResponse response,String param) throws IOException {
        return toAuthorize(redirectUrl, param, response);
    }

    /**
     * 重定向微信认证页面
     */
    public String toAuthorize(String redirectUrl, String state, HttpServletResponse response) throws IOException {
        String encodedUrl = URLEncoder.encode(redirectUrl, "utf-8");
        StringBuilder redirect = new StringBuilder(authorizeUrl);
        redirect.append("?appid=").append(appId)
                .append("&redirect_uri=").append(encodedUrl)
                .append("&response_type=code&scope=snsapi_base")
                .append("&state=").append(state)
                .append("#wechat_redirect");
        log.info("[weixin]: go to authorize page : {}.", redirect);
        return redirect.toString();
    }

    /**
     * 获取微信用户的openid
     * @param code 认证后的code值
     * @return 获取成功openid存放在map中, 反之errcode为错误码
     */
    public Map<String, Object> getOpenId(String code){
        StringBuilder url = new StringBuilder(getOpenIdUrl);
        url.append("?appid=").append(appId)
                .append("&secret=").append(secret)
                .append("&code=").append(code)
                .append("&grant_type=authorization_code");
        return get(url.toString());
    }

    /**
     * 获取微信用户信息, 若用户未关注公众号, 将获取不到用户信息
     * @param openId 用户openid
     * @return 微信用户信息, 若用户未关注公众号, 将获取不到用户信息
     */
    public Map<String, Object> getUserInfo(String openId){
        return doPost(getUserInfoUrl + "?openid=" + openId, null, Boolean.TRUE);
    }

    public String getPublicName(){
        return "电商平台";//todo 待确定
    }


    /**
     * 获取带参数的永久二维码, id 必须为 1 ~ 10万
     *
     * @param id 永久二维码 scene id
     * @return 微信返回结果
     */
    public Map<String, Object> getLimitedQR(Long sceneId) throws IllegalArgumentException {
        if (sceneId == null || sceneId < 1 || sceneId > 100_000) {
            log.error("[weixin] scence id must present and between 1 and 100_000, but instead got {}" + sceneId);
            throw new IllegalArgumentException("scence id must present and between 1 and 100_000, but now got " + sceneId);
        }
        return post(getLimitedQR, String.format(LIMITED_SCENE_ID_TEMPLATE, sceneId), Boolean.TRUE);
    }

    /**
     * 获取带参数的永久二维码，str 长度不得超过 64
     *
     * @param sceneStr 永久二维码的 scene string
     * @return 微信返回结果
     */
    public Map<String, Object> getLimitedQR(String sceneStr) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(sceneStr) || sceneStr.length() > 64) {
            log.error("[weixin] scene string must present and size shouldn't grater then 64, but instead got {}", sceneStr);
            throw new IllegalArgumentException("scene string must present and size shouldn't grater then 64, but instead got " + sceneStr);
        }
        return post(getLimitedQR, String.format(SCENE_STR_TEMPLATE, sceneStr), Boolean.TRUE);
    }

    /**
     * @param sceneId
     * @return
     * @throws IllegalArgumentException
     */
    public Map<String, Object> getTempraryQr(String sceneId) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(sceneId) || !Arguments.isDecimal(sceneId)) {
            log.error("[weixin] scene must be numberic, but instead got {}", sceneId);
            throw new IllegalArgumentException("scene must be numberic, but instead got " + sceneId);
        }
        return post(getLimitedQR, String.format(SCENE_ID_TEMPLATE, sceneId), Boolean.TRUE);
    }

    /**
     * 获取分享用的js sdk，
     *
     * @return
     */
    public Map<String, Object> getJsApiTicket() {
        return get(getJsApiTicket, Boolean.TRUE);
    }

    /**
     * 获取微信JS API调用的的 ticket
     *
     * @return 数据库中缓存的ticket
     */
    public String doGetTicket() {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyOfJSTicket(appId));
            }
        });
    }

    /**
     * 保存JS API调用的 ticket 到数据库
     *
     * @param ticket  调用API的ticket
     * @param expires 过期时间
     */
    public void doSaveTicket(final String ticket, final Integer expires) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(keyOfJSTicket(appId), expires, ticket);
            }
        });
    }

    private final String keyOfJSTicket(String appId) {
        return "wx:js-token:" + appId;
    }
}