package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采采购员信息
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderPurchaserDto implements Serializable {
    private static final long serialVersionUID = -8066564579820025273L;

    /**
     * 采购员名
     */
    private String name;

    /**
     * 采购员邮编
     */
    private String zip;

    /**
     * 采购员座机号
     */
    private String phone;

    /**
     * 采购员手机号
     */
    private String mobile;

    /**
     * 采购员邮箱
     */
    private String email;

}
