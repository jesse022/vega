package com.sanlux.web.front.core.youyuncai.order.constants;

/**
 * 友云采订单对接常量
 * Created by lujm on 2018/3/2.
 */
public class YouyuncaiConstants {

    /**
     * 友云采用户编号
     */
    public static final String USER_CODE = "youyuncaiUserCode";

    /**
     * 友云采用户标识
     */
    public static final String USER_TAGS = "youyuncaiUserTags";

    /**
     * 友云采buyerCookie
     */
    public static final String BUYER_COOKIE = "buyerCookie";

    /**
     * 友云采用户名称
     */
    public static final String USER_NAME = "youyuncaiUserName";

    /**
     * 友云采企业名称
     */
    public static final String GROUP_NAME = "youyuncaiGroupName";

    /**
     * 友云采机构名称
     */
    public static final String ORG_NAME = "youyuncaiOrgName";

    /**
     * 友云采checkoutRedirectUrl
     */
    public static final String CHECKOUT_REDIRECT_URL = "checkoutRedirectUrl";

    /**
     * 友云采传值固定税率
     */
    public static final String taxRate = "0.17";

    /**
     * 友云采传值固定货期(天)
     */
    public static final Integer leadTime = 0;


    /**
     * 友云采请求参数header
     */
    public static final String  HEADER = "header";


    /**
     * 友云采请求参数body
     */
    public static final String  BODY = "body";

    /**
     * 集乘网企业用户编号（唯一）
     */
    public static final String  CUST_USER_CODE = "custUserCode";

    /**
     * 集乘网企业编号（同友云采企业编号，原路返回）
     */
    public static final String  CUST_GROUP_CODE = "custGroupCode";

    /**
     * 集乘网企业机构编号（同友云采企业机构编码，原路返回）
     */
    public static final String  CUST_ORG_CODE = "custOrgCode";

    /**
     * 集乘网全场包邮默认金额(元)
     */
    public static final String FREE_SHIPPING = "500";

    /**
     * 友云采订单是否已经开票默认为0
     */
    public static final Integer HAS_INVOICED = 0;


    /**
     * 友云采接口accesstoken参数
     */
    public static final String ACCESS_TOKEN = "accesstoken";

    /**
     * 友云采接口appcode参数
     */
    public static final String APP_CODE = "appcode";

}
