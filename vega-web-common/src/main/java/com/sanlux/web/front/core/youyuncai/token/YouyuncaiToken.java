package com.sanlux.web.front.core.youyuncai.token;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采接口token
 * Created by lujm on 2018/1/29.
 */
@Data
public class YouyuncaiToken implements Serializable {

    private static final long serialVersionUID = 761903761781513537L;

    /**
     * 友云采客户端授权token
     */
    private String accesstoken;

    /**
     * 友云采系统标识
     */
    private String appcode;

    /**
     * 商品类目初始化接口地址
     */
    private String categoryInitUrl;

    /**
     * 商品初始化接口地址
     */
    private String itemInitUrl;

    /**
     * 商品增量接口地址
     */
    private String itemAddUrl;

    /**
     * 商品修改接口地址
     */
    private String itemUpdateUrl;

    /**
     * 商品删除接口地址
     */
    private String itemDeleteUrl;

    /**
     * 订单交期确认
     */
    private String orderDeliveryUrl;

    /**
     * 订单出货通知接口
     */
    private String orderShipInfoUrl;

    /**
     * 友云采获取授权接口url
     */
    private String  accesstokenUrl;

    /**
     * 友云采登录认证ID(订单对接)
     */
    private String clientId;

    /**
     * 友云采登录认证密码(订单对接)
     */
    private String clientSecret;


    public YouyuncaiToken() {
        String gatewayHost = "yc.yonyou.com";
        String categoryInitUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/ec/productclass/add";
        String itemInitUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/ec/product/add";
        String itemAddUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/ec/product";
        String itemUpdateUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/ec/product";
        String itemDeleteUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/ec/product/";
        String orderDeliveryUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/punchout/deliveryOrder/";
        String orderShipInfoUrl = "http://" + gatewayHost + "/gateway/openapi/ublinker/v1/punchout/shipInfo/";
        String accesstokenUrl = "http://" + gatewayHost + "/gateway/openapi/oauth/v1/getaccesstoken";


        setCategoryInitUrl(categoryInitUrl);
        setItemInitUrl(itemInitUrl);
        setItemAddUrl(itemAddUrl);
        setItemUpdateUrl(itemUpdateUrl);
        setItemDeleteUrl(itemDeleteUrl);
        setOrderDeliveryUrl(orderDeliveryUrl);
        setOrderShipInfoUrl(orderShipInfoUrl);
        setAccesstokenUrl(accesstokenUrl);
        //// TODO: 2018/1/29
    }

}
