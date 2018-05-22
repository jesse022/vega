package com.sanlux.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务经理会员表Model类
 *
 * Created by lujm on 2017/5/24.
 */
@Data
public class ServiceManagerUser implements Serializable {
    private static final long serialVersionUID = -9116304561758500791L;

    private Long id;

    /**
     * 业务经理ID
     */
    private Long serviceManagerId;

    /**
     * 业务经理姓名
     */
    private String serviceManagerName;

    /**
     * 业务经理类型
     */
    private Integer type;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;


    /**
     * 用户手机号
     *
     */
    private String mobile;

    /**
     * 备注
     */
    private String remark;

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
