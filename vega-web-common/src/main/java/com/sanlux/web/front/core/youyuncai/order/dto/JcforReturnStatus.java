package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 集乘网接口返回状态信息
 * Created by lujm on 2018/2/27.
 */
@Data
public class JcforReturnStatus implements Serializable{

    private static final long serialVersionUID = 7703257228157372678L;

    /**
     * 必须  1:成功,0:失败
     */
    private Integer status;

    /**
     * 非必须,成功返回信息
     */
    private Map<String, Object> data;

    /**
     * 非必须,错误编码
     */
    private String errorcode;

    /**
     * 非必须,错误描述
     */
    private String errormsg;
}
