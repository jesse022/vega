package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采发票收票人信息Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderInvoiceReceiverDto implements Serializable {

    private static final long serialVersionUID = 8076215769735002145L;

    /**
     * 收票人组织名称
     */
    private String orgName;

    /**
     * 收票人部门名称
     */
    private String depName;

    /**
     * 收票人名
     */
    private String name;

    /**
     * 收票人邮编
     */
    private String zip;

    /**
     * 收票人座机号
     */
    private String phone;

    /**
     * 收票人手机号
     */
    private String mobile;

    /**
     * 收票人邮箱
     */
    private String email;
}
