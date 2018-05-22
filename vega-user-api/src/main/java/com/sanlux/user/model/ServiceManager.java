package com.sanlux.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务经理信息表Model类
 *
 * Created by lujm on 2017/5/23.
 */
@Data
public class ServiceManager implements Serializable {

    private static final long serialVersionUID = 3390358059593183698L;

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名
     */
    private String shopName;

    /**
     * 用户手机号
     *
     */
    private String mobile;

    /**
     * 业务经理姓名
     */
    private String name;

    /**
     * 业务经理类型
     */
    private Integer type;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 扩展字段JSON格式
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后一次更新时间
     */
    private Date updatedAt;

}
