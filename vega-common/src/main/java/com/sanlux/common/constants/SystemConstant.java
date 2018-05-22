package com.sanlux.common.constants;

/**
 * 系统常理
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/31/16
 * Time: 4:18 PM
 */
public class SystemConstant {

    /**
     * 积分比例
     */
    public static final String INTEGRAL_SCALE = "integralScale";

    /**
     * 成长值比例
     */
    public static final String GROWTH_VALUE = "growthValue";

    /**
     * 信用额度利息
     */
    public static final String CREDIT_INTEREST = "creditInterest";

    /**
     * 信用额度短信节点
     */
    public static final String CREDIT_SMS_NODE ="creditSmsNode";

    /**
     * 交易短信节点
     */
    public static final String TRADE_SMS_NODE ="tradeSmsNode";

    /**
     * 罚息比率
     * eg: 信用额度利息 / 罚息比率
     *     CREDIT_INTEREST / FINE_RATE
     */
    public static final Integer FINE_RATE = 10000;

    /**
     * 默认时间格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 是否已发货
     */
    public static final String IS_SHIPPED = "shipped";

    /**
     * 已发货
     */
    public static final String SHIPPED = "1";

    /**
     * 已发货
     */
    public static final String NOT_SHIPPED = "0";


    /**
     * 角色名称
     */
    public static final String ROLE_NAME = "roleName";

    /**
     * 下单时sku的最终价格
     */
    public static final String ORDER_SKU_PRICE = "orderSkuPrice";

    /**
     * 下单时sku的供货价
     */
    public static final String ORDER_SKU_SELLER_PRICE = "orderSkuSellerPrice";

    /**
     * 下单时sku一级经销商成本价
     */
    public static final String ORDER_SKU_FIRST_SELLER_PRICE = "orderSkuFirstSellerPrice";

    /**
     * 下单时sku二级经销商成本价
     */
    public static final String ORDER_SKU_SECOND_SELLER_PRICE = "orderSkuSecondSellerPrice";

    /**
     * 平台店铺名称
     */
    public static final String PLATFORM_FORM_SHOP_NAME= "platformShopName";

    /**
     * 通联退款标记位 0代表退款已申请待审核
     */
    public static final String ALLINPAY_REFUND_APPLY= "0";

    /**
     * 通联退款标记位 1代表申请退款 通过
     */
    public static final String ALLINPAY_REFUND_APPLY_AGREE= "1";

    /**
     * 通联退款标记位 -1代表申请退款拒绝
     */
    public static final String ALLINPAY_REFUND_APPLY_REJECT= "-1";

    /**
     * 通联退款信息
     */
    public static final String ALLINPAY_REFUND_APPLY_MESSAGE= "allinpayRefundMessage";



    /**
     * 商家拒绝备注
     */
    public static final String SELLER_NOTE= "sellerNote";

    /**
     * 物流公司名称
     */
    public static final String EXPRESS_COMPANY_NAME= "corpCode";

    /**
     * 物流编号
     */
    public static final String EXPRESS_NO= "serialNo";



    /**
     * 附件
     */
    public static final String ANNEX_URL= "annexUrl";


    /**
     * 运营订单备注
     */
    public static final String OPERATION_NOTE= "operationNote";

    /**
     * 订单发票备注
     */
    public static final String ORDER_INVOICE_NOTE= "orderInvoiceNote";


    /**
     * 卖家订单备注
     */
    public static final String ORDER_SELLER_NOTE= "orderSellerNote";


    /**
     * 下单时订单成本总价
     */
    public static final String ORDER_SELLER_PRICE = "orderSellerPrice";

    /**
     * 物流额外补充
     */
    public static final String SHIPMENT_EXTRA_COMMENT = "comment";

    /**
     * 商店会员备注
     */
    public static final String SHOP_USER_EXTRA = "shopUserExtra";

    /**
     * 友云采订单ID标识
     */
    public static final String YOUYUNCAI_ORDER_ID = "youyuncaiOrderId";

    /**
     * 友云采订单来源标识
     */
    public static final String YOUYUNCAI_ORDER_FROM = "youyuncaiOrderFrom";


    /**
     * 友云采外部订单来源名称
     */
    public static final String YOUYUNCAI_ORDER_FROM_NAME = "友云采";

    /**
     * 非平台店铺平台退货标记字段
     */
    public static final String PLATFORM_REFUND = "platformRefund";

    /**
     * 非平台店铺平台退货标记字段值
     */
    public static final String PLATFORM_REFUND_TAG = "1";

    /**
     * 触摸屏推荐品牌
     */
    public static final String TOUCH_SCREEN_RECOMMENDED_BRANDS = "recommendedBrands";

    /**
     * 商品导入API外部ID标志字段
     */
    public static final String ITEM_IMPORT_API_OUT_ID_TAG = "outIdTag";

    /**
     * 商品导入API类目上级Id标志字段
     */
    public static final String ITEM_IMPORT_API_CATEGORY_PID_TAG = "categoryPid";

    /**
     * 商品导入API集乘网认证ID标志字段
     */
    public static final String ITEM_IMPORT_API_CLIENT_ID = "clientId";

    /**
     * 商品导入API集乘网认证密码标志字段
     */
    public static final String ITEM_IMPORT_API_CLIENT_SECRET = "clientSecret";



}
