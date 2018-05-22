package com.sanlux.trade.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by lujm on 2018/3/9.
 */
@Data
public class YouyuncaiOrder implements Serializable {

    private static final long serialVersionUID = -6040228275287165233L;

    private Long Id;

    /**
     * 集乘网订单号
     */
    private Long orderId;

    /**
     * 买家用户Id
     */
    private Long userId;

    /**
     * 友云采订单号
     */
    private String orderCode;

    /**
     * 友云采下单时间
     */
    private String approvedTime;

    /**
     * 友云采运费
     */
    private String freight;

    /**
     * 友云采订单总额(含税费+运费)
     */
    private String totalAmount;

    /**
     * 友云采开票类型 0为不开票，1为随货开票，2为集中开票
     */
    private String invoiceState;

    /**
     * 友云采付款方式 0为在线支付，1为货到付款，2为赊销，3为供应商代收
     */
    private String payment;

    /**
     * 友云采是否验证订单价格 0为不验证，1为验证，不一致则下单失败
     */
    private String orderPriceMode;

    /**
     * 是否已开具过发票 0:未开 1：部分已开 2：全部已开
     */
    private Integer hasInvoiced;

    /**
     * 友云采采购员信息
     */
    private String purchaserJson;

    /**
     * 友云采收货人信息
     */
    private String consigneeJson;

    /**
     * 友云采收货地址信息
     */
    private String deliverAddressJson;

    /**
     * 友云采发票信息
     */
    private String invoiceInfoJson;

    /**
     * 友云采收票人信息
     */
    private String invoiceReceiverJson;

    /**
     * 友云采收票人地址信息
     */
    private String invoiceAddressJson;

    /**
     * 友云采SKU详情
     */
    private String orderDetailJson;

    /**
     * 其他扩展信息
     */
    private String extraJson;

    private Date createdAt;

    private Date updatedAt;

}
