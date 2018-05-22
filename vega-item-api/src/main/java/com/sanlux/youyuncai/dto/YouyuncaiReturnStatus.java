package com.sanlux.youyuncai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采返回状态信息
 * Created by lujm on 2018/1/30.
 */
@Data
public class YouyuncaiReturnStatus implements Serializable {
    private static final long serialVersionUID = 3709996215745580625L;

    /**
     * 1:成功  0:错误
     */
    private Integer status;

    /**
     * 错误编码
     */
    private String errorcode;

    /**
     * 错误描述
     */
    private String errormsg;
}
