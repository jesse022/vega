package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采发票信息
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderInvoiceInfoDto implements Serializable {
    private static final long serialVersionUID = 2991238432864167592L;

    /**
     * 发票类型 1为普通发票，2为增值税发票
     */
    private Integer invoiceType;

    /**
     * 发票抬头 
     */
    //// TODO: 2018/3/7 待定，增值税发票需要纳税人识别号注册地址、电话等信息
    private String companyName;

    /**
     * 发票内容,1为明细，3为电脑配件，19为耗材，22为办公用品；若为增值发票，则只能选1（明细）
     */
    private String invoiceContent;
}
