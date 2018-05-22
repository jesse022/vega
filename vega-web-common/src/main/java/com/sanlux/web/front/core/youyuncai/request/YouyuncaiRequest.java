package com.sanlux.web.front.core.youyuncai.request;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.web.front.core.youyuncai.order.constants.YouyuncaiConstants;
import com.sanlux.web.front.core.youyuncai.token.YouyuncaiToken;
import com.sanlux.youyuncai.dto.SkuDto;
import com.sanlux.youyuncai.dto.YouyuncaiReturnStatus;
import com.sanlux.youyuncai.dto.YouyuncaiTokenReturnStatus;
import com.sanlux.youyuncai.enums.YouyuncaiApiType;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 友云采接口请求
 * Created by lujm on 2018/1/29.
 */
@Slf4j
@Component
public class YouyuncaiRequest implements ApplicationContextAware {

    private final static JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private YouyuncaiToken youyuncaiToken;

    private Map<String, String> urlParams = Maps.newTreeMap();

    public YouyuncaiTokenReturnStatus youyuncaiTokenReturnStatus;

    private static ApplicationContext applicationContext = null;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        if (YouyuncaiRequest.applicationContext == null) {
            YouyuncaiRequest.applicationContext = applicationContext;
        }
    }

    public static <T> T getBeanByClass(Class<T> requiredType) throws BeansException {
        return applicationContext.getBean(requiredType);
    }

    public YouyuncaiRequest() {

    }

    private YouyuncaiRequest(YouyuncaiToken youyuncaiToken) {
        // // TODO: 2018/1/29 生产环境根据实际修改
        youyuncaiToken.setAppcode("sanlishi");

        urlParams.put(YouyuncaiConstants.APP_CODE, youyuncaiToken.getAppcode());
        youyuncaiToken.setClientId("jcfor");
        youyuncaiToken.setClientSecret("123456");

        //获取授权信息
        VegaYouyuncaiAccesstokenCacher vegaYouyuncaiAccesstokenCacher = getBeanByClass(VegaYouyuncaiAccesstokenCacher.class);
        YouyuncaiTokenReturnStatus youyuncaiTokenReturnStatus = vegaYouyuncaiAccesstokenCacher.findByKey("tokenKey");
        Long nowTime = System.currentTimeMillis();
        if (Arguments.isNull(youyuncaiTokenReturnStatus) ||
                nowTime > youyuncaiTokenReturnStatus.getExpiretime()) {
            // 当前时间超期时间重新获取授权
            vegaYouyuncaiAccesstokenCacher.invalidByKey("tokenKey");
            youyuncaiTokenReturnStatus = vegaYouyuncaiAccesstokenCacher.findByKey("tokenKey");
        }

        youyuncaiToken.setAccesstoken(youyuncaiTokenReturnStatus.getAccesstoken());
        urlParams.put(YouyuncaiConstants.ACCESS_TOKEN, youyuncaiToken.getAccesstoken());


        this.youyuncaiToken = youyuncaiToken;
    }

    private YouyuncaiRequest(YouyuncaiToken youyuncaiToken, String appCode) {
        youyuncaiToken.setAppcode(appCode);
        this.youyuncaiToken = youyuncaiToken;
        getYouyuncaiAccesstoken();
    }

    public static YouyuncaiRequest build(YouyuncaiToken youyuncaiToken) {
        return new YouyuncaiRequest(youyuncaiToken);
    }

    public static YouyuncaiRequest buildYouyuncaiAccesstoken(YouyuncaiToken youyuncaiToken) {
        return new YouyuncaiRequest(youyuncaiToken, "sanlishi");
    }

    /**
     * 获取友云采授权token接口
     * @return
     */
    private void getYouyuncaiAccesstoken() {
        Map<String, String> tokenUrlParams = Maps.newTreeMap();
        tokenUrlParams.put(YouyuncaiConstants.APP_CODE, youyuncaiToken.getAppcode());
        tokenUrlParams.put("clientid", "3vijhbTEQJy83pe5nddKJ_");
        tokenUrlParams.put("clientsecretkey", "c4ba283e1dc54901ad26f1cdb989a0c3");

        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(tokenUrlParams);
        String url = youyuncaiToken.getAccesstokenUrl() + "?" + suffix;


        log.info("[YOU-YUN-CAI]:api url: {}", url);
        String body = HttpRequest.
                get(url).
                contentType(HttpRequest.CONTENT_TYPE_JSON).
                connectTimeout(1000000).
                readTimeout(1000000).
                body();
        log.info("[YOU-YUN-CAI]:api result: {}", body);

        this.youyuncaiTokenReturnStatus = getYouyuncaiAccesstokenResponse(body);
    }

    /**
     * 友云采API
     * @param params   请求正文参数
     * @param apiType  API类型
     * @return         返回结果
     */
    public YouyuncaiReturnStatus youyuncaiApi(Map<String, Object> params, Integer apiType) {
        String url = url(apiType);
        if (Objects.equal(apiType, YouyuncaiApiType.ORDER_CHECK_OUT.value())) {
            url = params.get(YouyuncaiConstants.CHECKOUT_REDIRECT_URL).toString();
            params.remove(YouyuncaiConstants.CHECKOUT_REDIRECT_URL);
        }

        if (Strings.isNullOrEmpty(url)) {
            return null;
        }

        log.info("[YOU-YUN-CAI]:api url: {}", url);
        String body = "";
        switch (YouyuncaiApiType.from(apiType)) {
            case CATEGORY_INIT:
            case ITEM_INIT:
            case ITEM_ADD:
            case ORDER_CHECK_OUT:
            case ORDER_DELIVERY_ORDER:
            case ORDER_SHIP_INFO:
                body = post(url, params);
                break;
            case ITEM_UPDATE:
                body = put(url, params);
                break;
            case ITEM_DELETE:
                body = delete(url, params);
                break;
        }
        log.info("[YOU-YUN-CAI]:api result: {}", body);

        return convertToResponse(body);
    }

    /**
     * 根据接口类型获取接口地址
     * @param apiType 类型
     * @return        返回地址
     */
    private String url(Integer apiType) {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(urlParams);

        switch (YouyuncaiApiType.from(apiType)) {
            case CATEGORY_INIT:
                return youyuncaiToken.getCategoryInitUrl() + "?" + suffix;
            case ITEM_INIT:
                return youyuncaiToken.getItemInitUrl() + "?" + suffix;
            case ITEM_ADD:
                return youyuncaiToken.getItemAddUrl() + "?" + suffix;
            case ITEM_UPDATE:
                return youyuncaiToken.getItemUpdateUrl() + "?" + suffix;
            case ITEM_DELETE:
                return youyuncaiToken.getItemDeleteUrl();
            case ORDER_DELIVERY_ORDER:
                return youyuncaiToken.getOrderDeliveryUrl() + youyuncaiToken.getAppcode();
            case ORDER_SHIP_INFO:
                return youyuncaiToken.getOrderShipInfoUrl() + youyuncaiToken.getAppcode();
        }

        return null;
    }

    /**
     * 向友云采发起POST请求
     * @param url     请求地址
     * @param params  请求参数,转换成json格式
     * @return        返回结果
     */
    private String post(String url,Map<String, Object> params){

        return HttpRequest.
                post(url).
                contentType(HttpRequest.CONTENT_TYPE_JSON).
                connectTimeout(1000000).
                readTimeout(1000000).
                send(JSON_MAPPER.toJson(params)).
                //form(params).
                body();
    }

    /**
     * 向友云采发起PUT请求
     * @param url     请求地址
     * @param params  请求参数,转换成json格式
     * @return        返回结果
     */
    private String put(String url,Map<String, Object> params){
        return HttpRequest.
                put(url).
                contentType(HttpRequest.CONTENT_TYPE_JSON).
                connectTimeout(1000000).
                readTimeout(1000000).
                send(JSON_MAPPER.toJson(params)).
                body();
    }

    /**
     * 向友云采发起DELETE请求
     * @param url     请求地址
     * @param params  请求参数,转换成json格式
     * @return        返回结果
     */
    private String delete(String url,Map<String, Object> params){
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(urlParams);

        Object data = Arguments.isNull(params.get("data")) ? "" : params.get("data");
        if (data instanceof List) {
            List dataList =  (List)data;
            if (dataList.get(0) instanceof SkuDto) {
                List<String> skuCodes = Lists.newArrayList();
                for (int i =0; i < dataList.size(); i++) {
                    SkuDto skuDto = (SkuDto)dataList.get(i);
                    skuCodes.add(skuDto.getSkuCode().trim());
                }
                String skuCode =  Joiner.on(",").join(skuCodes);
                url = url + skuCode;
            }

        }

        return HttpRequest.
                delete(url + "?" + suffix).
                connectTimeout(1000000).
                readTimeout(1000000).
                body();
    }

    /**
     * 友云采返回信息解析
     * @param body 处理前返回内容
     * @return     处理后返回内容
     */
    private YouyuncaiReturnStatus convertToResponse(String body) {
        try {
            if (Strings.isNullOrEmpty(body)) {
                return null;
            }
            return JSON_MAPPER.fromJson(body, YouyuncaiReturnStatus.class);
        } catch (Exception e) {
            log.error("[YOU-YUN-CAI]:fail to get you yun cai status from data={},cause:{}", body, Throwables.getStackTraceAsString(e));
            return null;
        }
    }


    /**
     * 获取友云采授权信息解析
     * @param body 接口返回信息
     */
    private YouyuncaiTokenReturnStatus getYouyuncaiAccesstokenResponse(String body) {
        try {
            if (Strings.isNullOrEmpty(body)) {
                throw new JsonResponseException(500, "获取友云采授权信息失败");
            }
            if (body.contains("status")) {
                // 获取失败
                YouyuncaiReturnStatus youyuncaiReturnStatus = JSON_MAPPER.fromJson(body, YouyuncaiReturnStatus.class);
                log.error("[YOU-YUN-CAI]:fail to get you yun cai token api, youyuncaiReturnStatus = {}", youyuncaiReturnStatus);
                throw new JsonResponseException(500, "获取友云采授权信息失败");
            }

            return JSON_MAPPER.fromJson(body, YouyuncaiTokenReturnStatus.class);
        } catch (Exception e) {
            log.error("[YOU-YUN-CAI]:fail to get you yun cai token api from body={},cause:{}", body, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "获取友云采授权信息失败");
        }
    }



}
