package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采收货人信息Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderConsigneeDto implements Serializable {
    private static final long serialVersionUID = -4652962691889322944L;

    /**
     * 收货人组织名称
     */
    private String orgName;

    /**
     * 收货人部门名称
     */
    private String depName;

    /**
     * 收货人名
     */
    private String name;

    /**
     * 收货人邮编
     */
    private String zip;

    /**
     * 收货人座机号
     */
    private String phone;

    /**
     * 收货人手机号
     */
    private String mobile;

    /**
     * 收货人邮箱
     */
    private String email;

}
