package com.sanlux.item.dto.api;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/3/20.
 */
@Data
public class ApiReturnStatus implements Serializable {

    private static final long serialVersionUID = -834783427167309894L;

    /**
     * 成功标志 1:成功 0:失败
     */
    private Integer status;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 错误描述
     */
    private String errorMsg;
}
