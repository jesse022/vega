package com.sanlux.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Code generated by terminus code gen
 * Desc: 等级履历表Model类
 * Date: 2016-08-04
 */
@Data
public class UserRankResume implements Serializable {


    private static final long serialVersionUID = 4904386282026820035L;
    private Long id;

    /**
     * 操作人ID
     */
    private Long operateId;

    /**
     * 操作人名称
     */
    private String operateName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户当前成长值
     */
    //private Long integration;
    private Long growthValue;
    /**
     * 用户所属等级ID
     */
    private Long rankId;

    /**
     * 等级名称
     */
    private String rankName;

    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后一次更新时间
     */
    private Date updatedAt;
}
