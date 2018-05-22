package com.sanlux.item.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
@Data
public class TaskJob implements Serializable {


    private static final long serialVersionUID = -5828305678136927561L;

    /**
     * job创建者
     */
    private Long userId;

    /**
     * job 状态
     */
    private Integer status;

    /**
     * 失败原因
     */
    private String error;

    /**
     * 额外信息
     */
    private String extra;
}
