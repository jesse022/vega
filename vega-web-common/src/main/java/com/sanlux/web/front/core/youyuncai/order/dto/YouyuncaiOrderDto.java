package com.sanlux.web.front.core.youyuncai.order.dto;

import com.sanlux.trade.dto.YouyuncaiOrderAdressDto;
import com.sanlux.trade.dto.YouyuncaiOrderConsigneeDto;
import com.sanlux.trade.dto.YouyuncaiOrderInvoiceInfoDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 友云采下单Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderDto implements Serializable {

    private static final long serialVersionUID = -6153432222203954867L;

    /**
     * 友云采订单号
     */
    private String orderCode;

    /**
     * 集乘网企业用户号
     */
    private String custUserCode;

    /**
     * 集乘网企业编码
     */
    private String custGroupCode;

    /**
     * 集乘网企业机构编码
     */
    private String custOrgCode;

    /**
     * 集乘网企业机构名称
     */
    private String custOrgName;

    /**
     * 友云采下单时间
     */
    private String approvedTime;

    /**
     * 友云采无税金额
     */
    private String nakedAmount;

    /**
     * 友云采含税金额
     */
    private String amount;

    /**
     * 友云采税额
     */
    private String taxAmount;

    /**
     * 友云采运费
     */
    private String freight;

    /**
     * 友云采总额（含税金额+运费）
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
     * 友云采采购员信息
     */
    private YouyuncaiOrderPurchaserDto purchaser;

    /**
     * 友云采收货人信息
     */
    private YouyuncaiOrderConsigneeDto consignee;

    /**
     * 友云采收货地址信息
     */
    private YouyuncaiOrderAdressDto deliverAddress;

    /**
     * 友云采发票信息
     */
    private YouyuncaiOrderInvoiceInfoDto invoiceInfo;

    /**
     * 友云采发票接收人信息
     */
    private YouyuncaiOrderInvoiceReceiverDto invoiceReceiver;

    /**
     * 友云采发票收货地址信息
     */
    private YouyuncaiOrderAdressDto invoiceAddress;


    /**
     * 友云采订单sku详情
     */
    private List<YouyuncaiOrderDetailDto> orderDetail;

}
